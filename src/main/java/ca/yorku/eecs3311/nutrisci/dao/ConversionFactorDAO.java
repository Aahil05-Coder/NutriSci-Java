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
        // Fallback: use the first available conversion factor for this food
        String fallbackSql = "SELECT conversionfactorvalue FROM conversion_factor WHERE foodid = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.err.println("[WARN] Fallback: using first available conversion factor for foodId=" + foodId + ", measureId=" + measureId);
                    return rs.getDouble("conversionfactorvalue");
                }
            }
        }
        throw new SQLException("Conversion factor not found for foodId = " + foodId + ", measureId = " + measureId);
    }
}
