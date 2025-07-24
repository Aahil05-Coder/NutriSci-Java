package ca.yorku.eecs3311.nutrisci.controller;

import ca.yorku.eecs3311.nutrisci.dao.ConversionFactorDAO;
import ca.yorku.eecs3311.nutrisci.dao.NutrientAmountDAO;
import ca.yorku.eecs3311.nutrisci.dao.NutrientNameDAO;
import ca.yorku.eecs3311.nutrisci.model.MealItem;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;
import ca.yorku.eecs3311.nutrisci.controller.MealController;

public class MealNutritionController {

    private final ConversionFactorDAO factorDAO = new ConversionFactorDAO();
    private final NutrientAmountDAO nutrientDAO = new NutrientAmountDAO();
    private final NutrientNameDAO nutrientNameDAO = new NutrientNameDAO();
    
    // Cache for nutrient IDs to avoid repeated database queries
    private Integer energyNutrientId = null;
    private Integer carbsNutrientId = null;
    private Integer proteinsNutrientId = null;
    private Integer fatsNutrientId = null;

    public double calculateTotalCalories(List<MealItem> items) throws SQLException {
        return calculateTotalCalories(items, null);
    }
    
    public double calculateTotalCalories(List<MealItem> items, Connection conn) throws SQLException {
        double total = 0.0;
        
        // Get energy nutrient ID from database
        if (energyNutrientId == null) {
            try {
                energyNutrientId = nutrientNameDAO.getNutrientIdByName("Energy (kcal)");
            } catch (SQLException e) {
                // Fallback to common energy nutrient ID if name not found
                energyNutrientId = 208;
            }
        }
        
        for (MealItem mi : items) {
            double weight = mi.getQuantity() * factorDAO.getFactor(mi.getFoodId(), mi.getMeasureId());
            Map<Integer, Double> nutrients = nutrientDAO.getNutrientMapByFoodId(mi.getFoodId());
            Double kcal = nutrients.get(energyNutrientId);
            System.out.println("DEBUG: foodId=" + mi.getFoodId() + ", measureId=" + mi.getMeasureId() + ", quantity=" + mi.getQuantity() + ", factor=" + weight + ", kcal=" + kcal);
            if (kcal != null && kcal > 0) {
                double itemKcal = kcal * weight / 100.0;
                System.out.println("DEBUG: Calculated item kcal = " + itemKcal);
                total += itemKcal;
            } else {
                System.out.println("DEBUG: Skipping food " + mi.getFoodId() + " - no kcal data available");
            }
        }
        System.out.println("DEBUG: Total calculated calories = " + total);
        return total;
    }

    public Map<Integer, Double> calculateNutrientSummary(List<MealItem> items) throws SQLException {
        return calculateNutrientSummary(items, null);
    }
    
    public Map<Integer, Double> calculateNutrientSummary(List<MealItem> items, Connection conn) throws SQLException {
        Map<Integer, Double> result = new HashMap<>();
        for (MealItem mi : items) {
            double weight = mi.getQuantity() * factorDAO.getFactor(mi.getFoodId(), mi.getMeasureId());
            Map<Integer, Double> nutrients = nutrientDAO.getNutrientMapByFoodId(mi.getFoodId());
            for (Map.Entry<Integer, Double> entry : nutrients.entrySet()) {
                double added = entry.getValue() * weight / 100.0;
                result.merge(entry.getKey(), added, Double::sum);
            }
        }
        return result;
    }
    
    public void calculateAndStoreDailySummary(int userId, LocalDate date) throws SQLException {
        // Get nutrient IDs from database
        if (carbsNutrientId == null) {
            try {
                carbsNutrientId = nutrientNameDAO.getNutrientIdByName("Carbohydrate, by difference");
            } catch (SQLException e) {
                carbsNutrientId = 205; // Fallback
            }
        }
        if (proteinsNutrientId == null) {
            try {
                proteinsNutrientId = nutrientNameDAO.getNutrientIdByName("Protein");
            } catch (SQLException e) {
                proteinsNutrientId = 203; // Fallback
            }
        }
        if (fatsNutrientId == null) {
            try {
                fatsNutrientId = nutrientNameDAO.getNutrientIdByName("Total lipid (fat)");
            } catch (SQLException e) {
                fatsNutrientId = 204; // Fallback
            }
        }
        if (energyNutrientId == null) {
            try {
                energyNutrientId = nutrientNameDAO.getNutrientIdByName("Energy (kcal)");
            } catch (SQLException e) {
                energyNutrientId = 208; // Fallback
            }
        }
        
        try (Connection conn = DBUtil.getConnection()) {
            // Get all meals for the user on the given date
            String mealSql = "SELECT id FROM meals WHERE user_id = ? AND meal_date = ?";
            List<Integer> mealIds = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(mealSql)) {
                ps.setInt(1, userId);
                ps.setDate(2, java.sql.Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        mealIds.add(rs.getInt("id"));
                    }
                }
            }
            
