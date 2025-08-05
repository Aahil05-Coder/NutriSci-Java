package ca.yorku.eecs3311.nutrisci.controller;

import ca.yorku.eecs3311.nutrisci.util.DBUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.plot.PlotOrientation;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class ChartVisualizer {

    public ChartPanel createDailyPieChart(int userId, LocalDate start, LocalDate end) {
        System.out.println("DEBUG: createDailyPieChart userId=" + userId + ", start=" + start + ", end=" + end);
        String sql = "SELECT AVG(carbs_pct) AS carbs, " +
                     "AVG(proteins_pct) AS proteins, " +
                     "AVG(fats_pct) AS fats, " +
                     "AVG(others_pct) AS others " +
                     "FROM daily_summary " +
                     "WHERE user_id = ? AND summary_date BETWEEN ? AND ?";
        System.out.println("DEBUG: SQL=" + sql);
        DefaultPieDataset dataset = new DefaultPieDataset();
        boolean hasData = false;
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(start));
            ps.setDate(3, java.sql.Date.valueOf(end));
            
            // First check if there are any records in the daily_summary table for this user
            String checkSql = "SELECT COUNT(*) as count FROM daily_summary WHERE user_id = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, userId);
                try (ResultSet checkRs = checkPs.executeQuery()) {
                    if (checkRs.next()) {
                        int totalRecords = checkRs.getInt("count");
                        System.out.println("DEBUG: Total daily_summary records for user " + userId + ": " + totalRecords);
                    }
                }
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double carbs = rs.getDouble("carbs");
                    double proteins = rs.getDouble("proteins");
                    double fats = rs.getDouble("fats");
                    double others = rs.getDouble("others");
                    
                    System.out.println("DEBUG: PieChart results: carbs=" + carbs + ", proteins=" + proteins + ", fats=" + fats + ", others=" + others);
                    
                    // Check if any of the values are meaningful (not null and greater than 0)
                    boolean hasCarbs = !rs.wasNull() && carbs > 0;
                    boolean hasProteins = !rs.wasNull() && proteins > 0;
                    boolean hasFats = !rs.wasNull() && fats > 0;
                    boolean hasOthers = !rs.wasNull() && others > 0;
                    
                    if (hasCarbs || hasProteins || hasFats || hasOthers) {
                        hasData = true;
                        
                        // Only add non-zero values to the chart
                        if (hasCarbs) {
                            dataset.setValue("Carbs", carbs);
                        }
                        if (hasProteins) {
                            dataset.setValue("Proteins", proteins);
                        }
                        if (hasFats) {
                            dataset.setValue("Fats", fats);
                        }
                        if (hasOthers) {
                            dataset.setValue("Others", others);
                        }
                    } else {
                        System.out.println("DEBUG: All values are null or zero - no meaningful data");
                        hasData = false;
                    }
                } else {
                    System.out.println("DEBUG: No results found for the date range");
                    hasData = false;
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR in createDailyPieChart: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("DEBUG: hasData = " + hasData + ", dataset item count = " + dataset.getItemCount());
        
        JFreeChart chart = ChartFactory.createPieChart(
            "Average Daily Nutrient Distribution",
            dataset,
            true, true, false
        );
        
        // Store the hasData flag in a custom property of the ChartPanel
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.putClientProperty("hasData", hasData);
        
        return chartPanel;
    }


    public JFreeChart createSwapComparisonChart(int userId, int recommendationId) {
        try (Connection conn = DBUtil.getConnection()) {
            // Get recommendation details including original and suggested food IDs
            String sql = "SELECT r.original_item_id, r.suggested_food_id, r.original_food_id, r.expected_change, " +
                        "sg.nutrient_id, n.nutrientname " +
                        "FROM recommendations r " +
                        "JOIN swap_goals sg ON r.goal_id = sg.id " +
                        "JOIN nutrient_name n ON sg.nutrient_id = n.nutrientid " +
                        "WHERE r.id = ?";
            
            int originalFoodId = -1;
            int suggestedFoodId = -1;
            int nutrientId = -1;
            String nutrientName = "";
            double expectedChange = 0.0;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, recommendationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        originalFoodId = rs.getInt("original_food_id"); // Use the stored original food ID
                        suggestedFoodId = rs.getInt("suggested_food_id");
                        nutrientId = rs.getInt("nutrient_id");
                        nutrientName = rs.getString("nutrientname");
                        expectedChange = rs.getDouble("expected_change");
                        
                        System.out.println("DEBUG: Found recommendation - original_food_id=" + originalFoodId + 
                                         ", suggested_food_id=" + suggestedFoodId + 
                                         ", nutrient=" + nutrientName);
                    }
                }
            }
            
            if (originalFoodId == -1 || suggestedFoodId == -1 || nutrientId == -1) {
                System.err.println("DEBUG: Invalid recommendation data");
                return null;
            }
            
            // Get nutrient values for original and suggested foods
            double originalValue = getNutrientValue(conn, originalFoodId, nutrientId);
            double suggestedValue = getNutrientValue(conn, suggestedFoodId, nutrientId);
            
            System.out.println("DEBUG: Values - original=" + originalValue + ", suggested=" + suggestedValue);
            
            // Create the dataset
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dataset.addValue(originalValue, "Before", nutrientName);
            dataset.addValue(suggestedValue, "After", nutrientName);
            
            // Create the chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Swap Nutrient Comparison",
                nutrientName,
                "Amount (g)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Customize the chart appearance
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, Color.RED);  // Before
            renderer.setSeriesPaint(1, Color.BLUE); // After
            
            return chart;
            
        } catch (SQLException e) {
            System.err.println("ERROR in createSwapComparisonChart: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
}
