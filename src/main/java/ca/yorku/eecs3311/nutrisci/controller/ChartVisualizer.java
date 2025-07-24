package ca.yorku.eecs3311.nutrisci.controller;

import ca.yorku.eecs3311.nutrisci.util.DBUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

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
                    
                    // Check if any of the values are not null and greater than 0
                    if (!rs.wasNull() && (carbs > 0 || proteins > 0 || fats > 0 || others > 0)) {
                        hasData = true;
                        
                        // Only add non-zero values to the chart
                        if (carbs > 0) {
                            dataset.setValue("Carbs", carbs);
                        }
                        if (proteins > 0) {
                            dataset.setValue("Proteins", proteins);
                        }
                        if (fats > 0) {
                            dataset.setValue("Fats", fats);
                        }
                        if (others > 0) {
                            dataset.setValue("Others", others);
                        }
                    } else {
                        System.out.println("DEBUG: All values are null or zero");
                    }
                } else {
                    System.out.println("DEBUG: No results found for the date range");
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


    public ChartPanel createSwapComparisonChart(int userId, int recommendationId) {
        System.out.println("DEBUG: createSwapComparisonChart userId=" + userId + ", recommendationId=" + recommendationId);
        String sql = "SELECT original_value, suggested_value, nutrientname " +
                     "FROM recommendations r " +
                     "JOIN nutrient_name n ON r.nutrient_id = n.nutrientid " +
                     "WHERE r.id = ?";
        System.out.println("DEBUG: SQL=" + sql);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recommendationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("DEBUG: BarChart row: nutrient=" + rs.getString("nutrientname") + ", original=" + rs.getDouble("original_value") + ", suggested=" + rs.getDouble("suggested_value"));
                    String nutrient = rs.getString("nutrientname");
                    dataset.addValue(rs.getDouble("original_value"), "Before", nutrient);
                    dataset.addValue(rs.getDouble("suggested_value"), "After", nutrient);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        JFreeChart barChart = ChartFactory.createBarChart(
            "Swap Nutrient Comparison", 
            "Nutrient",              
            "Amount",  
            dataset
        );
        return new ChartPanel(barChart);
    }
}
