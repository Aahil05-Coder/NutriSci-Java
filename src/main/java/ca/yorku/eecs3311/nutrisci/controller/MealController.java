package ca.yorku.eecs3311.nutrisci.controller;

import ca.yorku.eecs3311.nutrisci.dao.MealDAO;
import ca.yorku.eecs3311.nutrisci.dao.MealItemDAO;
import ca.yorku.eecs3311.nutrisci.model.Meal;
import ca.yorku.eecs3311.nutrisci.model.MealItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

public class MealController {

    private final MealDAO mealDAO = new MealDAO();
    private final MealItemDAO itemDAO = new MealItemDAO();

    public void saveMeal(int userId, LocalDate date, String mealType, List<MealItem> items) throws SQLException {
        if (!"SNACK".equalsIgnoreCase(mealType)) {
            List<Meal> existingMeals = mealDAO.findMealsByUser(userId);
            for (Meal meal : existingMeals) {
                if (meal.getMealDate().equals(date) && meal.getMealType().equalsIgnoreCase(mealType)) {
                    throw new SQLException("You have already logged " + mealType + " on " + date);
                }
            }
        }

        int mealId = mealDAO.insertMeal(userId, date, mealType);
        for (MealItem mi : items) {
            mi.setMealId(mealId);
        }
        itemDAO.insertBatch(items);
    }

    public List<Meal> getMeals(int userId) throws SQLException {
        return mealDAO.findMealsByUser(userId);
    }

    public void deleteMeal(int mealId) throws SQLException {
        mealDAO.deleteMeal(mealId);
    }
    
    public void deleteAllMealsForUser(int userId) throws SQLException {
        mealDAO.deleteAllMealsForUser(userId);
    }


    public List<MealItem> getMealItems(int mealId) throws SQLException {
        return itemDAO.findByMealId(mealId);
    }
    
    public List<MealItem> getAllMealItemsForUser(int userId) throws SQLException {
        return itemDAO.findAllByUserId(userId);
    }
    
    public void fixInvalidMealItems() throws SQLException {
        // Find meal items with invalid conversion factors and fix them
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT mi.id, mi.food_id, mi.measure_id, fn.fooddescription " +
                        "FROM meal_items mi " +
                        "JOIN food_name fn ON mi.food_id = fn.foodid " +
                        "WHERE NOT EXISTS (SELECT 1 FROM conversion_factor cf " +
                        "WHERE cf.foodid = mi.food_id AND cf.measureid = mi.measure_id)";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int mealItemId = rs.getInt("id");
                        int foodId = rs.getInt("food_id");
                        int measureId = rs.getInt("measure_id");
                        String foodName = rs.getString("fooddescription");
                        
                        System.out.println("DEBUG: Found invalid meal item - ID: " + mealItemId + 
                                         ", Food: " + foodName + " (ID: " + foodId + "), Measure: " + measureId);
                        
                        // Try to find a compatible measure for the same food first
                        int newMeasureId = findCompatibleMeasure(conn, foodId);
                        if (newMeasureId != -1) {
                            // Update the meal item with a compatible measure for the same food
                            String updateSql = "UPDATE meal_items SET measure_id = ? WHERE id = ?";
                            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                                updatePs.setInt(1, newMeasureId);
                                updatePs.setInt(2, mealItemId);
                                int rowsAffected = updatePs.executeUpdate();
                                System.out.println("DEBUG: Fixed meal item " + mealItemId + " - updated measure_id to " + newMeasureId + " (rows affected: " + rowsAffected + ")");
                            }
                        } else {
                            // If no compatible measure found, try to find a compatible food
                            int newFoodId = findCompatibleFood(conn, foodId);
                            if (newFoodId != -1) {
                                int newMeasureId2 = findCompatibleMeasure(conn, newFoodId);
                                if (newMeasureId2 != -1) {
                                    // Update the meal item
                                    String updateSql = "UPDATE meal_items SET food_id = ?, measure_id = ? WHERE id = ?";
                                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                                        updatePs.setInt(1, newFoodId);
                                        updatePs.setInt(2, newMeasureId2);
                                        updatePs.setInt(3, mealItemId);
                                        int rowsAffected = updatePs.executeUpdate();
                                        System.out.println("DEBUG: Fixed meal item " + mealItemId + " - updated to food_id=" + newFoodId + ", measure_id=" + newMeasureId2 + " (rows affected: " + rowsAffected + ")");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private int findCompatibleFood(Connection conn, int originalFoodId) throws SQLException {
        // Try to find a similar food with conversion factors
        // For now, just return a default food that we know has conversion factors
        return 86; // Dried egg white powder
    }
    
    private int findCompatibleMeasure(Connection conn, int foodId) throws SQLException {
        String sql = "SELECT measureid FROM conversion_factor WHERE foodid = ? ORDER BY conversionfactorvalue ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("measureid");
                }
            }
        }
        return -1;
    }
}
