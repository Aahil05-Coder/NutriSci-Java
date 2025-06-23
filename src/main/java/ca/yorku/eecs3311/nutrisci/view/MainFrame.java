package ca.yorku.eecs3311.nutrisci.view;

import ca.yorku.eecs3311.nutrisci.model.UserProfile;

import java.awt.BorderLayout;

import javax.swing.*;

public class MainFrame extends JFrame {
    private UserProfile user;

    public MainFrame(UserProfile profile) {
        super("NutriSci: SwEATch to better! — " + profile.getUsername());
        this.user = profile;
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private MainFrame() {

        throw new IllegalStateException("Use MainFrame(UserProfile) instead");
    }

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Profile", new ProfilePanel(user));
        tabs.addTab("Log Meal", new LogMealPanel(user.getId()));
        tabs.addTab("Swap", new SwapPanel(user.getId()));
        tabs.addTab("Visualization", new VisualizationPanel(user.getId()));

        getContentPane().add(tabs, BorderLayout.CENTER);
    }
}