package ca.yorku.eecs3311.nutrisci.dao;

import java.sql.*;
import java.util.*;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

public class NutrientNameDAO {

    public Map<String, Integer> getAllNutrientNames() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>(); 
        String sql = "SELECT nutrientid, nutrientname FROM nutrient_name ORDER BY nutrientname";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("nutrientname"), rs.getInt("nutrientid"));
            }
        }
        return map;
    }

    public int getNutrientIdByName(String name) throws SQLException {
        String sql = "SELECT nutrientid FROM nutrient_name WHERE nutrientname = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("nutrientid");
                }
            }
        }
        throw new SQLException("Not found nutrient" + name);
    }


    public String getNutrientNameById(int id) throws SQLException {
        String sql = "SELECT nutrientname FROM nutrient_name WHERE nutrientid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nutrientname");
                }
            }
        }
        throw new SQLException("Not found nutrient ID: " + id);
    }
    public String getUnitByNutrientId(int id) throws SQLException {
        String sql = "SELECT nutrientunit FROM nutrient_name WHERE nutrientid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nutrientunit");
                }
            }
        }
        return "";
    }

}
