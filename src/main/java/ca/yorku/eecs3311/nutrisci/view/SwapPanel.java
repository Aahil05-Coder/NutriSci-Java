


package ca.yorku.eecs3311.nutrisci.view;

import ca.yorku.eecs3311.nutrisci.controller.MealController;
import ca.yorku.eecs3311.nutrisci.controller.SwapGoalController;
import ca.yorku.eecs3311.nutrisci.model.Meal;
import ca.yorku.eecs3311.nutrisci.model.MealItem;
import ca.yorku.eecs3311.nutrisci.model.SwapGoal;
import ca.yorku.eecs3311.nutrisci.recommendation.SwapRecommender;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.DefaultCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import ca.yorku.eecs3311.nutrisci.util.DBUtil;

public class SwapPanel extends JPanel {
    private final int userId;
    private final SwapGoalController goalCtl = new SwapGoalController();

    private final JComboBox<String> nutrientCombo;
    private final JComboBox<String> directionCombo;
    private final JTextField amountField;
    private final JComboBox<String> unitCombo;
    private final JComboBox<String> mealSelector;
    private final JButton addGoalBtn;
    private final DefaultListModel<SwapGoal> goalListModel;
    private final JList<SwapGoal> goalList;
    private final JButton suggestBtn;
    private final JTable resultTable;
    private final DefaultTableModel resultTableModel;

    private Map<String, Integer> nutrientMap;
    private Map<Integer, String> nutrientUnits;
    private Map<String, Integer> mealKeyToId;

