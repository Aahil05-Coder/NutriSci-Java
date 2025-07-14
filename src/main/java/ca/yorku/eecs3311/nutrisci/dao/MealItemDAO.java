package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.model.MealItem;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MealItemDAO {

    public void insert(MealItem item) throws SQLException {
        String sql = "INSERT INTO meal_items(meal_id, food_id, measure_id, quantity) VALUES (?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getMealId());
            ps.setInt(2, item.getFoodId());
            ps.setInt(3, item.getMeasureId());
            ps.setDouble(4, item.getQuantity());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }
        }
    }


    public void insertBatch(List<MealItem> items) throws SQLException {
        String sql = "INSERT INTO meal_items(meal_id, food_id, measure_id, quantity) VALUES (?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (MealItem item : items) {
                ps.setInt(1, item.getMealId());
                ps.setInt(2, item.getFoodId());
                ps.setInt(3, item.getMeasureId());
                ps.setDouble(4, item.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    public List<MealItem> findByMealId(int mealId) throws SQLException {
        String sql = "SELECT id, meal_id, food_id, measure_id, quantity FROM meal_items " +
                     "WHERE meal_id = ? ORDER BY id";
        List<MealItem> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mealId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MealItem mi = new MealItem();
                    mi.setId(rs.getInt("id"));
                    mi.setMealId(rs.getInt("meal_id"));
                    mi.setFoodId(rs.getInt("food_id"));
                    mi.setMeasureId(rs.getInt("measure_id"));
                    mi.setQuantity(rs.getDouble("quantity"));
                    list.add(mi);
                }
            }
        }
        return list;
    }

    public void deleteByMealId(int mealId) throws SQLException {
        String sql = "DELETE FROM meal_items WHERE meal_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mealId);
            ps.executeUpdate();
        }
    }
}
