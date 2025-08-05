


package ca.yorku.eecs3311.nutrisci.view;

import ca.yorku.eecs3311.nutrisci.controller.MealController;
import ca.yorku.eecs3311.nutrisci.controller.SwapGoalController;
import ca.yorku.eecs3311.nutrisci.controller.MealNutritionController;
import ca.yorku.eecs3311.nutrisci.model.Meal;
import ca.yorku.eecs3311.nutrisci.model.MealItem;
import ca.yorku.eecs3311.nutrisci.model.SwapGoal;
import ca.yorku.eecs3311.nutrisci.recommendation.AdvancedSwapRecommender;
import ca.yorku.eecs3311.nutrisci.recommendation.AdvancedSwapRecommender.MultiMealSwapSuggestion;
import ca.yorku.eecs3311.nutrisci.recommendation.AdvancedSwapRecommender.SingleMealSwap;

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
import java.sql.Statement;

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
    
    private List<MultiMealSwapSuggestion> storedSuggestions = new ArrayList<>();

    public SwapPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout(10, 10));

        // Goal creation panel
        JPanel goalPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        goalPanel.setBorder(BorderFactory.createTitledBorder("Create Swap Goal"));
        
        goalPanel.add(new JLabel("Nutrient:"));
        nutrientCombo = new JComboBox<>(new String[]{"PROTEIN", "CARBS", "FATS", "CALORIES"});
        goalPanel.add(nutrientCombo);
        
        goalPanel.add(new JLabel("Direction:"));
        directionCombo = new JComboBox<>(new String[]{"INCREASE", "DECREASE"});
        goalPanel.add(directionCombo);
        
        goalPanel.add(new JLabel("Amount (g):"));
        amountField = new JTextField("20");
        goalPanel.add(amountField);
        
        goalPanel.add(new JLabel("Unit:"));
        unitCombo = new JComboBox<>(new String[]{"g", "mg", "kcal"});
        goalPanel.add(unitCombo);
        
        addGoalBtn = new JButton("Add Goal");
        goalPanel.add(addGoalBtn);
        
        // Meal selector panel
        JPanel mealPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        mealPanel.add(new JLabel("Meal:"));
        mealSelector = new JComboBox<>();
        mealPanel.add(mealSelector);
        
        // Top panel combining goal and meal panels
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(goalPanel, BorderLayout.CENTER);
        topPanel.add(mealPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);

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
                new Object[]{"Multi-Meal Swap Description", "Total Expected Change", "Action"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only the Action column is editable
            }
        };
        resultTable = new JTable(resultTableModel);
        resultTable.setRowHeight(35);
        
        // Add button renderer and editor for the Action column
        resultTable.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer());
        resultTable.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor(new JCheckBox()));
        
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
                    System.out.println("DEBUG: Added meal to selector: " + label + " (ID: " + m.getId() + ")");
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

        // Validate the amount is reasonable
        if (amt < 1.0) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a reasonable amount (at least 1g).\n" +
                "Small amounts like " + amt + "g may not provide meaningful changes.",
                "Amount Too Small", 
                JOptionPane.WARNING_MESSAGE);
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
            double amt = 10.0; // Default to 10g instead of user input
            
            try {
                double userInput = Double.parseDouble(amountField.getText().trim());
                // Use user input only if it's reasonable (at least 1g)
                if (userInput >= 1.0) {
                    amt = userInput;
                } else {
                    System.out.println("DEBUG: User input " + userInput + " too small, using default " + amt + "g");
                }
            } catch (Exception ignored) {
                System.out.println("DEBUG: Invalid user input, using default " + amt + "g");
            }
            
            SwapGoal defaultGoal = new SwapGoal(userId, nut, dir, amt, "", unit);
            goals.add(defaultGoal);
            System.out.println("DEBUG: Added default goal: " + nut + ", " + dir + ", " + amt + unit);
        }

        // Get ALL meal items for the user, not just from one meal
        MealController mealCtl = new MealController();
        List<ca.yorku.eecs3311.nutrisci.model.MealItem> allMealItems;
        try {
            allMealItems = mealCtl.getAllMealItemsForUser(userId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load meal items: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        System.out.println("DEBUG: allMealItems.size=" + allMealItems.size());
        
        if (allMealItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No meal items found. Please add meals first.");
            return;
        }

        // Use the advanced recommender for multi-meal swaps
        AdvancedSwapRecommender advancedRecommender = new AdvancedSwapRecommender();
        List<MultiMealSwapSuggestion> suggestions = advancedRecommender.suggestMultiMealSwaps(goals, allMealItems, userId);
        
        if (suggestions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No swap suggestions found for the current goals and meals.");
            return;
        }
        
        // Store suggestions for later use
        storedSuggestions = suggestions;
        
        for (MultiMealSwapSuggestion suggestion : suggestions) {
            resultTableModel.addRow(new Object[]{
                    suggestion.getDescription(),
                    suggestion.getTotalExpectedChange(),
                    "Apply Multi-Swap"
            });
        }
        
        JOptionPane.showMessageDialog(this, "Generated " + suggestions.size() + " multi-meal swap suggestions!");
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
                // Safety check to prevent ArrayIndexOutOfBoundsException
                if (row < 0 || row >= resultTableModel.getRowCount()) {
                    System.err.println("Invalid row index: " + row + ", table has " + resultTableModel.getRowCount() + " rows");
                    return;
                }
                
                // Get the multi-meal swap suggestion
                if (row >= storedSuggestions.size()) {
                    JOptionPane.showMessageDialog(SwapPanel.this, "Invalid suggestion index.");
                    return;
                }
                
                MultiMealSwapSuggestion suggestion = storedSuggestions.get(row);
                String description = suggestion.getDescription();
                double totalExpectedChange = suggestion.getTotalExpectedChange();
                
                int choice = JOptionPane.showConfirmDialog(SwapPanel.this,
                    "Apply this multi-meal swap?\n\n" +
                    description + "\n" +
                    "Total Expected Change: " + totalExpectedChange + "g",
                    "Confirm Multi-Swap",
                    JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    // Apply all the swaps in the suggestion
                    boolean allSuccessful = true;
                    StringBuilder results = new StringBuilder();
                    
                    try (Connection conn = DBUtil.getConnection()) {
                        conn.setAutoCommit(false); // Start transaction
                        
                        for (SingleMealSwap singleSwap : suggestion.getMealSwaps()) {
                            try {
                                // Check if the meal item still exists with the original food
                                String checkSql = "SELECT id FROM meal_items WHERE meal_id = ? AND food_id = ?";
                                int mealItemId = -1;
                                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                                    checkPs.setInt(1, singleSwap.getMealId());
                                    checkPs.setInt(2, singleSwap.getOriginalFoodId());
                                    try (ResultSet checkRs = checkPs.executeQuery()) {
                                        if (checkRs.next()) {
                                            mealItemId = checkRs.getInt("id");
                                        }
                                    }
                                }
                                
                                if (mealItemId == -1) {
                                    results.append("✗ Meal item not found: ").append(singleSwap.getOriginalFoodName()).append("\n");
                                    allSuccessful = false;
                                    continue;
                                }
                                
                                // Store recommendation BEFORE updating the meal item
                                // This ensures we capture the original food ID correctly
                                try {
                                    storeRecommendation(conn, singleSwap.getMealId(), 
                                                      singleSwap.getOriginalFoodId(), 
                                                      singleSwap.getSuggestedFoodId(), 
                                                      singleSwap.getExpectedChange());
                                    System.out.println("DEBUG: Stored recommendation BEFORE swap - original_food_id=" + singleSwap.getOriginalFoodId() + 
                                                     ", suggested_food_id=" + singleSwap.getSuggestedFoodId());
                                } catch (Exception e) {
                                    System.err.println("DEBUG: Failed to store recommendation: " + e.getMessage());
                                    // Continue with the swap even if recommendation storage fails
                                }
                                
                                // Find a compatible measure for the suggested food
                                int compatibleMeasureId;
                                try {
                                    compatibleMeasureId = findCompatibleMeasure(conn, singleSwap.getSuggestedFoodId());
                                } catch (SQLException e) {
                                    results.append("✗ No compatible measure found for: ").append(singleSwap.getSuggestedFoodName()).append(" - ").append(e.getMessage()).append("\n");
                                    allSuccessful = false;
                                    continue;
                                }
                                
                                double conversionFactor = getConversionFactor(conn, singleSwap.getSuggestedFoodId(), compatibleMeasureId);
                                double targetWeight = 10.0;
                                double quantity = Math.max(1.0, targetWeight / conversionFactor);
                                
                                // Update the meal item using the meal item ID instead of meal_id + food_id
                                String updateSql = "UPDATE meal_items SET food_id = ?, measure_id = ?, quantity = ? WHERE id = ?";
                                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                                    ps.setInt(1, singleSwap.getSuggestedFoodId());
                                    ps.setInt(2, compatibleMeasureId);
                                    ps.setDouble(3, quantity);
                                    ps.setInt(4, mealItemId);
                                    int rowsAffected = ps.executeUpdate();
                                    
                                    if (rowsAffected > 0) {
                                        results.append("✓ ").append(singleSwap.getOriginalFoodName())
                                               .append(" → ").append(singleSwap.getSuggestedFoodName())
                                               .append("\n");
                                    } else {
                                        results.append("✗ Failed to update: ").append(singleSwap.getOriginalFoodName()).append("\n");
                                        allSuccessful = false;
                                    }
                                }
                            } catch (Exception e) {
                                results.append("✗ Error updating: ").append(singleSwap.getOriginalFoodName())
                                       .append(" - ").append(e.getMessage()).append("\n");
                                allSuccessful = false;
                            }
                        }
                        
                        if (allSuccessful) {
                            conn.commit();
                            JOptionPane.showMessageDialog(SwapPanel.this, 
                                "Multi-meal swap applied successfully!\n\n" + results.toString());
                            
                            // Auto-recalculate all meals for the user
                            recalculateAllMeals();
                                
                                // Refresh the meals list
                                loadMeals();
                            } else {
                            conn.rollback();
                                JOptionPane.showMessageDialog(SwapPanel.this, 
                                "Some swaps failed. Changes rolled back.\n\n" + results.toString());
                        }
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(SwapPanel.this, 
                    "Failed to apply multi-meal swap: " + ex.getMessage());
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

        private int findCompatibleMeasure(Connection conn, int foodId) throws SQLException {
            // First try to find a measure that gives a reasonable serving size (between 10g and 200g)
            String sql = "SELECT cf.measureid, cf.conversionfactorvalue, mn.measuredescription " +
                        "FROM conversion_factor cf " +
                        "JOIN measure_name mn ON cf.measureid = mn.measureid " +
                        "WHERE cf.foodid = ? AND cf.conversionfactorvalue BETWEEN 10 AND 200 " +
                        "ORDER BY cf.conversionfactorvalue ASC " +
                        "LIMIT 1";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, foodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int measureId = rs.getInt("measureid");
                        double factor = rs.getDouble("conversionfactorvalue");
                        String description = rs.getString("measuredescription");
                        System.out.println("DEBUG: Selected measure " + measureId + " (" + description + ") with factor " + factor + " for food " + foodId);
                        return measureId;
                    }
                }
            }
            
            // If no reasonable measure found, try to find any measure and adjust quantity
            String fallbackSql = "SELECT cf.measureid, cf.conversionfactorvalue, mn.measuredescription " +
                               "FROM conversion_factor cf " +
                               "JOIN measure_name mn ON cf.measureid = mn.measureid " +
                               "WHERE cf.foodid = ? " +
                               "ORDER BY cf.conversionfactorvalue DESC " +
                               "LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                ps.setInt(1, foodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int measureId = rs.getInt("measureid");
                        double factor = rs.getDouble("conversionfactorvalue");
                        String description = rs.getString("measuredescription");
                        System.out.println("DEBUG: Selected fallback measure " + measureId + " (" + description + ") with factor " + factor + " for food " + foodId);
                        return measureId;
                    }
                }
            }
            
            // If no conversion factor found at all, throw an exception instead of returning a default
            throw new SQLException("No conversion factor found for foodId = " + foodId + ". This food cannot be used for swaps.");
        }

        private double getConversionFactor(Connection conn, int foodId, int measureId) throws SQLException {
            String sql = "SELECT conversionfactorvalue FROM conversion_factor WHERE foodid = ? AND measureid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, foodId);
                ps.setInt(2, measureId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("conversionfactorvalue");
                    }
                }
            }
            return 1.0; // Default to 1.0 if no conversion factor found
        }

        private int findOrCreateSwapGoal(Connection conn, String nutrientName, String direction, double amount) throws SQLException {
            String nutrientSql = "SELECT nutrientid FROM nutrient_name WHERE nutrientname = ?";
            int nutrientId = -1;
            try (PreparedStatement ps = conn.prepareStatement(nutrientSql)) {
                ps.setString(1, nutrientName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nutrientId = rs.getInt("nutrientid");
                    }
                }
            }

            if (nutrientId == -1) {
                System.err.println("DEBUG: Nutrient not found: " + nutrientName);
                return -1; // Indicate failure
            }

            String findGoalSql = "SELECT id FROM swap_goals WHERE user_id = ? AND nutrient_id = ? AND direction = ? AND amount = ? AND unit = ?";
            int goalId = -1;
            try (PreparedStatement ps = conn.prepareStatement(findGoalSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, nutrientId);
                ps.setString(3, direction);
                ps.setDouble(4, amount);
                ps.setString(5, "g"); // Assuming unit is always "g" for now
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        goalId = rs.getInt("id");
                    }
                }
            }

            if (goalId == -1) {
                String insertGoalSql = "INSERT INTO swap_goals (user_id, nutrient_id, direction, amount, unit, intensity) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertGoalSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, nutrientId);
                    ps.setString(3, direction);
                    ps.setDouble(4, amount);
                    ps.setString(5, "g");
                    ps.setString(6, "MODERATE"); // Default intensity
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            goalId = rs.getInt(1);
                        }
                    }
                }
            }
            return goalId;
        }

        private void storeRecommendation(Connection conn, int mealId, int originalFoodId, int suggestedFoodId, double expectedChange) throws SQLException {
            // Get the current goal information
            String nutrientName = "PROTEIN"; // Default
            String direction = "INCREASE"; // Default
            
            if (!goalListModel.isEmpty()) {
                SwapGoal currentGoal = goalListModel.get(0); // Use the first goal
                nutrientName = currentGoal.getNutrient();
                direction = currentGoal.getDirection();
            }
            
            // First, find or create a swap goal
            int goalId = findOrCreateSwapGoal(conn, nutrientName, direction, expectedChange);
            
            // Find the meal item ID for the original food
            String findItemSql = "SELECT id FROM meal_items WHERE meal_id = ? AND food_id = ?";
            int originalItemId = -1;
            try (PreparedStatement ps = conn.prepareStatement(findItemSql)) {
                ps.setInt(1, mealId);
                ps.setInt(2, originalFoodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        originalItemId = rs.getInt("id");
                    }
                }
            }
            
            if (originalItemId == -1) {
                System.err.println("DEBUG: Could not find meal item for meal_id=" + mealId + ", food_id=" + originalFoodId);
                return;
            }
            
            // Store the recommendation with correct original and suggested food IDs
            // We need to store the original food ID separately since the meal item will be updated
            String insertSql = "INSERT INTO recommendations (goal_id, original_item_id, suggested_food_id, expected_change, original_food_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, goalId);
                ps.setInt(2, originalItemId);
                ps.setInt(3, suggestedFoodId);
                ps.setDouble(4, expectedChange);
                ps.setInt(5, originalFoodId); // Store the original food ID separately
                int rowsAffected = ps.executeUpdate();
                System.out.println("DEBUG: Stored recommendation in database with goal_id=" + goalId + 
                                 ", original_item_id=" + originalItemId + 
                                 ", suggested_food_id=" + suggestedFoodId + 
                                 ", original_food_id=" + originalFoodId +
                                 ", expected_change=" + expectedChange + 
                                 " (rows affected: " + rowsAffected + ")");
            }
        }

        private void recalculateAllMeals() {
            try {
                MealNutritionController nutritionCtl = new MealNutritionController();
                nutritionCtl.recalculateAllDailySummaries(userId);
                System.out.println("DEBUG: Auto-recalculated all meals for user " + userId);
            } catch (Exception e) {
                System.err.println("ERROR: Failed to recalculate meals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}



