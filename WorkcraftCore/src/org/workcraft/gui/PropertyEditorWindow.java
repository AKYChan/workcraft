package org.workcraft.gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.workcraft.gui.propertyeditor.Properties;
import org.workcraft.gui.propertyeditor.PropertyEditorTable;

@SuppressWarnings("serial")
public class PropertyEditorWindow extends JPanel {
    private final PropertyEditorTable propertyTable;
    private final JScrollPane scrollProperties;

    public PropertyEditorWindow() {
        propertyTable = new PropertyEditorTable();

        scrollProperties = new JScrollPane();
        scrollProperties.setViewportView(propertyTable);

        setLayout(new BorderLayout(0, 0));
        add(new DisabledPanel(), BorderLayout.CENTER);
        validate();
    }

    public Properties getObject() {
        return propertyTable.getObject();
    }

    public void setObject(Properties o) {
        removeAll();
        propertyTable.setObject(o);
        add(scrollProperties, BorderLayout.CENTER);
        validate();
        repaint();
    }

    public void clearObject() {
        if (propertyTable.getObject() != null) {
            removeAll();
            propertyTable.clearObject();
            add(new DisabledPanel(), BorderLayout.CENTER);
            validate();
            repaint();
        }
    }

}
