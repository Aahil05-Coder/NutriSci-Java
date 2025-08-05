package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.model.Meal;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MealDAO {

    public int insertMeal(int userId, LocalDate date, String mealType) throws SQLException {
        String sql = "INSERT INTO meals (user_id, meal_date, meal_type) VALUES (?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, mealType);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Insert meal failed, no ID obtained.");
    }

    public List<Meal> findMealsByUser(int userId) throws SQLException {
        String sql = "SELECT id, meal_date, meal_type FROM meals WHERE user_id=? ORDER BY meal_date DESC";
        List<Meal> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Meal m = new Meal();
                    m.setId(rs.getInt("id"));
                    m.setUserId(userId);
                    m.setMealDate(rs.getDate("meal_date").toLocalDate());
                    m.setMealType(rs.getString("meal_type"));
                    list.add(m);
                }
            }
        }
        return list;
    }

    public void deleteMeal(int mealId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            
            try {
                // First, get the meal date to identify related daily summaries
                String getMealDateSql = "SELECT meal_date FROM meals WHERE id = ?";
                LocalDate mealDate = null;
                try (PreparedStatement ps = conn.prepareStatement(getMealDateSql)) {
                    ps.setInt(1, mealId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            mealDate = rs.getDate("meal_date").toLocalDate();
                        }
                    }
                }
                
                if (mealDate == null) {
                    throw new SQLException("Meal not found with ID: " + mealId);
                }
                
                // Delete recommendations that reference meal items from this meal
                String deleteRecommendationsSql = "DELETE r FROM recommendations r " +
                                                "JOIN meal_items mi ON r.original_item_id = mi.id " +
                                                "WHERE mi.meal_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteRecommendationsSql)) {
                    ps.setInt(1, mealId);
                    int recommendationsDeleted = ps.executeUpdate();
                    System.out.println("DEBUG: Deleted " + recommendationsDeleted + " recommendations for meal " + mealId);
                }
                
                // Delete meal items
                String deleteItemsSql = "DELETE FROM meal_items WHERE meal_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteItemsSql)) {
                    ps.setInt(1, mealId);
                    int itemsDeleted = ps.executeUpdate();
                    System.out.println("DEBUG: Deleted " + itemsDeleted + " meal items for meal " + mealId);
                }
                
                // Delete the meal
                String deleteMealSql = "DELETE FROM meals WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteMealSql)) {
                    ps.setInt(1, mealId);
                    int rowsAffected = ps.executeUpdate();
                    System.out.println("DEBUG: Deleted meal " + mealId + " (rows affected: " + rowsAffected + ")");
                }
                
                // Check if there are any other meals for this user on the same date
                String checkOtherMealsSql = "SELECT COUNT(*) as count FROM meals WHERE user_id = (SELECT user_id FROM meals WHERE id = ?) AND meal_date = ?";
                int otherMealsCount = 0;
                try (PreparedStatement ps = conn.prepareStatement(checkOtherMealsSql)) {
                    ps.setInt(1, mealId);
                    ps.setDate(2, java.sql.Date.valueOf(mealDate));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            otherMealsCount = rs.getInt("count");
                        }
                    }
                }
                
                // If no other meals exist for this date, delete the daily summary
                if (otherMealsCount == 0) {
                    String deleteDailySummarySql = "DELETE FROM daily_summary WHERE user_id = (SELECT user_id FROM meals WHERE id = ?) AND summary_date = ?";
                    try (PreparedStatement ps = conn.prepareStatement(deleteDailySummarySql)) {
                        ps.setInt(1, mealId);
                        ps.setDate(2, java.sql.Date.valueOf(mealDate));
                        int summariesDeleted = ps.executeUpdate();
                        System.out.println("DEBUG: Deleted " + summariesDeleted + " daily summaries for date " + mealDate);
                    }
                } else {
                    System.out.println("DEBUG: Keeping daily summary - " + otherMealsCount + " other meals exist for date " + mealDate);
                }
                
                conn.commit(); // Commit the transaction
                
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                throw e;
            }
        }
    }
    
    public void deleteAllMealsForUser(int userId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            
            try {
                // Delete all recommendations for this user
                String deleteRecommendationsSql = "DELETE r FROM recommendations r " +
                                                "JOIN meal_items mi ON r.original_item_id = mi.id " +
                                                "JOIN meals m ON mi.meal_id = m.id " +
                                                "WHERE m.user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteRecommendationsSql)) {
                    ps.setInt(1, userId);
                    int recommendationsDeleted = ps.executeUpdate();
                    System.out.println("DEBUG: Deleted " + recommendationsDeleted + " recommendations for user " + userId);
                }
                
                // Delete all meal items for this user
                String deleteItemsSql = "DELETE mi FROM meal_items mi " +
                                      "JOIN meals m ON mi.meal_id = m.id " +
                                      "WHERE m.user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteItemsSql)) {
                    ps.setInt(1, userId);
                    int itemsDeleted = ps.executeUpdate();
                    System.out.println("DEBUG: Deleted " + itemsDeleted + " meal items for user " + userId);
                }
                
                // Delete all meals for this user
                String deleteMealsSql = "DELETE FROM meals WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteMealsSql)) {
                    ps.setInt(1, userId);
                    int mealsDeleted = ps.executeUpdate();
                    System.out.println("DEBUG: Deleted " + mealsDeleted + " meals for user " + userId);
                }
                
                // Delete all daily summaries for this user
                String deleteDailySummariesSql = "DELETE FROM daily_summary WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteDailySummariesSql)) {
                    ps.setInt(1, userId);
                    int summariesDeleted = ps.executeUpdate();
                    System.out.println("DEBUG: Deleted " + summariesDeleted + " daily summaries for user " + userId);
                }
                
                conn.commit(); // Commit the transaction
                System.out.println("DEBUG: Successfully deleted all data for user " + userId);
                
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                throw e;
            }
        }
    }

}
