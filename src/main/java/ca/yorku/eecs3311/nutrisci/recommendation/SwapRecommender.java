package ca.yorku.eecs3311.nutrisci.recommendation;

import ca.yorku.eecs3311.nutrisci.dao.NutrientNameDAO;
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
    private final NutrientNameDAO nutrientNameDAO = new NutrientNameDAO();
    
    // Cache for nutrient mappings to avoid repeated database queries
    private Map<String, Integer> nutrientMap = null;

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
    
    private Map<String, Integer> getNutrientMap() throws SQLException {
        if (nutrientMap == null) {
            nutrientMap = nutrientNameDAO.getAllNutrientNames();
        }
        return nutrientMap;
    }

    public List<SwapSuggestion> suggestSwaps(List<SwapGoal> goals, List<ca.yorku.eecs3311.nutrisci.model.MealItem> mealItems) {
        List<SwapSuggestion> result = new ArrayList<>();
        Connection conn = null;
        try {
            // Get nutrient mapping BEFORE opening the main connection
            Map<String, Integer> nutrientMapping = getNutrientMap();
            
            conn = getConnection();
            conn.setAutoCommit(true); // Ensure auto-commit is enabled
            
            for (SwapGoal goal : goals) {
                Integer nutrNo = nutrientMapping.get(goal.getNutrient());
                System.out.println("DEBUG: Goal nutrient=" + goal.getNutrient() + ", mapped nutrNo=" + nutrNo);
                if (nutrNo == null) {
                    System.out.println("DEBUG: Nutrient not found in database: " + goal.getNutrient());
                    continue;
                }
                
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
                    String betterSql;
                    double targetImprovement = goal.getAmount(); // Use the goal amount as target improvement
                    
                    if ("INCREASE".equals(goal.getDirection())) {
                        // For INCREASE, find foods with MORE of the nutrient
                        // Look for foods that provide at least some improvement
                        // Filter out foods with obviously incorrect data (protein > 50g per 100g is suspicious)
                        betterSql = "SELECT foodid, nutrientvalue FROM nutrient_amount " +
                                   "WHERE nutrientid = ? AND nutrientvalue > ? " +
                                   "ORDER BY nutrientvalue DESC LIMIT 10"; // Get foods with more of the nutrient
                    } else {
                        // For DECREASE, find foods with LESS of the nutrient
                        // Look for foods that reduce by at least some amount
                        betterSql = "SELECT foodid, nutrientvalue FROM nutrient_amount " +
                                   "WHERE nutrientid = ? AND nutrientvalue < ? " +
                                   "ORDER BY nutrientvalue ASC LIMIT 10"; // Get foods with less of the nutrient
                    }
                    
                    int betterFoodId = item.getFoodId();
                    double betterVal = currentVal;
                    double bestImprovement = 0;
                    
                    try (PreparedStatement ps = conn.prepareStatement(betterSql)) {
                        ps.setInt(1, nutrNo);
                        ps.setDouble(2, currentVal);
                        try (ResultSet rs = ps.executeQuery()) {
                            // Find the best food that provides reasonable improvement
                            while (rs.next()) {
                                int candidateFoodId = rs.getInt("foodid");
                                double candidateVal = rs.getDouble("nutrientvalue");
                                double improvement = Math.abs(candidateVal - currentVal);
                                
                                // For small current values, accept smaller improvements
                                // For larger current values, require larger improvements
                                double minImprovement = Math.max(0.1, currentVal * 0.05); // At least 5% improvement or 0.1g
                                
                                if (improvement >= minImprovement) {
                                    // Prefer moderate improvements over extreme ones
                                    // But accept any improvement that meets the minimum
                                    if (betterFoodId == item.getFoodId() || 
                                        (improvement <= targetImprovement * 3 && improvement > bestImprovement)) {
                                        betterFoodId = candidateFoodId;
                                        betterVal = candidateVal;
                                        bestImprovement = improvement;
                                    }
                                }
                            }
                        }
                    }
                    System.out.println("DEBUG: Best swap foodId=" + betterFoodId + ", betterVal=" + betterVal);
                    
                    if (betterFoodId != item.getFoodId()) {
                        String origName = fetchFoodName(conn, item.getFoodId());
                        String suggName = fetchFoodName(conn, betterFoodId);
                        double change = betterVal - currentVal;
                        System.out.println("DEBUG: Suggest swap " + origName + " -> " + suggName + ", change=" + change);
                        result.add(new SwapSuggestion(origName, suggName, change));
                    } else {
                        System.out.println("DEBUG: No better swap found for foodId=" + item.getFoodId());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR in suggestSwaps: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("ERROR closing connection: " + closeEx.getMessage());
                }
            }
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
