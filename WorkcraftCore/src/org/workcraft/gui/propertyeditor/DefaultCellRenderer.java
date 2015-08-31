package org.workcraft.gui.propertyeditor;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
public class DefaultCellRenderer extends JLabel implements TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (value != null) {
			String text = value.toString();
			setText(text);
			setFont(table.getFont());
			setOpaque(text.isEmpty());
		} else {
			setText("");
			setOpaque(true);
		}
		setBorder(PropertyEditorTable.BORDER_RENDER);
		return this;
	}

}
