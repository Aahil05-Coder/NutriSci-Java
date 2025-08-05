package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.model.Measure;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.*;
import java.util.*;

public class ConversionFactorDAO {

    public List<Measure> getMeasuresForFood(int foodId) throws SQLException {
        String sql = "SELECT DISTINCT measureid FROM conversion_factor WHERE foodid = ?";
        List<Integer> measureIds = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    measureIds.add(rs.getInt("measureid"));
                }
            }
        }

        List<Measure> result = new ArrayList<>();
        for (int id : measureIds) {
            Measure m = MeasureNameDAO.getById(id);
            if (m != null) result.add(m);
        }
        return result;
    }

    public double getFactor(int foodId, int measureId) throws SQLException {
        String sql = "SELECT conversionfactorvalue FROM conversion_factor WHERE foodid = ? AND measureid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            ps.setInt(2, measureId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("conversionfactorvalue");
                }
            }
        }
        
        // If no conversion factor found, provide a more helpful error message
        String foodName = getFoodName(foodId);
        String measureName = getMeasureName(measureId);
        throw new SQLException("Conversion factor not found for foodId = " + foodId + " (" + foodName + "), measureId = " + measureId + " (" + measureName + "). This food may not have conversion factors for this measure.");
    }
    
    private String getFoodName(int foodId) {
        try {
            String sql = "SELECT fooddescription FROM food_name WHERE foodid = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, foodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("fooddescription");
                    }
                }
            }
        } catch (SQLException e) {
            // Ignore errors in getting food name
        }
        return "Unknown Food";
    }
    
    private String getMeasureName(int measureId) {
        try {
            String sql = "SELECT measuredescription FROM measure_name WHERE measureid = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, measureId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("measuredescription");
                    }
                }
            }
        } catch (SQLException e) {
            // Ignore errors in getting measure name
        }
        return "Unknown Measure";
    }
}
