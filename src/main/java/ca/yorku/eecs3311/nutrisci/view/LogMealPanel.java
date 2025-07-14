package ca.yorku.eecs3311.nutrisci.view;

    import ca.yorku.eecs3311.nutrisci.controller.MealController;
    import ca.yorku.eecs3311.nutrisci.controller.MealNutritionController;
    import ca.yorku.eecs3311.nutrisci.model.Meal;
    import ca.yorku.eecs3311.nutrisci.model.MealItem;
    import ca.yorku.eecs3311.nutrisci.model.Food;
    import ca.yorku.eecs3311.nutrisci.model.Measure;
    import ca.yorku.eecs3311.nutrisci.dao.*;

    import ca.yorku.eecs3311.nutrisci.view.model.MealTableModel;
    import ca.yorku.eecs3311.nutrisci.view.model.RecordTableModel;
    import ca.yorku.eecs3311.nutrisci.view.model.RecordTableModel.RecordRow;
    import ca.yorku.eecs3311.nutrisci.view.editor.FoodComboBoxCellEditor;
    import ca.yorku.eecs3311.nutrisci.view.editor.MeasureComboBoxCellEditor;

    import javax.swing.*;
    import java.awt.*;
    import java.awt.event.MouseAdapter;
    import java.awt.event.MouseEvent;
    import java.time.LocalDate;
    import java.time.format.DateTimeParseException;
    import java.util.List;
    import java.util.ArrayList;
    import java.util.Map;

    public class LogMealPanel extends JPanel {

        private final JTextField dateField;
        private final JComboBox<String> mealTypeCombo;

        private final MealTableModel entryModel = new MealTableModel();
        private final JTable entryTable = new JTable(entryModel);

        private final RecordTableModel recordModel = new RecordTableModel();
        private final JTable recordTable = new JTable(recordModel);

        private final MealController mealCtl = new MealController();
        private final MealNutritionController nutritionCtl = new MealNutritionController();
        private final int uid;

        public LogMealPanel(int userId) {
            this.uid = userId;
            setLayout(new BorderLayout(6, 6));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            top.add(new JLabel("Date (YYYY-MM-DD):"));
            dateField = new JTextField(LocalDate.now().toString(), 10);
            top.add(dateField);
            top.add(new JLabel("Meal Type:"));
            mealTypeCombo = new JComboBox<>(new String[]{"BREAKFAST", "LUNCH", "DINNER", "SNACK"});
            top.add(mealTypeCombo);
            entryTable.setRowHeight(28); 
            entryTable.getColumnModel().getColumn(0).setCellEditor(new FoodComboBoxCellEditor());
            entryTable.getColumnModel().getColumn(2).setCellEditor(new MeasureComboBoxCellEditor());
            entryModel.addEmptyRow();

            JButton addRow = new JButton("Add Row");
            addRow.addActionListener(e -> {
                if (entryTable.isEditing()) entryTable.getCellEditor().cancelCellEditing();
                entryModel.addEmptyRow();
            });

            JButton delRow = new JButton("Delete Row");
            delRow.addActionListener(e -> {
                if (entryTable.isEditing()) entryTable.getCellEditor().cancelCellEditing();
                int r = entryTable.getSelectedRow();
                if (r >= 0) entryModel.removeRow(r);
            });

            JButton saveBtn = new JButton("Save Records");
            saveBtn.addActionListener(e -> saveMeal());

            JPanel entryBtnPanel = new JPanel(new GridLayout(1, 3, 5, 5));
            entryBtnPanel.add(addRow);
            entryBtnPanel.add(delRow);
            entryBtnPanel.add(saveBtn);

            JPanel entryPanel = new JPanel(new BorderLayout(5, 5));
            entryPanel.add(top, BorderLayout.NORTH);
            entryPanel.add(new JScrollPane(entryTable), BorderLayout.CENTER);
            entryPanel.add(entryBtnPanel, BorderLayout.SOUTH);

            JPanel recordPanel = new JPanel(new BorderLayout(5, 5));
            recordPanel.setBorder(BorderFactory.createTitledBorder("Saved Meals"));
            recordPanel.add(new JScrollPane(recordTable), BorderLayout.CENTER);
            JButton delMeal = new JButton("Delete Meal");
            delMeal.addActionListener(e -> deleteSelectedMeal());
            JPanel recSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            recSouth.add(delMeal);
            recordPanel.add(recSouth, BorderLayout.SOUTH);

            recordTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            recordTable.setRowHeight(28); 
            recordTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1 && recordTable.getSelectedRow() != -1) {
                        int row = recordTable.getSelectedRow();
                        RecordRow rr = recordModel.getRow(row);
                        showMealDetail(rr.getMeal().getId());
                    }
                }
            });

            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, entryPanel, recordPanel);
            split.setResizeWeight(0.5);
            add(split, BorderLayout.CENTER);

            refreshMeals();
        }

        private void saveMeal() {
            try {
                if (entryTable.isEditing()) entryTable.getCellEditor().stopCellEditing();
                LocalDate d = LocalDate.parse(dateField.getText().trim());
                String mealType = (String) mealTypeCombo.getSelectedItem();
                List<MealItem> items = entryModel.toMealItems();
                if (items.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter at least one valid record.");
                    return;
                }
                mealCtl.saveMeal(uid, d, mealType, items);
                JOptionPane.showMessageDialog(this, "Saved successfully.");
                entryModel.reset();
                refreshMeals();
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format (YYYY-MM-DD).");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        private void refreshMeals() {
            try {
                List<Meal> meals = mealCtl.getMeals(uid);
                List<RecordRow> rows = new ArrayList<>();
                for (Meal m : meals) {
                    List<MealItem> items = mealCtl.getMealItems(m.getId());
                    double totalCalories = nutritionCtl.calculateTotalCalories(items);
                    rows.add(new RecordRow(m, items.size(), totalCalories));
                }
                recordModel.setRows(rows);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void deleteSelectedMeal() {
            int r = recordTable.getSelectedRow();
            if (r < 0) return;
            RecordRow rr = recordModel.getRow(r);
            try {
                mealCtl.deleteMeal(rr.getMeal().getId());
                refreshMeals();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void showMealDetail(int mealId) {
            try {
                List<MealItem> items = mealCtl.getMealItems(mealId);
                String[] cols = {"Ingredient", "Quantity", "Unit"};
                Object[][] data = new Object[items.size()][3];
                for (int i = 0; i < items.size(); i++) {
                    MealItem mi = items.get(i);
                    Food f = new FoodNameDAO().searchById(mi.getFoodId());
                    Measure m = MeasureNameDAO.getById(mi.getMeasureId());
                    data[i][0] = f != null ? f.getDescription() : ("#" + mi.getFoodId());
                    data[i][1] = mi.getQuantity();
                    data[i][2] = m != null ? m.getDescription() : ("#" + mi.getMeasureId());
                }

                JTable detailTable = new JTable(data, cols);
                detailTable.setRowHeight(28);
                detailTable.setEnabled(false);

                Map<Integer, Double> nutrientSum = nutritionCtl.calculateNutrientSummary(items);
                NutrientNameDAO nameDAO = new NutrientNameDAO();
                String[] nutrientCols = {"Nutrient", "Amount", "Unit"};
                List<Object[]> rows = new ArrayList<>();

                for (Map.Entry<Integer, Double> entry : nutrientSum.entrySet()) {
                    if (entry.getValue() < 0.01) continue;
                    String name = nameDAO.getNutrientNameById(entry.getKey());
                    String unit = nameDAO.getUnitByNutrientId(entry.getKey());
                    rows.add(new Object[]{name, String.format("%.2f", entry.getValue()), unit});
                }

                JTable nutrientTable = new JTable(rows.toArray(new Object[0][0]), nutrientCols);
                nutrientTable.setRowHeight(28);
                nutrientTable.setAutoCreateRowSorter(true);
                nutrientTable.setEnabled(false);

                JPanel fullPanel = new JPanel(new BorderLayout(10, 10));
                fullPanel.add(new JLabel("Meal Items:"), BorderLayout.NORTH);
                fullPanel.add(new JScrollPane(detailTable), BorderLayout.CENTER);
                fullPanel.add(new JScrollPane(nutrientTable), BorderLayout.SOUTH);

                JOptionPane.showMessageDialog(this, fullPanel, "Meal Details", JOptionPane.PLAIN_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load details: " + ex.getMessage());
            }
        }
    }