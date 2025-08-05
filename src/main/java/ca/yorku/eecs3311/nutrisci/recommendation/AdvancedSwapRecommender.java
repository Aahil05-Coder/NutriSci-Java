package ca.yorku.eecs3311.nutrisci.recommendation;

import ca.yorku.eecs3311.nutrisci.model.SwapGoal;
import static ca.yorku.eecs3311.nutrisci.util.DBUtil.getConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AdvancedSwapRecommender {
    
    public static class MultiMealSwapSuggestion {
        private final List<SingleMealSwap> mealSwaps;
        private final double totalExpectedChange;
        private final String description;

        public MultiMealSwapSuggestion(List<SingleMealSwap> mealSwaps, double totalExpectedChange) {
            this.mealSwaps = mealSwaps;
            this.totalExpectedChange = totalExpectedChange;
            this.description = generateDescription(mealSwaps, totalExpectedChange);
        }

        public List<SingleMealSwap> getMealSwaps() { return mealSwaps; }
        public double getTotalExpectedChange() { return totalExpectedChange; }
        public String getDescription() { return description; }

        private String generateDescription(List<SingleMealSwap> swaps, double totalChange) {
            StringBuilder desc = new StringBuilder();
            desc.append("Multi-meal swap: ");
            for (int i = 0; i < swaps.size(); i++) {
                if (i > 0) desc.append(" + ");
                desc.append(swaps.get(i).getOriginalFoodName())
                    .append(" → ")
                    .append(swaps.get(i).getSuggestedFoodName())
                    .append(" (+")
                    .append(String.format("%.1f", swaps.get(i).getExpectedChange()))
                    .append("g)");
            }
            desc.append(" = +").append(String.format("%.1f", totalChange)).append("g total");
            return desc.toString();
        }
    }

    public static class SingleMealSwap {
        private final int mealId;
        private final String originalFoodName;
        private final String suggestedFoodName;
        private final double expectedChange;
        private final int originalFoodId;
        private final int suggestedFoodId;

        public SingleMealSwap(int mealId, String originalFoodName, String suggestedFoodName, 
                            double expectedChange, int originalFoodId, int suggestedFoodId) {
            this.mealId = mealId;
            this.originalFoodName = originalFoodName;
            this.suggestedFoodName = suggestedFoodName;
            this.expectedChange = expectedChange;
            this.originalFoodId = originalFoodId;
            this.suggestedFoodId = suggestedFoodId;
        }

        public int getMealId() { return mealId; }
        public String getOriginalFoodName() { return originalFoodName; }
        public String getSuggestedFoodName() { return suggestedFoodName; }
        public double getExpectedChange() { return expectedChange; }
        public int getOriginalFoodId() { return originalFoodId; }
        public int getSuggestedFoodId() { return suggestedFoodId; }
    }

    public List<MultiMealSwapSuggestion> suggestMultiMealSwaps(List<SwapGoal> goals, 
                                                              List<ca.yorku.eecs3311.nutrisci.model.MealItem> allMealItems,
                                                              int userId) {
        List<MultiMealSwapSuggestion> result = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            for (SwapGoal goal : goals) {
                // Get nutrient ID
                int nutrientId = getNutrientId(conn, goal.getNutrient());
                if (nutrientId == -1) continue;

                double targetAmount = goal.getAmount();
                String direction = goal.getDirection();
                System.out.println("DEBUG: Looking for " + targetAmount + "g " + direction.toLowerCase() + " in " + goal.getNutrient());

                // Find all possible single-meal swaps
                List<SingleMealSwap> allPossibleSwaps = findAllPossibleSwaps(conn, allMealItems, nutrientId, direction, targetAmount);
                
                // Generate combinations to reach the target
                List<MultiMealSwapSuggestion> combinations = generateCombinations(allPossibleSwaps, targetAmount);
                result.addAll(combinations);
            }
        } catch (SQLException e) {
            System.err.println("ERROR in suggestMultiMealSwaps: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    private List<SingleMealSwap> findAllPossibleSwaps(Connection conn, 
                                                     List<ca.yorku.eecs3311.nutrisci.model.MealItem> mealItems,
                                                     int nutrientId, String direction, double targetAmount) throws SQLException {
        List<SingleMealSwap> swaps = new ArrayList<>();
        
        for (ca.yorku.eecs3311.nutrisci.model.MealItem item : mealItems) {
            // Get current nutrient value
            double currentValue = getNutrientValue(conn, item.getFoodId(), nutrientId);
            String currentFoodName = getFoodName(conn, item.getFoodId());
            
            System.out.println("DEBUG: Checking food " + currentFoodName + " (ID: " + item.getFoodId() + ") with " + currentValue + "g of nutrient");
            System.out.println("DEBUG: Target " + direction + " by " + targetAmount + "g");
            
            // Find better foods that target the goal amount
            String sql;
            if ("INCREASE".equals(direction)) {
                // For increase, look for foods that will give us close to the target increase
                double targetValue = currentValue + targetAmount;
                double minValue = targetValue * 0.8; // Allow 20% tolerance
                double maxValue = targetValue * 1.5; // Allow up to 50% more
                
                sql = "SELECT DISTINCT fn.foodid, fn.fooddescription, na.nutrientvalue " +
                      "FROM food_name fn " +
                      "JOIN nutrient_amount na ON fn.foodid = na.foodid " +
                      "JOIN conversion_factor cf ON fn.foodid = cf.foodid " +
                      "WHERE na.nutrientid = ? AND na.nutrientvalue >= ? AND na.nutrientvalue <= ? " +
                      "AND na.nutrientvalue <= 50 " +
                      "ORDER BY ABS(na.nutrientvalue - ?) ASC LIMIT 10";
            } else {
                // For decrease, look for foods that will give us close to the target decrease
                double targetValue = currentValue - targetAmount;
                double minValue = targetValue * 0.5; // Allow 50% tolerance for decrease
                double maxValue = targetValue * 1.2; // Allow up to 20% more
                
                sql = "SELECT DISTINCT fn.foodid, fn.fooddescription, na.nutrientvalue " +
                      "FROM food_name fn " +
                      "JOIN nutrient_amount na ON fn.foodid = na.foodid " +
                      "JOIN conversion_factor cf ON fn.foodid = cf.foodid " +
                      "WHERE na.nutrientid = ? AND na.nutrientvalue >= ? AND na.nutrientvalue <= ? " +
                      "ORDER BY ABS(na.nutrientvalue - ?) ASC LIMIT 10";
            }
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, nutrientId);
                if ("INCREASE".equals(direction)) {
                    double targetValue = currentValue + targetAmount;
                    double minValue = targetValue * 0.8;
                    double maxValue = targetValue * 1.5;
                    ps.setDouble(2, minValue);
                    ps.setDouble(3, maxValue);
                    ps.setDouble(4, targetValue);
                } else {
                    double targetValue = currentValue - targetAmount;
                    double minValue = targetValue * 0.5;
                    double maxValue = targetValue * 1.2;
                    ps.setDouble(2, minValue);
                    ps.setDouble(3, maxValue);
                    ps.setDouble(4, targetValue);
                }
                
                try (ResultSet rs = ps.executeQuery()) {
                    int foundCount = 0;
                    while (rs.next()) {
                        int suggestedFoodId = rs.getInt("foodid");
                        double suggestedValue = rs.getDouble("nutrientvalue");
                        double improvement = Math.abs(suggestedValue - currentValue);
                        
                        // Don't suggest the same food
                        if (suggestedFoodId == item.getFoodId()) {
                            System.out.println("DEBUG: Skipping same food " + suggestedFoodId);
                            continue;
                        }
                        
                        String suggestedFoodName = getFoodName(conn, suggestedFoodId);
                        System.out.println("DEBUG: Found candidate " + suggestedFoodName + " with " + suggestedValue + "g (improvement: " + improvement + "g, target: " + targetAmount + "g)");
                        
                        // Only consider foods with valid conversion factors
                        if (hasValidConversionFactors(conn, suggestedFoodId)) {
                            swaps.add(new SingleMealSwap(
                                item.getMealId(), currentFoodName, suggestedFoodName, 
                                improvement, item.getFoodId(), suggestedFoodId
                            ));
                            foundCount++;
                        } else {
                            System.out.println("DEBUG: Skipping " + suggestedFoodName + " - no conversion factors available");
                        }
                    }
                    System.out.println("DEBUG: Found " + foundCount + " valid swaps for " + currentFoodName);
                }
            }
            
            // If no swaps found with the targeted criteria, try a broader search
            if (swaps.isEmpty() || swaps.stream().noneMatch(s -> s.getMealId() == item.getMealId())) {
                System.out.println("DEBUG: No targeted swaps found, trying broader search...");
                
                // Look for foods with similar nutrient content but different overall nutrition
                String broaderSql = "SELECT DISTINCT fn.foodid, fn.fooddescription, na.nutrientvalue " +
                                   "FROM food_name fn " +
                                   "JOIN nutrient_amount na ON fn.foodid = na.foodid " +
                                   "JOIN conversion_factor cf ON fn.foodid = cf.foodid " +
                                   "WHERE na.nutrientid = ? AND na.nutrientvalue BETWEEN ? AND ? " +
                                   "AND fn.foodid != ? " +
                                   "ORDER BY na.nutrientvalue DESC LIMIT 10";
                
                try (PreparedStatement ps = conn.prepareStatement(broaderSql)) {
                    ps.setInt(1, nutrientId);
                    ps.setDouble(2, currentValue * 0.8); // 20% lower
                    ps.setDouble(3, currentValue * 1.2); // 20% higher
                    ps.setInt(4, item.getFoodId());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int suggestedFoodId = rs.getInt("foodid");
                            double suggestedValue = rs.getDouble("nutrientvalue");
                            double improvement = Math.abs(suggestedValue - currentValue);
                            
                            String suggestedFoodName = getFoodName(conn, suggestedFoodId);
                            System.out.println("DEBUG: Broader search found " + suggestedFoodName + " with " + suggestedValue + "g");
                            
                            // Accept smaller improvements for variety, but only if conversion factors exist
                            if (improvement >= 0.1 && hasValidConversionFactors(conn, suggestedFoodId)) {
                                swaps.add(new SingleMealSwap(
                                    item.getMealId(), currentFoodName, suggestedFoodName, 
                                    improvement, item.getFoodId(), suggestedFoodId
                                ));
                            } else if (!hasValidConversionFactors(conn, suggestedFoodId)) {
                                System.out.println("DEBUG: Skipping " + suggestedFoodName + " from broader search - no conversion factors available");
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("DEBUG: Total swaps found: " + swaps.size());
        return swaps;
    }

    private List<MultiMealSwapSuggestion> generateCombinations(List<SingleMealSwap> swaps, double targetIncrease) {
        List<MultiMealSwapSuggestion> result = new ArrayList<>();
        
        // Sort swaps by expected change (descending for increase)
        swaps.sort((a, b) -> Double.compare(b.getExpectedChange(), a.getExpectedChange()));
        
        // Try different combinations
        for (int count = 1; count <= Math.min(3, swaps.size()); count++) {
            List<MultiMealSwapSuggestion> combinations = generateCombinationsOfSize(swaps, targetIncrease, count);
            result.addAll(combinations);
        }
        
        return result;
    }

    private List<MultiMealSwapSuggestion> generateCombinationsOfSize(List<SingleMealSwap> swaps, 
                                                                    double targetIncrease, int size) {
        List<MultiMealSwapSuggestion> result = new ArrayList<>();
        
        System.out.println("DEBUG: Generating combinations of size " + size + " for target " + targetIncrease + "g");
        System.out.println("DEBUG: Available swaps: " + swaps.size());
        
        if (size == 1) {
            // Single swaps that meet or exceed the target
            for (SingleMealSwap swap : swaps) {
                // More flexible tolerance - accept any meaningful improvement
                double tolerance = Math.max(0.1, targetIncrease * 0.1); // At least 0.1g or 10% of target
                if (swap.getExpectedChange() >= tolerance) {
                    result.add(new MultiMealSwapSuggestion(Arrays.asList(swap), swap.getExpectedChange()));
                    System.out.println("DEBUG: Single swap found: " + swap.getOriginalFoodName() + " → " + swap.getSuggestedFoodName() + " (+" + swap.getExpectedChange() + "g)");
                }
            }
        } else if (size == 2) {
            // Two-meal combinations - ensure different meal items
            for (int i = 0; i < swaps.size(); i++) {
                for (int j = i + 1; j < swaps.size(); j++) {
                    SingleMealSwap swap1 = swaps.get(i);
                    SingleMealSwap swap2 = swaps.get(j);
                    
                    // Only combine if they are different meal items
                    if (swap1.getMealId() != swap2.getMealId() || swap1.getOriginalFoodId() != swap2.getOriginalFoodId()) {
                        double totalChange = swap1.getExpectedChange() + swap2.getExpectedChange();
                        
                        // More flexible tolerance for combinations
                        double minTolerance = Math.max(0.1, targetIncrease * 0.1);
                        double maxTolerance = targetIncrease * 2.0;
                        
                        if (totalChange >= minTolerance && totalChange <= maxTolerance) {
                            result.add(new MultiMealSwapSuggestion(Arrays.asList(swap1, swap2), totalChange));
                            System.out.println("DEBUG: Two-meal combination found: " + swap1.getOriginalFoodName() + " + " + swap2.getOriginalFoodName() + " = +" + totalChange + "g");
                        }
                    }
                }
            }
        } else if (size == 3) {
            // Three-meal combinations - ensure different meal items
            for (int i = 0; i < swaps.size(); i++) {
                for (int j = i + 1; j < swaps.size(); j++) {
                    for (int k = j + 1; k < swaps.size(); k++) {
                        SingleMealSwap swap1 = swaps.get(i);
                        SingleMealSwap swap2 = swaps.get(j);
                        SingleMealSwap swap3 = swaps.get(k);
                        
                        // Only combine if they are all different meal items
                        if ((swap1.getMealId() != swap2.getMealId() || swap1.getOriginalFoodId() != swap2.getOriginalFoodId()) &&
                            (swap1.getMealId() != swap3.getMealId() || swap1.getOriginalFoodId() != swap3.getOriginalFoodId()) &&
                            (swap2.getMealId() != swap3.getMealId() || swap2.getOriginalFoodId() != swap3.getOriginalFoodId())) {
                            
                            double totalChange = swap1.getExpectedChange() + swap2.getExpectedChange() + swap3.getExpectedChange();
                            
                            // More flexible tolerance for three-meal combinations
                            double minTolerance = Math.max(0.1, targetIncrease * 0.1);
                            double maxTolerance = targetIncrease * 2.0;
                            
                            if (totalChange >= minTolerance && totalChange <= maxTolerance) {
                                result.add(new MultiMealSwapSuggestion(Arrays.asList(swap1, swap2, swap3), totalChange));
                                System.out.println("DEBUG: Three-meal combination found: " + swap1.getOriginalFoodName() + " + " + swap2.getOriginalFoodName() + " + " + swap3.getOriginalFoodName() + " = +" + totalChange + "g");
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("DEBUG: Generated " + result.size() + " combinations of size " + size);
        return result;
    }

    private int getNutrientId(Connection conn, String nutrientName) throws SQLException {
        String sql = "SELECT nutrientid FROM nutrient_name WHERE nutrientname = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nutrientName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("nutrientid");
                }
            }
        }
        return -1;
    }

    private double getNutrientValue(Connection conn, int foodId, int nutrientId) throws SQLException {
        String sql = "SELECT nutrientvalue FROM nutrient_amount WHERE foodid = ? AND nutrientid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            ps.setInt(2, nutrientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("nutrientvalue");
                }
            }
        }
        return 0.0;
    }

    private String getFoodName(Connection conn, int foodId) throws SQLException {
        String sql = "SELECT fooddescription FROM food_name WHERE foodid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("fooddescription");
                }
            }
        }
        return "Unknown Food";
    }
    
    private boolean hasValidConversionFactors(Connection conn, int foodId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM conversion_factor WHERE foodid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }
} 