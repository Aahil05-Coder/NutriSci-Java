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

            try (PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM meal_items WHERE meal_id = ?")) {
                ps.setInt(1, mealId);
                ps.executeUpdate();  
            }

            try (PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM meals WHERE id = ?")) {
                ps.setInt(1, mealId);
                ps.executeUpdate();
            }
        }
    }

}