    public SwapPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout(10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        nutrientCombo = new JComboBox<>();
        directionCombo = new JComboBox<>(new String[]{"INCREASE", "DECREASE"});
        amountField = new JTextField(5);
        unitCombo = new JComboBox<>();
        mealSelector = new JComboBox<>();
        addGoalBtn = new JButton("Add Goal");

        top.add(new JLabel("Meal:"));
        top.add(mealSelector);

        top.add(new JLabel("Nutrient:"));
        nutrientCombo.setPreferredSize(new Dimension(200, 25));
        nutrientCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    label.setToolTipText(value.toString());
                }
                return label;
            }
        });
        nutrientCombo.addActionListener(e -> {
            Object selected = nutrientCombo.getSelectedItem();
            if (selected != null) {
                nutrientCombo.setToolTipText(selected.toString());
            }
        });
        top.add(nutrientCombo);
        top.add(directionCombo);
        top.add(new JLabel("Amount:"));
        top.add(amountField);
        top.add(unitCombo);
        top.add(addGoalBtn);
        top.add(new JLabel("(Tip: Up to two goals allowed. Right-click a goal to delete it)"));
        add(top, BorderLayout.NORTH);

        try {
            nutrientMap = goalCtl.getNutrientNameToIdMap();
            nutrientUnits = goalCtl.getAllNutrientUnits();
            
            if (nutrientMap.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Warning: No nutrients found in database. Please check database initialization.");
            } else {
                for (String name : nutrientMap.keySet()) {
                    nutrientCombo.addItem(name);
                }
                System.out.println("DEBUG: Loaded " + nutrientMap.size() + " nutrients from database");
            }
            
            nutrientCombo.addActionListener(e -> updateUnitCombo());
            updateUnitCombo();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load nutrients: " + e.getMessage());
            e.printStackTrace();
            nutrientMap = new HashMap<>();
            nutrientUnits = new HashMap<>();
        }

        mealKeyToId = new HashMap<>();
        loadMeals();

        goalListModel = new DefaultListModel<>();
        goalList = new JList<>(goalListModel);
        goalList.setToolTipText("Right-click on a target to delete it.");
        goalList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane goalScroll = new JScrollPane(goalList);
        goalScroll.setPreferredSize(new Dimension(300, 0));
        add(goalScroll, BorderLayout.WEST);
        loadSavedGoals();

        goalList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && goalList.locationToIndex(e.getPoint()) != -1) {
                    goalList.setSelectedIndex(goalList.locationToIndex(e.getPoint()));
                    SwapGoal selected = goalList.getSelectedValue();
                    if (selected != null) {
                        int result = JOptionPane.showConfirmDialog(SwapPanel.this,
                                "Do you want to delete this goal?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                goalCtl.deleteGoal(selected);
                                goalListModel.removeElement(selected);
                            } catch (SQLException ex) {
                                JOptionPane.showMessageDialog(SwapPanel.this, "Delete Failed: " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        });

        resultTableModel = new DefaultTableModel(
                new Object[]{"Original Ingredient", "Suggested Ingredient", "Expected Change", "Action"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only the Action column is editable
            }
        };
        resultTable = new JTable(resultTableModel);
        resultTable.setRowHeight(35);
        
        // Add button renderer and editor for the Action column
        resultTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        resultTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox()));
        
        add(new JScrollPane(resultTable), BorderLayout.CENTER);

        suggestBtn = new JButton("Generate Suggestions");
        JPanel bottom = new JPanel();
        bottom.add(suggestBtn);
        add(bottom, BorderLayout.SOUTH);

        addGoalBtn.addActionListener(e -> onAddGoal());
        suggestBtn.addActionListener(e -> onSuggest());
        
        // Add a refresh button to reload meals
        JButton refreshBtn = new JButton("Refresh Meals");
        refreshBtn.addActionListener(e -> {
            loadMeals();
            JOptionPane.showMessageDialog(this, "Meals refreshed!");
        });
        bottom.add(refreshBtn);
    }

    private void updateUnitCombo() {
        String selected = (String) nutrientCombo.getSelectedItem();
        unitCombo.removeAllItems();
        if (selected != null && nutrientMap.containsKey(selected)) {
            int nid = nutrientMap.get(selected);
            String unit = nutrientUnits.getOrDefault(nid, "unit");
            unitCombo.addItem("%");
            unitCombo.addItem(unit);
        }
    }

    private void loadSavedGoals() {
        try {
            List<SwapGoal> saved = goalCtl.getGoalsForUser(userId);
            for (SwapGoal g : saved) {
                goalListModel.addElement(g);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load goals: " + e.getMessage());
        }
    }

    private void loadMeals() {
        try {
            MealController mealCtl = new MealController();
            List<Meal> meals = mealCtl.getMeals(userId);
            mealSelector.removeAllItems();
            mealKeyToId.clear();

            if (meals.isEmpty()) {
                mealSelector.addItem("No meals available - Please add meals first");
                System.out.println("DEBUG: No meals found for user " + userId);
            } else {
                for (Meal m : meals) {
                    String label = m.getMealDate().toString() + " - " + m.getMealType();
                    mealSelector.addItem(label);
                    mealKeyToId.put(label, m.getId());
                }
                System.out.println("DEBUG: Loaded " + meals.size() + " meals for user " + userId);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load meals: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void refreshMeals() {
        loadMeals();
    }

    private void onAddGoal() {
        if (goalListModel.size() >= 2) {
            JOptionPane.showMessageDialog(this, "You can set up to two goals only.");
            return;
        }

        String nut = (String) nutrientCombo.getSelectedItem();
        String dir = (String) directionCombo.getSelectedItem();
        String unit = (String) unitCombo.getSelectedItem();
        double amt;

        try {
            amt = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
            return;
        }

        SwapGoal goal = new SwapGoal(userId, nut, dir, amt, "", unit);
        goalListModel.addElement(goal);

        try {
            goalCtl.insertGoal(goal);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Save Goal Failed: " + e.getMessage());
        }
    }

    private void onSuggest() {
        System.out.println("DEBUG: onSuggest called");
        resultTableModel.setRowCount(0);
        List<SwapGoal> goals = new ArrayList<>();
        for (int i = 0; i < goalListModel.size(); i++) {
            goals.add(goalListModel.get(i));
        }
        System.out.println("DEBUG: goals.size=" + goals.size());

        // If no goals, add a default goal for testing
        if (goals.isEmpty()) {
            String nut = (String) nutrientCombo.getSelectedItem();
            String dir = (String) directionCombo.getSelectedItem();
            String unit = (String) unitCombo.getSelectedItem();
            double amt = 10.0;
            try {
                amt = Double.parseDouble(amountField.getText().trim());
            } catch (Exception ignored) {}
            SwapGoal defaultGoal = new SwapGoal(userId, nut, dir, amt, "", unit);
            goals.add(defaultGoal);
            System.out.println("DEBUG: Added default goal: " + nut + ", " + dir + ", " + amt + unit);
        }

        String selected = (String) mealSelector.getSelectedItem();
        if (selected == null || selected.equals("No meals available - Please add meals first")) {
            System.out.println("DEBUG: No meal selected or no meals available");
            JOptionPane.showMessageDialog(this, "Please add meals first before generating swap suggestions.");
            return;
        }
        
        if (!mealKeyToId.containsKey(selected)) {
            System.out.println("DEBUG: Invalid meal selection");
            JOptionPane.showMessageDialog(this, "Please select a valid meal.");
            return;
        }

        int mealId = mealKeyToId.get(selected);
        System.out.println("DEBUG: mealId=" + mealId);
        MealController mealCtl = new MealController();
        List<ca.yorku.eecs3311.nutrisci.model.MealItem> items;
        try {
            items = mealCtl.getMealItems(mealId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load meal items: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        System.out.println("DEBUG: items.size=" + items.size());
        
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The selected meal has no ingredients to swap.");
            return;
        }

        List<ca.yorku.eecs3311.nutrisci.recommendation.SwapRecommender.SwapSuggestion> suggestions = goalCtl.generateSuggestions(goals, items);
        
        if (suggestions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No swap suggestions found for the current goals and meal.");
            return;
        }
        
        for (ca.yorku.eecs3311.nutrisci.recommendation.SwapRecommender.SwapSuggestion s : suggestions) {
            resultTableModel.addRow(new Object[]{
                    s.getOriginalFoodName(),
                    s.getSuggestedFoodName(),
                    s.getExpectedChange(),
                    "Apply Swap"
            });
        }
        
        JOptionPane.showMessageDialog(this, "Generated " + suggestions.size() + " swap suggestions!");
    }
    
    // Button renderer for the Action column
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Apply Swap");
            return this;
        }
    }

    // Button editor for the Action column
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow; // Store the current row index

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row; // Store the row index
            label = "Apply Swap";
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Handle the swap application here
                applySwap(currentRow);
            }
            isPushed = false;
            return label;
        }

        private void applySwap(int row) {
            try {
                String originalIngredient = (String) resultTable.getValueAt(row, 0);
                String suggestedIngredient = (String) resultTable.getValueAt(row, 1);
                Double expectedChange = (Double) resultTable.getValueAt(row, 2);
                
                int choice = JOptionPane.showConfirmDialog(SwapPanel.this,
                    "Apply this swap?\n\n" +
                    "Original: " + originalIngredient + "\n" +
                    "Suggested: " + suggestedIngredient + "\n" +
                    "Expected Change: " + expectedChange,
                    "Confirm Swap",
                    JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    // Get the selected meal
                    String selected = (String) mealSelector.getSelectedItem();
                    if (selected == null || !mealKeyToId.containsKey(selected)) {
                        JOptionPane.showMessageDialog(SwapPanel.this, "No meal selected for swap.");
                        return;
                    }
                    
                    int mealId = mealKeyToId.get(selected);
                    
                    // Use a single connection for the entire swap operation
                    try (Connection conn = DBUtil.getConnection()) {
                        conn.setAutoCommit(true);
                        
                        // Find the food ID for the suggested ingredient
                        int suggestedFoodId = findFoodIdByName(conn, suggestedIngredient);
                        if (suggestedFoodId == -1) {
                            JOptionPane.showMessageDialog(SwapPanel.this, "Could not find suggested food in database.");
                            return;
                        }
                        
                        // Find the food ID of the original ingredient
                        int originalFoodId = findFoodIdByName(conn, originalIngredient);
                        if (originalFoodId == -1) {
                            JOptionPane.showMessageDialog(SwapPanel.this, "Could not find original food in database.");
                            return;
                        }
                        
                        // Update the meal item with the suggested food
                        String updateSql = "UPDATE meal_items SET food_id = ? WHERE meal_id = ? AND food_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                            ps.setInt(1, suggestedFoodId);
                            ps.setInt(2, mealId);
                            ps.setInt(3, originalFoodId);
                            int rowsAffected = ps.executeUpdate();
                            
                            System.out.println("DEBUG: Updated meal item - rows affected: " + rowsAffected);
                            
                            if (rowsAffected > 0) {
                                JOptionPane.showMessageDialog(SwapPanel.this, 
                                    "Swap applied successfully!\n" +
                                    "Your meal has been updated with: " + suggestedIngredient);
                                
                                // Refresh the meals list
                                loadMeals();
                                
                                // Clear the suggestions table
                                resultTableModel.setRowCount(0);
                            } else {
                                JOptionPane.showMessageDialog(SwapPanel.this, 
                                    "Failed to apply swap. No matching meal item found.");
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(SwapPanel.this, 
                    "Failed to apply swap: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        
        private int findFoodIdByName(Connection conn, String foodName) throws SQLException {
            String sql = "SELECT foodid FROM food_name WHERE fooddescription = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, foodName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("foodid");
                    }
                }
            }
            return -1;
        }
    }
}