            if (mealIds.isEmpty()) {
                return; // No meals for this date
            }
            
            System.out.println("DEBUG: Found " + mealIds.size() + " meals for date " + date);
            
            // Check if meal items exist for these meals
            for (Integer mealId : mealIds) {
                String checkSql = "SELECT COUNT(*) as count FROM meal_items WHERE meal_id = ?";
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setInt(1, mealId);
                    try (ResultSet checkRs = checkPs.executeQuery()) {
                        if (checkRs.next()) {
                            int itemCount = checkRs.getInt("count");
                            System.out.println("DEBUG: Meal " + mealId + " has " + itemCount + " items");
                        }
                    }
                }
            }
            
            // Calculate total nutrition for all meals using the same connection
            double totalCalories = 0.0;
            double totalCarbs = 0.0;
            double totalProteins = 0.0;
            double totalFats = 0.0;
            
            // Get meal items and calculate nutrition directly using the same connection
            String itemSql = "SELECT mi.food_id, mi.measure_id, mi.quantity, " +
                           "cf.conversionfactorvalue, " + // Fixed column name
                           "na.nutrientvalue as energy_val, " +
                           "na2.nutrientvalue as carbs_val, " +
                           "na3.nutrientvalue as proteins_val, " +
                           "na4.nutrientvalue as fats_val " +
                           "FROM meal_items mi " +
                           "JOIN conversion_factor cf ON mi.food_id = cf.foodid AND mi.measure_id = cf.measureid " +
                           "LEFT JOIN nutrient_amount na ON mi.food_id = na.foodid AND na.nutrientid = ? " +
                           "LEFT JOIN nutrient_amount na2 ON mi.food_id = na2.foodid AND na2.nutrientid = ? " +
                           "LEFT JOIN nutrient_amount na3 ON mi.food_id = na3.foodid AND na3.nutrientid = ? " +
                           "LEFT JOIN nutrient_amount na4 ON mi.food_id = na4.foodid AND na4.nutrientid = ? " +
                           "WHERE mi.meal_id = ?";
            
            for (Integer mealId : mealIds) {
                System.out.println("DEBUG: Processing meal ID: " + mealId);
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    ps.setInt(1, energyNutrientId);
                    ps.setInt(2, carbsNutrientId);
                    ps.setInt(3, proteinsNutrientId);
                    ps.setInt(4, fatsNutrientId);
                    ps.setInt(5, mealId);
                    
                    System.out.println("DEBUG: Executing SQL with energyNutrientId=" + energyNutrientId + 
                                     ", carbsNutrientId=" + carbsNutrientId + 
                                     ", proteinsNutrientId=" + proteinsNutrientId + 
                                     ", fatsNutrientId=" + fatsNutrientId);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        int rowCount = 0;
                        while (rs.next()) {
                            rowCount++;
                            double quantity = rs.getDouble("quantity");
                            double factor = rs.getDouble("conversionfactorvalue"); // Fixed column name
                            double weight = quantity * factor;
                            
                            System.out.println("DEBUG: Row " + rowCount + " - food_id=" + rs.getInt("food_id") + 
                                             ", measure_id=" + rs.getInt("measure_id") + 
                                             ", quantity=" + quantity + 
                                             ", factor=" + factor + 
                                             ", weight=" + weight);
                            
                            // Calculate calories
                            Double energyVal = rs.getDouble("energy_val");
                            if (!rs.wasNull() && energyVal > 0) {
                                double itemKcal = energyVal * weight / 100.0;
                                totalCalories += itemKcal;
                                System.out.println("DEBUG: Item kcal = " + itemKcal + " (total: " + totalCalories + ")");
                            } else {
                                System.out.println("DEBUG: Energy value is null or zero: " + energyVal);
                            }
                            
                            // Calculate carbs
                            Double carbsVal = rs.getDouble("carbs_val");
                            if (!rs.wasNull()) {
                                totalCarbs += carbsVal * weight / 100.0;
                                System.out.println("DEBUG: Carbs value: " + carbsVal + " (total: " + totalCarbs + ")");
                            } else {
                                System.out.println("DEBUG: Carbs value is null");
                            }
                            
                            // Calculate proteins
                            Double proteinsVal = rs.getDouble("proteins_val");
                            if (!rs.wasNull()) {
                                totalProteins += proteinsVal * weight / 100.0;
                                System.out.println("DEBUG: Proteins value: " + proteinsVal + " (total: " + totalProteins + ")");
                            } else {
                                System.out.println("DEBUG: Proteins value is null");
                            }
                            
                            // Calculate fats
                            Double fatsVal = rs.getDouble("fats_val");
                            if (!rs.wasNull()) {
                                totalFats += fatsVal * weight / 100.0;
                                System.out.println("DEBUG: Fats value: " + fatsVal + " (total: " + totalFats + ")");
                            } else {
                                System.out.println("DEBUG: Fats value is null");
                            }
                        }
                        System.out.println("DEBUG: Found " + rowCount + " rows for meal " + mealId);
                    }
                }
            }
            
            // Calculate percentages using proper calorie conversion factors
            double carbsPct = totalCalories > 0 ? (totalCarbs * 4 / totalCalories) * 100 : 0.0;
            double proteinsPct = totalCalories > 0 ? (totalProteins * 4 / totalCalories) * 100 : 0.0;
            double fatsPct = totalCalories > 0 ? (totalFats * 9 / totalCalories) * 100 : 0.0;
            double othersPct = Math.max(0, 100 - carbsPct - proteinsPct - fatsPct);
            
            System.out.println("DEBUG: Daily summary calculation - calories=" + totalCalories + 
                             ", carbs=" + totalCarbs + "(" + carbsPct + "%), " +
                             "proteins=" + totalProteins + "(" + proteinsPct + "%), " +
                             "fats=" + totalFats + "(" + fatsPct + "%)");
            
            // Only save if we have actual data
            if (totalCalories > 0) {
                // Store or update daily summary using the same connection
                // First delete any existing record for this user and date
                String deleteSql = "DELETE FROM daily_summary WHERE user_id = ? AND summary_date = ?";
                try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                    deletePs.setInt(1, userId);
                    deletePs.setDate(2, java.sql.Date.valueOf(date));
                    int deletedRows = deletePs.executeUpdate();
                    System.out.println("DEBUG: Deleted " + deletedRows + " existing daily summary records");
                }
                
                // Then insert the new record
                String insertSql = "INSERT INTO daily_summary (user_id, summary_date, total_calories, carbs_pct, proteins_pct, fats_pct, others_pct) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setDate(2, java.sql.Date.valueOf(date));
                    ps.setDouble(3, totalCalories);
                    ps.setDouble(4, carbsPct);
                    ps.setDouble(5, proteinsPct);
                    ps.setDouble(6, fatsPct);
                    ps.setDouble(7, othersPct);
                    int rowsAffected = ps.executeUpdate();
                    System.out.println("DEBUG: Daily summary saved successfully for user " + userId + " on " + date + " (rows affected: " + rowsAffected + ")");
                    
                    // Verify the data was saved
                    String verifySql = "SELECT * FROM daily_summary WHERE user_id = ? AND summary_date = ?";
                    try (PreparedStatement verifyPs = conn.prepareStatement(verifySql)) {
                        verifyPs.setInt(1, userId);
                        verifyPs.setDate(2, java.sql.Date.valueOf(date));
                        try (ResultSet verifyRs = verifyPs.executeQuery()) {
                            if (verifyRs.next()) {
                                System.out.println("DEBUG: Verified daily summary exists - calories: " + verifyRs.getDouble("total_calories") + 
                                                 ", carbs: " + verifyRs.getDouble("carbs_pct") + 
                                                 ", proteins: " + verifyRs.getDouble("proteins_pct") + 
                                                 ", fats: " + verifyRs.getDouble("fats_pct"));
                            } else {
                                System.out.println("DEBUG: ERROR - Daily summary was not saved!");
                            }
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("ERROR saving daily summary: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("DEBUG: No calories calculated, skipping daily summary save");
            }
        }
    }
    
    public void recalculateAllDailySummaries(int userId) throws SQLException {
        System.out.println("DEBUG: Recalculating all daily summaries for user " + userId);
        
        try (Connection conn = DBUtil.getConnection()) {
            // Get all unique dates for this user's meals
            String dateSql = "SELECT DISTINCT meal_date FROM meals WHERE user_id = ? ORDER BY meal_date";
            List<LocalDate> dates = new ArrayList<>();
            
            try (PreparedStatement ps = conn.prepareStatement(dateSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        dates.add(rs.getDate("meal_date").toLocalDate());
                    }
                }
            }
            
            System.out.println("DEBUG: Found " + dates.size() + " unique dates for user " + userId);
            
            // Recalculate daily summary for each date
            for (LocalDate date : dates) {
                System.out.println("DEBUG: Recalculating for date: " + date);
                calculateAndStoreDailySummary(userId, date);
            }
        }
    }
}
