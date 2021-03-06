package org.workcraft.gui.properties;

import org.workcraft.gui.controls.FlatComboBox;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
public class ChoiceCellRenderer extends FlatComboBox implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if (value != null) {
            addItem(value);
        }
        setOpaque(value == null);
        return this;
    }

}
