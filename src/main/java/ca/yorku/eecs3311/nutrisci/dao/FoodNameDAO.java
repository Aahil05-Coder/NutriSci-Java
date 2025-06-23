package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.model.Food;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FoodNameDAO {

    public List<Food> searchByDescription(String keyword) throws SQLException {
        String sql = "SELECT foodid, fooddescription FROM food_name "
                   + "WHERE fooddescription LIKE ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Food> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Food(
                        rs.getInt("foodid"),
                        rs.getString("fooddescription")
                    ));
                }
                return list;
            }
        }
    }
    public Food searchById(int id) throws SQLException {
        String sql = "SELECT foodid, fooddescription FROM food_name WHERE foodid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Food(
                        rs.getInt("foodid"),
                        rs.getString("fooddescription")
                    );
                }
            }
        }
        return null;
    }
}
