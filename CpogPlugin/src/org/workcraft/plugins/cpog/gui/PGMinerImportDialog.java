package org.workcraft.plugins.cpog.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.workcraft.Framework;
import org.workcraft.util.DialogUtils;
import org.workcraft.util.GUI;

@SuppressWarnings("serial")
public class PGMinerImportDialog extends JDialog {

    private final JTextField filePath;
    private final JCheckBox extractConcurrencyCB, splitCB;
    private boolean canImport;

    public PGMinerImportDialog() {
        super(Framework.getInstance().getMainWindow(), "Import event log", ModalityType.APPLICATION_MODAL);

        canImport = false;

        filePath = new JTextField("", 25);
        filePath.setEditable(true);

        JButton selectFileBtn = GUI.createDialogButton("Browse for file");

        selectFileBtn.addActionListener(event -> actionSelect());

        JPanel filePanel = new JPanel();
        JPanel selPanel = new JPanel(new FlowLayout());
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.PAGE_AXIS));
        filePanel.add(filePath);
        selPanel.add(selectFileBtn);
        filePanel.add(selPanel);

        extractConcurrencyCB = new JCheckBox("Perform concurrency extraction", false);
        splitCB = new JCheckBox("Split traces into scenarios", false);

        splitCB.setEnabled(false);
        extractConcurrencyCB.addActionListener(event -> actionExtract());

        JPanel optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.PAGE_AXIS));
        optionPanel.add(extractConcurrencyCB);
        optionPanel.add(splitCB);

        JButton importButton = GUI.createDialogButton("Import");
        importButton.addActionListener(event -> actionImport());

        JButton cancelButton = GUI.createDialogButton("Cancel");
        cancelButton.addActionListener(event -> actionCancel());

        JPanel btnPanel = new JPanel();
        btnPanel.add(importButton);
        btnPanel.add(cancelButton);

        JPanel content = new JPanel(new BorderLayout());
        content.add(filePanel, BorderLayout.NORTH);
        content.add(optionPanel);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);

        getRootPane().registerKeyboardAction(event -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(Framework.getInstance().getMainWindow());
    }

    private void actionSelect() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                if (!file.exists()) {
                    throw new FileNotFoundException();
                }
                filePath.setText(file.getAbsolutePath());
            } catch (FileNotFoundException e1) {
                DialogUtils.showError(e1.getMessage());
            }
        }
    }

    private void actionExtract() {
        if (extractConcurrencyCB.isSelected()) {
            splitCB.setEnabled(true);
        } else {
            splitCB.setSelected(false);
            splitCB.setEnabled(false);
        }
    }

    private void actionCancel() {
        canImport = false;
        setVisible(false);
    }

    private void actionImport() {
        File eventLog = new File(filePath.getText());
        if (!eventLog.exists()) {
            DialogUtils.showError("The event log chosen does not exist");
        } else {
            canImport = true;
            setVisible(false);
        }
    }

    public boolean getExtractConcurrency() {
        return extractConcurrencyCB.isSelected();
    }

    public boolean getSplit() {
        return splitCB.isSelected();
    }

    public String getFilePath() {
        return filePath.getText();
    }

    public boolean getCanImport() {
        return canImport;
    }

}
