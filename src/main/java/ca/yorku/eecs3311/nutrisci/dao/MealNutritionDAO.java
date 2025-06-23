package ca.yorku.eecs3311.nutrisci.dao;

import ca.yorku.eecs3311.nutrisci.model.MealItem;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

import java.sql.*;
import java.util.*;

public class MealNutritionDAO {

    public Map<String, Double> getTotalNutrientsForMeal(int mealId) throws SQLException {
        Map<String, Double> nutrientTotals = new LinkedHashMap<>();

        String sql = 
        	    "SELECT " +
        	    "    na.nutrientid AS nutrient_id, " +
        	    "    nn.nutrientname AS nutrient_name, " +
        	    "    mi.quantity AS quantity, " +
        	    "    cf.conversion_factor_value AS conversion_factor, " +
        	    "    na.nutrient_value_per_100g AS nutrient_value_per_100g " +
        	    "FROM meal_items mi " +
        	    "JOIN conversion_factor cf ON mi.food_id = cf.food_id AND mi.measure_id = cf.measure_id " +
        	    "JOIN nutrient_amount na ON mi.food_id = na.food_id " +
        	    "JOIN nutrient_name nn ON na.nutrientid = nn.nutrientid " +
        	    "WHERE mi.meal_id = ?";


        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mealId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("nutrientname");
                    double quantity = rs.getDouble("quantity");
                    double grams = rs.getDouble("conversionfactorvalue");
                    double per100g = rs.getDouble("nutrientvalueper100g");
                    double total = quantity * grams * per100g / 100.0;
                    nutrientTotals.merge(name, total, Double::sum);
                }
            }
        }

        return nutrientTotals;
    }
}
