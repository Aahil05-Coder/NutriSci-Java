package ca.yorku.eecs3311.nutrisci.view;

import ca.yorku.eecs3311.nutrisci.controller.ChartVisualizer;
import ca.yorku.eecs3311.nutrisci.model.UserProfile;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class VisualizationPanel extends JPanel {
    private final int userId;
    private final ChartVisualizer visualizer = new ChartVisualizer();

    private final JButton dailyBtn, compareBtn;
    private final JPanel chartContainer;

    public VisualizationPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout(10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dailyBtn   = new JButton("Daily Intake Percentage");
        compareBtn = new JButton("Before-and-After Comparison");

        top.add(dailyBtn);
        top.add(compareBtn);
        add(top, BorderLayout.NORTH);

        chartContainer = new JPanel(new BorderLayout());
        add(chartContainer, BorderLayout.CENTER);

        dailyBtn.addActionListener(e -> showDailyChart());
        compareBtn.addActionListener(e -> showCompareChart());
    }

    private void showDailyChart() {
        chartContainer.removeAll();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(7);
        ChartPanel chart = visualizer.createDailyPieChart(userId, start, end);
        chartContainer.add(chart, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showCompareChart() {
        chartContainer.removeAll();
        int recommendationId = selectRecommendation(); 
        ChartPanel chart = visualizer.createSwapComparisonChart(userId, recommendationId);
        chartContainer.add(chart, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private int selectRecommendation() {
        // Fetch available recommendation IDs and nutrients
        StringBuilder recList = new StringBuilder();
        try {
            java.sql.Connection conn = ca.yorku.eecs3311.nutrisci.util.DBUtil.getConnection();
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery("SELECT r.id, n.nutrientname FROM recommendations r JOIN nutrient_name n ON r.nutrient_id = n.nutrientid ORDER BY r.id DESC LIMIT 10");
            recList.append("Available Recommendation IDs:\n");
            while (rs.next()) {
                recList.append("ID: ").append(rs.getInt(1)).append(" (Nutrient: ").append(rs.getString(2)).append(")\n");
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            recList.append("(Could not fetch recommendations)");
        }
        String input = JOptionPane.showInputDialog(this, recList.toString() + "\nEnter the recommendationId you want to compare:");
        try {
            return Integer.parseInt(input.trim());
        } catch (Exception ex) {
            return -1;
        }
    }
}
