package ca.yorku.eecs3311.nutrisci.view.editor;

import ca.yorku.eecs3311.nutrisci.dao.FoodNameDAO;
import ca.yorku.eecs3311.nutrisci.model.Food;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.*;

public class FoodComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JComboBox<Food> combo = new JComboBox<Food>();
    private final DefaultComboBoxModel<Food> model = new DefaultComboBoxModel<Food>();
    private SwingWorker<List<Food>, Void> worker;
    private boolean programmatic = false;
    private String lastQuery = "";

    public FoodComboBoxCellEditor() {
        combo.setModel(model);
        combo.setEditable(true);
        combo.setMaximumRowCount(10);
        combo.setLightWeightPopupEnabled(false);

        JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
        Timer debounce = new Timer(300, e -> doSearch(editor.getText()));
        debounce.setRepeats(false);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });

        combo.addActionListener(e -> {
            if (programmatic) return;
            if (!combo.isPopupVisible() && combo.getSelectedItem() instanceof Food) {
                if (worker != null && !worker.isDone()) worker.cancel(true);
                model.removeAllElements(); combo.hidePopup(); lastQuery = ""; stopCellEditing();
            }
        });
    }

    private void doSearch(final String key) {
        if (key.equals(lastQuery)) return;
        lastQuery = key;
        if (worker != null && !worker.isDone()) worker.cancel(true);
        if (key.trim().isEmpty()) { model.removeAllElements(); combo.hidePopup(); return; }

        worker = new SwingWorker<List<Food>, Void>() {
            protected List<Food> doInBackground() {
                try {
                    return new FoodNameDAO().searchByDescription(key);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return Collections.emptyList();
                }
            }

            protected void done() {
                if (isCancelled() || !key.equals(lastQuery)) return;
                try {
                    List<Food> list = get();
                    programmatic = true;
                    model.removeAllElements();
                    for (Food f : list) model.addElement(f);
                    combo.setSelectedIndex(-1);
                    combo.getEditor().setItem(key);
                    programmatic = false;
                    if (!list.isEmpty() && combo.isShowing()) combo.showPopup();
                    else combo.hidePopup();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        programmatic = true;
        combo.setSelectedItem(value instanceof Food ? value : "");
        programmatic = false;
        return combo;
    }

    public Object getCellEditorValue() {
        Object v = combo.getEditor().getItem();
        return (v instanceof Food) ? v : null;
    }
}
