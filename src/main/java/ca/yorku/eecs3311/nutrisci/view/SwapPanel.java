


package ca.yorku.eecs3311.nutrisci.view;

import ca.yorku.eecs3311.nutrisci.controller.MealController;
import ca.yorku.eecs3311.nutrisci.controller.SwapGoalController;
import ca.yorku.eecs3311.nutrisci.model.Meal;
import ca.yorku.eecs3311.nutrisci.model.MealItem;
import ca.yorku.eecs3311.nutrisci.model.SwapGoal;
import ca.yorku.eecs3311.nutrisci.recommendation.SwapRecommender;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

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
            for (String name : nutrientMap.keySet()) {
                nutrientCombo.addItem(name);
            }
            nutrientCombo.addActionListener(e -> updateUnitCombo());
            updateUnitCombo();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load nutrients: " + e.getMessage());
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
                new Object[]{"Original Ingredient", "Suggested Ingredient", "Expected Change"}, 0
        );
        resultTable = new JTable(resultTableModel);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);

        suggestBtn = new JButton("Generate Suggestions");
        JPanel bottom = new JPanel();
        bottom.add(suggestBtn);
        add(bottom, BorderLayout.SOUTH);

        addGoalBtn.addActionListener(e -> onAddGoal());
        suggestBtn.addActionListener(e -> onSuggest());
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

            for (Meal m : meals) {
                String label = m.getMealDate().toString() + " - " + m.getMealType();
                mealSelector.addItem(label);
                mealKeyToId.put(label, m.getId());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load meals: " + e.getMessage());
        }
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
        if (selected == null || !mealKeyToId.containsKey(selected)) {
            System.out.println("DEBUG: No meal selected");
            JOptionPane.showMessageDialog(this, "Please select a meal.");
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
            return;
        }
        System.out.println("DEBUG: items.size=" + items.size());

        List<ca.yorku.eecs3311.nutrisci.recommendation.SwapRecommender.SwapSuggestion> suggestions = goalCtl.generateSuggestions(goals, items);
        for (ca.yorku.eecs3311.nutrisci.recommendation.SwapRecommender.SwapSuggestion s : suggestions) {
            resultTableModel.addRow(new Object[]{
                    s.getOriginalFoodName(),
                    s.getSuggestedFoodName(),
                    s.getExpectedChange()
            });
        }
    }
}
