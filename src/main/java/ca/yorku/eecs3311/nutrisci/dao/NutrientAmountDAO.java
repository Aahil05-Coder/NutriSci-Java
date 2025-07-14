package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.*;
import java.util.*;
import ca.yorku.eecs3311.nutrisci.util.Pair;


public class NutrientAmountDAO {

    public Map<String, Pair<Double, String>> getTotalNutrientsForMeal(int mealId) throws SQLException {
        String sql = "SELECT na.nutrient_id, SUM(" +
                     "    na.nutrient_value / 100.0 * cf.conversion_factor_value * mi.quantity" +
                     ") AS total_amount " +
                     "FROM meal_items mi " +
                     "JOIN conversion_factor cf ON mi.food_id = cf.food_id AND mi.measure_id = cf.measure_id " +
                     "JOIN nutrient_amount na ON mi.food_id = na.food_id " +
                     "WHERE mi.meal_id = ? " +
                     "GROUP BY na.nutrient_id " +
                     "HAVING total_amount > 0";

        Map<String, Pair<Double, String>> result = new LinkedHashMap<>();
        NutrientNameDAO nameDAO = new NutrientNameDAO();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mealId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int nutrientId = rs.getInt("nutrient_id");
                    double amount = rs.getDouble("total_amount");

                    String name = nameDAO.getNutrientNameById(nutrientId);
                    String unit = getUnitByNutrientId(conn, nutrientId); 

                    result.put(name, new Pair<>(amount, unit));
                }
            }
        }

        return result;
    }


    private String getUnitByNutrientId(Connection conn, int id) throws SQLException {
        String sql = "SELECT unit FROM nutrient_name WHERE nutrientid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("unit");
                }
            }
        }
        return "";
    }
    public Map<Integer, Double> getNutrientMapByFoodId(int foodId) throws SQLException {
        String sql = "SELECT nutrientid, nutrientvalue FROM nutrient_amount WHERE foodid = ?";
        Map<Integer, Double> map = new HashMap<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        int id = Integer.parseInt(rs.getString("nutrientid"));
                        double value = Double.parseDouble(rs.getString("nutrientvalue"));
                        map.put(id, value);
                    } catch (NumberFormatException e) {
                        // Skip invalid nutrient data
                        System.err.println("Invalid nutrient data for food " + foodId + ": " + e.getMessage());
                    }
                }
            }
        }
        return map;
    }



}
