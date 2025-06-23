package ca.yorku.eecs3311.nutrisci.view.model;

import ca.yorku.eecs3311.nutrisci.model.Meal;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class RecordTableModel extends AbstractTableModel {

    public static class RecordRow {
        private final Meal meal;
        private final int itemCount;
        private final double calories;

        public RecordRow(Meal meal, int itemCount, double calories) {
            this.meal = meal;
            this.itemCount = itemCount;
            this.calories = calories;
        }

        public Meal getMeal() {
            return meal;
        }

        public int getItemCount() {
            return itemCount;
        }

        public double getCalories() {
            return calories;
        }
    }

    private List<RecordRow> rows = new ArrayList<RecordRow>();
    private final String[] cols = {"Date", "Meal Type", "Items", "Calories"};

    public void setRows(List<RecordRow> rs) {
        this.rows = rs;
        fireTableDataChanged();
    }

    public RecordRow getRow(int r) {
        return rows.get(r);
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
        RecordRow rr = rows.get(r);
        switch (c) {
            case 0:
                return rr.getMeal().getMealDate();
            case 1:
                return rr.getMeal().getMealType();
            case 2:
                return rr.getItemCount();
            case 3:
                return String.format("%.2f kcal", rr.getCalories());
            default:
                return null;
        }
    }
}
