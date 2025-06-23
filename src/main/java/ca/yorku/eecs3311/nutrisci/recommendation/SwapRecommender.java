package ca.yorku.eecs3311.nutrisci.recommendation;

import ca.yorku.eecs3311.nutrisci.model.SwapGoal;
import static ca.yorku.eecs3311.nutrisci.util.DBUtil.getConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwapRecommender {
    private static final Map<String, Integer> NUTRIENT_MAP = new HashMap<>();
    static {
        NUTRIENT_MAP.put("Fiber", 291);
        NUTRIENT_MAP.put("Calories", 208);
        NUTRIENT_MAP.put("Protein", 203);
        NUTRIENT_MAP.put("Carbohydrate", 205);
    }

    public static class SwapSuggestion {
        private final String originalFoodName;
        private final String suggestedFoodName;
        private final double expectedChange;

        public SwapSuggestion(String originalFoodName, String suggestedFoodName, double expectedChange) {
            this.originalFoodName = originalFoodName;
            this.suggestedFoodName = suggestedFoodName;
            this.expectedChange = expectedChange;
        }

        public String getOriginalFoodName() { return originalFoodName; }
        public String getSuggestedFoodName() { return suggestedFoodName; }
        public double getExpectedChange() { return expectedChange; }
    }

    public List<SwapSuggestion> suggestSwaps(List<SwapGoal> goals) {
        List<SwapSuggestion> result = new ArrayList<>();
        try (Connection conn = getConnection()) {
            for (SwapGoal goal : goals) {
                Integer nutrNo = NUTRIENT_MAP.get(goal.getNutrient());
                if (nutrNo == null) continue;
                String lowSql = "SELECT food_no, nutrval FROM nutrient_amount " +
                                "WHERE nutrient_no = ? ORDER BY nutrval ASC LIMIT 1";
                String highSql = "SELECT food_no, nutrval FROM nutrient_amount " +
                                 "WHERE nutrient_no = ? ORDER BY nutrval DESC LIMIT 1";
                int lowFood = 0, highFood = 0;
                double lowVal = 0, highVal = 0;
                try (PreparedStatement ps = conn.prepareStatement(lowSql)) {
                    ps.setInt(1, nutrNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            lowFood = rs.getInt("food_no");
                            lowVal = rs.getDouble("nutrval");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(highSql)) {
                    ps.setInt(1, nutrNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            highFood = rs.getInt("food_no");
                            highVal = rs.getDouble("nutrval");
                        }
                    }
                }

                String lowName = fetchFoodName(conn, lowFood);
                String highName = fetchFoodName(conn, highFood);
                double change = highVal - lowVal;
                if ("INCREASE".equals(goal.getDirection())) {
                    result.add(new SwapSuggestion(lowName, highName, change));
                } else {
                    result.add(new SwapSuggestion(highName, lowName, -change));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String fetchFoodName(Connection conn, int foodNo) throws SQLException {
        String sql = "SELECT food_name_eng FROM food_name WHERE food_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("food_name_eng");
            }
        }
        return "Unknown";
    }
}
