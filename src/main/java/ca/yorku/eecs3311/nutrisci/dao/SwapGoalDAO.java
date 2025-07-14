package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.model.SwapGoal;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SwapGoalDAO {

    public void insert(SwapGoal goal) throws SQLException {
        String sql = "INSERT INTO swap_goals(user_id, nutrient_id, direction, amount, unit, intensity) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        int nutrientId;

        try {
            nutrientId = new NutrientNameDAO().getNutrientIdByName(goal.getNutrient());
        } catch (SQLException e) {
            throw new SQLException("Cannot get nutrient ID：" + goal.getNutrient(), e);
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, goal.getUserId());
            ps.setInt(2, nutrientId);
            ps.setString(3, goal.getDirection());
            ps.setDouble(4, goal.getAmount());
            ps.setString(5, goal.getUnit() == null ? "" : goal.getUnit());
            ps.setString(6, goal.getIntensity() == null ? "" : goal.getIntensity());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    goal.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<SwapGoal> findByUserId(int userId) throws SQLException {
        String sql = "SELECT sg.id, sg.user_id, sg.direction, sg.amount, sg.unit, sg.intensity, nn.nutrientname " +
                     "FROM swap_goals sg " +
                     "JOIN nutrient_name nn ON sg.nutrient_id = nn.nutrientid " +
                     "WHERE sg.user_id = ? " +
                     "ORDER BY sg.created_at DESC";
        
        List<SwapGoal> list = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SwapGoal goal = new SwapGoal();
                    goal.setId(rs.getInt("id"));
                    goal.setUserId(rs.getInt("user_id"));
                    goal.setNutrient(rs.getString("nutrientname")); 
                    goal.setDirection(rs.getString("direction"));
                    goal.setAmount(rs.getDouble("amount"));
                    goal.setUnit(rs.getString("unit"));
                    goal.setIntensity(rs.getString("intensity"));
                    list.add(goal);
                }
            }
        }
        return list;
    }


    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM swap_goals WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private int mapNutrientToId(String nutrient) throws SQLException {
        return new NutrientNameDAO().getNutrientIdByName(nutrient);
    }

    private String mapIdToNutrient(int id) throws SQLException {
        return new NutrientNameDAO().getNutrientNameById(id);
    }
}
