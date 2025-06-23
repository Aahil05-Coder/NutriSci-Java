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
        String sql = "SELECT AVG(carbs_pct) AS carbs, " +
                     "AVG(proteins_pct) AS proteins, " +
                     "AVG(fats_pct) AS fats, " +
                     "AVG(others_pct) AS others " +
                     "FROM daily_summary " +
                     "WHERE user_id = ? AND summary_date BETWEEN ? AND ?";
        DefaultPieDataset dataset = new DefaultPieDataset();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(start));
            ps.setDate(3, java.sql.Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dataset.setValue("Carbs", rs.getDouble("carbs"));
                    dataset.setValue("Proteins", rs.getDouble("proteins"));
                    dataset.setValue("Fats", rs.getDouble("fats"));
                    dataset.setValue("Others", rs.getDouble("others"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        JFreeChart chart = ChartFactory.createPieChart(
            "Average Daily Nutrient Distribution",
            dataset,
            true, true, false
        );
        return new ChartPanel(chart);
    }


    public ChartPanel createSwapComparisonChart(int userId, int recommendationId) {
        String sql = "SELECT original_value, suggested_value, nutrient_name " +
                     "FROM recommendations r " +
                     "JOIN nutrient_name n ON r.nutrient_id = n.nutrientid " +
                     "WHERE r.id = ?";
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recommendationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nutrient = rs.getString("nutrient_name");
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
