package ca.yorku.eecs3311.nutrisci.view.editor;

import ca.yorku.eecs3311.nutrisci.dao.ConversionFactorDAO;
import ca.yorku.eecs3311.nutrisci.model.Food;
import ca.yorku.eecs3311.nutrisci.model.Measure;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class MeasureComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JComboBox<Measure> combo = new JComboBox<Measure>();
    private final DefaultComboBoxModel<Measure> model = new DefaultComboBoxModel<Measure>();
    private int rowIndex = -1;

    public MeasureComboBoxCellEditor() {
        combo.setModel(model);
        combo.setEditable(false);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        model.removeAllElements();
        this.rowIndex = row;

        Object foodObj = table.getValueAt(row, 0);
        if (foodObj instanceof Food) {
            Food food = (Food) foodObj;
            try {
                List<Measure> measures = new ConversionFactorDAO().getMeasuresForFood(food.getId());
                for (Measure m : measures) {
                    model.addElement(m);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (value instanceof Measure) {
            combo.setSelectedItem(value);
        } else if (model.getSize() > 0) {
            combo.setSelectedIndex(0);
        }

        return combo;
    }

    @Override
    public Object getCellEditorValue() {
        return combo.getSelectedItem();
    }
}
