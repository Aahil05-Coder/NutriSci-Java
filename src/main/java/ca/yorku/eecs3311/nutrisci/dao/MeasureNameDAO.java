package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.model.Measure;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MeasureNameDAO {

    public static List<Measure> getAllMeasures() throws SQLException {
        List<Measure> list = new ArrayList<>();
        String sql = "SELECT measureid, measuredescription FROM measure_name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("measureid");
                String desc = rs.getString("measuredescription");
                list.add(new Measure(id, desc));
            }
        }
        return list;
    }
    public static Measure getById(int id) throws SQLException {
        String sql = "SELECT measureid, measuredescription FROM measure_name WHERE measureid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Measure m = new Measure();
                    m.setMeasureId(rs.getInt("measureid"));
                    m.setDescription(rs.getString("measuredescription"));
                    return m;
                }
            }
        }
        return null;
    }
    public String getUnitByNutrientId(int id) throws SQLException {
        String sql = "SELECT unit FROM nutrient_name WHERE nutrientid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("unit");
                }
            }
        }
        return "";
    }

}
