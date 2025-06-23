package ca.yorku.eecs3311.nutrisci.view.model;

import ca.yorku.eecs3311.nutrisci.model.Food;
import ca.yorku.eecs3311.nutrisci.model.Measure;
import ca.yorku.eecs3311.nutrisci.model.MealItem;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class MealTableModel extends AbstractTableModel {
    private final List<Object[]> rows = new ArrayList<Object[]>();
    private final String[] cols = {"Ingredient", "Quantity", "Unit"};

    public void addEmptyRow() {
        rows.add(new Object[]{null, "", null});
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void removeRow(int r) {
        if (r >= 0 && r < rows.size()) {
            rows.remove(r);
            fireTableRowsDeleted(r, r);
        }
    }

    public void reset() {
        rows.clear();
        fireTableDataChanged();
        addEmptyRow();
    }

    public List<MealItem> toMealItems() {
        List<MealItem> list = new ArrayList<MealItem>();
        for (Object[] r : rows) {
            if (r[0] instanceof Food && r[2] instanceof Measure) {
                try {
                    double q = Double.parseDouble(r[1].toString());
                    Food f = (Food) r[0];
                    Measure m = (Measure) r[2];

                    MealItem mi = new MealItem();
                    mi.setFoodId(f.getId());
                    mi.setQuantity(q);
                    mi.setMeasureId(m.getMeasureId());

                    list.add(mi);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return list;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return cols.length;
    }

    @Override
    public String getColumnName(int c) {
        return cols[c];
    }

    @Override
    public Object getValueAt(int r, int c) {
        return rows.get(r)[c];
    }

    @Override
    public void setValueAt(Object v, int r, int c) {
        rows.get(r)[c] = v;
        fireTableCellUpdated(r, c);
    }

    @Override
    public boolean isCellEditable(int r, int c) {
        return true;
    }

    @Override
    public Class<?> getColumnClass(int c) {
        if (c == 1) return String.class;
        else if (c == 2) return Measure.class;
        else return Object.class;
    }
}
