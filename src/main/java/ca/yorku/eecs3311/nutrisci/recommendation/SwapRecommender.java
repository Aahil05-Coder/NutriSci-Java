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
        NUTRIENT_MAP.put("FIBER", 291);
        NUTRIENT_MAP.put("Calories", 208);
        NUTRIENT_MAP.put("CALORIES", 208);
        NUTRIENT_MAP.put("Protein", 203);
        NUTRIENT_MAP.put("PROTEIN", 203);
        NUTRIENT_MAP.put("Carbohydrate", 205);
        NUTRIENT_MAP.put("CARBOHYDRATE", 205);
        NUTRIENT_MAP.put("Sugars, total", 269);
        NUTRIENT_MAP.put("SUGARS, TOTAL", 269);
        NUTRIENT_MAP.put("Fat", 204);
        NUTRIENT_MAP.put("FAT", 204);
        // Add more as needed, matching the dropdown exactly
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

    public List<SwapSuggestion> suggestSwaps(List<SwapGoal> goals, List<ca.yorku.eecs3311.nutrisci.model.MealItem> mealItems) {
        List<SwapSuggestion> result = new ArrayList<>();
        try (Connection conn = getConnection()) {
            for (SwapGoal goal : goals) {
                Integer nutrNo = NUTRIENT_MAP.get(goal.getNutrient());
                System.out.println("DEBUG: Goal nutrient=" + goal.getNutrient() + ", mapped nutrNo=" + nutrNo);
                if (nutrNo == null) continue;
                for (ca.yorku.eecs3311.nutrisci.model.MealItem item : mealItems) {
                    // Get current value for this food
                    double currentVal = 0;
                    String getValSql = "SELECT nutrientvalue FROM nutrient_amount WHERE foodid = ? AND nutrientid = ?";
                    try (PreparedStatement ps = conn.prepareStatement(getValSql)) {
                        ps.setInt(1, item.getFoodId());
                        ps.setInt(2, nutrNo);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                currentVal = rs.getDouble("nutrientvalue");
                            }
                        }
                    }
                    System.out.println("DEBUG: MealItem foodId=" + item.getFoodId() + ", currentVal=" + currentVal);
                    // Find a better food for this nutrient
                    String betterSql = "SELECT foodid, nutrientvalue FROM nutrient_amount WHERE nutrientid = ? ORDER BY nutrientvalue DESC LIMIT 1";
                    if ("DECREASE".equals(goal.getDirection())) {
                        betterSql = "SELECT foodid, nutrientvalue FROM nutrient_amount WHERE nutrientid = ? ORDER BY nutrientvalue ASC LIMIT 1";
                    }
                    int betterFoodId = item.getFoodId();
                    double betterVal = currentVal;
                    try (PreparedStatement ps = conn.prepareStatement(betterSql)) {
                        ps.setInt(1, nutrNo);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                betterFoodId = rs.getInt("foodid");
                                betterVal = rs.getDouble("nutrientvalue");
                            }
                        }
                    }
                    System.out.println("DEBUG: Best swap foodId=" + betterFoodId + ", betterVal=" + betterVal);
                    if (betterFoodId != item.getFoodId()) {
                        String origName = fetchFoodName(conn, item.getFoodId());
                        String suggName = fetchFoodName(conn, betterFoodId);
                        double change = betterVal - currentVal;
                        if ("DECREASE".equals(goal.getDirection())) change = currentVal - betterVal;
                        System.out.println("DEBUG: Suggest swap " + origName + " -> " + suggName + ", change=" + change);
                        result.add(new SwapSuggestion(origName, suggName, change));
                    } else {
                        System.out.println("DEBUG: No better swap found for foodId=" + item.getFoodId());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String fetchFoodName(Connection conn, int foodId) throws SQLException {
        String sql = "SELECT fooddescription FROM food_name WHERE foodid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("fooddescription");
            }
        }
        return "Unknown";
    }
}
