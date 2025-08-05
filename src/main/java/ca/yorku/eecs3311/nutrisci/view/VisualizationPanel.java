package ca.yorku.eecs3311.nutrisci.view;

import ca.yorku.eecs3311.nutrisci.controller.ChartVisualizer;
import ca.yorku.eecs3311.nutrisci.model.UserProfile;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import ca.yorku.eecs3311.nutrisci.controller.MealNutritionController;

public class VisualizationPanel extends JPanel {
    private final int userId;
    private final ChartVisualizer visualizer = new ChartVisualizer();
    private final MealNutritionController nutritionCtl = new MealNutritionController();

    private final JButton dailyBtn, compareBtn, recalculateBtn;
    private final JPanel chartContainer;

    public VisualizationPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout(10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dailyBtn   = new JButton("Daily Intake Percentage");
        compareBtn = new JButton("Before-and-After Comparison");
        recalculateBtn = new JButton("Recalculate Data");

        top.add(dailyBtn);
        top.add(compareBtn);
        top.add(recalculateBtn);
        add(top, BorderLayout.NORTH);

        chartContainer = new JPanel(new BorderLayout());
        add(chartContainer, BorderLayout.CENTER);

        dailyBtn.addActionListener(e -> showDailyChart());
        compareBtn.addActionListener(e -> showCompareChart());
        recalculateBtn.addActionListener(e -> recalculateDailySummaries());
        
        // Check data availability when panel is created
        checkMealData();
    }

    private void showDailyChart() {
        chartContainer.removeAll();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(7);
        ChartPanel chart = visualizer.createDailyPieChart(userId, start, end);
        
        // Check if chart has data by examining the chart's hasData property
        boolean hasData = false;
        try {
            Object hasDataProperty = chart.getClientProperty("hasData");
            if (hasDataProperty instanceof Boolean) {
                hasData = (Boolean) hasDataProperty;
            }
        } catch (Exception e) {
            hasData = false;
        }
        
        if (!hasData) {
            JLabel noDataLabel = new JLabel("No nutrition data available for the selected period. Please add meals first.", SwingConstants.CENTER);
            noDataLabel.setFont(new Font("Arial", Font.BOLD, 16));
            chartContainer.add(noDataLabel, BorderLayout.CENTER);
        } else {
            chartContainer.add(chart, BorderLayout.CENTER);
        }
        
        revalidate();
        repaint();
    }

    private void showCompareChart() {
        chartContainer.removeAll();
        int recommendationId = selectRecommendation(); 
        
        if (recommendationId <= 0) {
            JLabel noDataLabel = new JLabel("No recommendation selected or invalid ID.", SwingConstants.CENTER);
            noDataLabel.setFont(new Font("Arial", Font.BOLD, 16));
            chartContainer.add(noDataLabel, BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }
        
        JFreeChart chart = visualizer.createSwapComparisonChart(userId, recommendationId);
        
        if (chart != null) {
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(600, 400));
            
            // Clear previous chart and add new one
            chartContainer.removeAll();
            chartContainer.add(chartPanel);
            chartContainer.revalidate();
            chartContainer.repaint();
        } else {
            System.err.println("DEBUG: Failed to create comparison chart");
            JOptionPane.showMessageDialog(this, "Failed to create comparison chart. No valid recommendation data found.");
        }
        
        revalidate();
        repaint();
    }

    private int selectRecommendation() {
        // Fetch available recommendation IDs and nutrients from database
        StringBuilder recList = new StringBuilder();
        try {
            java.sql.Connection conn = ca.yorku.eecs3311.nutrisci.util.DBUtil.getConnection();
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery(
                "SELECT r.id, n.nutrientname, r.expected_change " +
                "FROM recommendations r " +
                "JOIN swap_goals sg ON r.goal_id = sg.id " +
                "JOIN nutrient_name n ON sg.nutrient_id = n.nutrientid " +
                "WHERE sg.user_id = " + userId + " " +
                "ORDER BY r.id DESC LIMIT 10"
            );
            recList.append("Available Recommendation IDs:\n");
            boolean hasRecommendations = false;
            while (rs.next()) {
                hasRecommendations = true;
                recList.append("ID: ").append(rs.getInt(1))
                       .append(" (Nutrient: ").append(rs.getString(2))
                       .append(", Expected Change: ").append(rs.getDouble(3))
                       .append(")\n");
            }
            rs.close();
            stmt.close();
            
            if (!hasRecommendations) {
                recList.append("No recommendations found in database.\n");
                recList.append("Please generate swap suggestions first.\n");
            }
        } catch (Exception e) {
            recList.append("Error fetching recommendations: ").append(e.getMessage()).append("\n");
            recList.append("Please try generating swap suggestions first in the Swap tab.");
        }
        
        String input = JOptionPane.showInputDialog(this, 
            recList.toString() + "\nEnter the recommendationId you want to compare:");
        try {
            return Integer.parseInt(input.trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    private void recalculateDailySummaries() {
        try {
            // First check if there are any meals for this user
            checkMealData();
            
            nutritionCtl.recalculateAllDailySummaries(userId);
            JOptionPane.showMessageDialog(this, "Daily summaries recalculated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            showDailyChart(); // Refresh the daily chart to show updated data
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error recalculating daily summaries: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void checkMealData() {
        try {
            java.sql.Connection conn = ca.yorku.eecs3311.nutrisci.util.DBUtil.getConnection();
            java.sql.Statement stmt = conn.createStatement();
            
            // Check meals
            java.sql.ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) as meal_count FROM meals WHERE user_id = " + userId
            );
            int mealCount = 0;
            if (rs.next()) {
                mealCount = rs.getInt("meal_count");
            }
            
            // Check daily summaries
            rs = stmt.executeQuery(
                "SELECT COUNT(*) as summary_count FROM daily_summary WHERE user_id = " + userId
            );
            int summaryCount = 0;
            if (rs.next()) {
                summaryCount = rs.getInt("summary_count");
            }
            
            System.out.println("DEBUG: User " + userId + " has " + mealCount + " meals and " + summaryCount + " daily summaries");
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            System.err.println("Error checking meal data: " + e.getMessage());
        }
    }
}
