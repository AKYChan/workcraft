package org.workcraft.plugins.cpog.properties;

import java.awt.Component;

import javax.swing.JTable;

import org.workcraft.gui.properties.GenericCellEditor;
import org.workcraft.plugins.cpog.Encoding;

class EncodingCellEditor extends GenericCellEditor {

    private static final long serialVersionUID = 8L;
    private Encoding encoding;

    @Override
    public Encoding getCellEditorValue() {
        encoding.updateEncoding((String) super.getCellEditorValue());
        return encoding;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value instanceof Encoding) {
            encoding = (Encoding) value;
        }
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

}
