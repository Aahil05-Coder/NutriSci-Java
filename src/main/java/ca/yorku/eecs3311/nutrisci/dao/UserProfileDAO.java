package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.model.UserProfile;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserProfileDAO {

    public UserProfile findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND deleted = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserProfile u = new UserProfile();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setSex(rs.getString("sex").charAt(0));
                    u.setBirthdate(rs.getDate("birthdate").toLocalDate());
                    u.setHeight(rs.getDouble("height"));
                    u.setHeightUnit(rs.getString("height_unit"));
                    u.setWeight(rs.getDouble("weight"));
                    u.setWeightUnit(rs.getString("weight_unit"));
                    return u;
                }
            }
        }
        return null;
    }

    public void insert(UserProfile u) throws SQLException {
        String sql = "INSERT INTO users(username, sex, birthdate, height, weight, height_unit, weight_unit) "
                   + "VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, String.valueOf(u.getSex()));
            ps.setDate(3, Date.valueOf(u.getBirthdate()));
            ps.setDouble(4, u.getHeight());
            ps.setDouble(5, u.getWeight());
            ps.setString(6, u.getHeightUnit());
            ps.setString(7, u.getWeightUnit());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setId(rs.getInt(1));
                }
            }
        }
    }


    public void update(UserProfile u) throws SQLException {
        String sql = "UPDATE users SET sex = ?, birthdate = ?, height = ?, weight = ?, height_unit = ?, weight_unit = ? "
                   + "WHERE username = ? AND deleted = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(u.getSex()));
            ps.setDate(2, Date.valueOf(u.getBirthdate()));
            ps.setDouble(3, u.getHeight());
            ps.setDouble(4, u.getWeight());
            ps.setString(5, u.getHeightUnit());
            ps.setString(6, u.getWeightUnit());
            ps.setString(7, u.getUsername());
            ps.executeUpdate();
        }
    }

    public void softDeleteByUsername(String username) throws SQLException {
        String sql = "UPDATE users SET deleted = 1 WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    public List<String> findAllUsernames() throws SQLException {
        String sql = "SELECT username FROM users WHERE deleted = 0";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("username"));
            }
        }
        return list;
    }
}
