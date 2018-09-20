package org.workcraft.gui.propertyeditor;

import org.workcraft.Config;
import org.workcraft.Framework;
import org.workcraft.PluginManager;
import org.workcraft.dom.visual.SizeHelper;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.PluginInfo;
import org.workcraft.util.DialogUtils;
import org.workcraft.util.GUI;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SettingsEditorDialog extends JDialog {
    private static final String DIALOG_RESTORE_SETTINGS = "Restore settings";
    private static final long serialVersionUID = 1L;
    private JButton restoreButton;
    private DefaultMutableTreeNode sectionRoot;
    private JTree sectionTree;
    private final PropertyEditorTable propertiesTable;

    private Settings currentPage;
    private Config currentConfig;

    static class SettingsPageNode {
        private final Settings page;

        SettingsPageNode(Settings page) {
            this.page = page;
        }

        @Override
        public String toString() {
            return page.getName();
        }

        public Settings getPage() {
            return page;
        }
    }

    public SettingsEditorDialog(MainWindow owner) {
        super(owner);

        propertiesTable = new PropertyEditorTable();
        propertiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setModal(true);
        setResizable(true);
        setTitle("Preferences");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                actionOk();
            }
        });

        Dimension minSize = new Dimension(500, 200);
        setMinimumSize(minSize);
        GUI.centerAndSizeToParent(this, owner);

        initComponents();
        loadSections();
    }

    public DefaultMutableTreeNode getSectionNode(DefaultMutableTreeNode node, String section) {
        if (section == null) {
            return node;
        }
        int dotPos = section.indexOf('.');
        String thisLevel = (dotPos < 0) ? section : section.substring(0, dotPos);
        String nextLevel = (dotPos < 0) ? null : section.substring(dotPos + 1);
        DefaultMutableTreeNode thisLevelNode = null;

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (!(child.getUserObject() instanceof String)) {
                continue;
            }
            if (child.getUserObject().equals(thisLevel)) {
                thisLevelNode = child;
                break;
            }
        }

        if (thisLevelNode == null) {
            thisLevelNode = new DefaultMutableTreeNode(thisLevel);
        }
        node.add(thisLevelNode);

        if (nextLevel == null) {
            return thisLevelNode;
        } else {
            return getSectionNode(thisLevelNode, nextLevel);
        }
    }

    private void addItem(String section, Settings item) {
        DefaultMutableTreeNode sectionNode = getSectionNode(sectionRoot, section);
        sectionNode.add(new DefaultMutableTreeNode(new SettingsPageNode(item)));
    }

    private void loadSections() {
        final Framework framework = Framework.getInstance();
        PluginManager pm = framework.getPluginManager();
        ArrayList<Settings> settings = getSortedPluginSettings(pm.getSettingsPlugins());

        // Add settings to the tree
        for (Settings s: settings) {
            addItem(s.getSection(), s);
        }

        sectionTree.setModel(new DefaultTreeModel(sectionRoot));

        // Expand all tree branches
        for (int i = 0; i < sectionTree.getRowCount(); i++) {
            final TreePath treePath = sectionTree.getPathForRow(i);
            sectionTree.expandPath(treePath);
        }
        setObject(null);
    }

    private ArrayList<Settings> getSortedPluginSettings(Collection<PluginInfo<? extends Settings>> plugins) {
        ArrayList<Settings> settings = new ArrayList<>();
        for (PluginInfo<? extends Settings> info : plugins) {
            settings.add(info.getSingleton());
        }

        // Sort settings by (Sections + Name) strings
        Collections.sort(settings, (o1, o2) -> {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            String s1 = o1.getSection();
            String s2 = o2.getSection();
            if (s1 == null) return -1;
            if (s2 == null) return 1;
            if (s1.equals(s2)) {
                String n1 = o1.getName();
                String n2 = o2.getName();
                if (n1 == null) return -1;
                if (n2 == null) return 1;
                return n1.compareTo(n2);
            }
            return s1.compareTo(s2);
        });
        return settings;
    }

    private void setObject(Settings page) {
        if (page == null) {
            currentConfig = null;
            restoreButton.setText("Restore defaults (all)");
        } else {
            currentConfig = new Config();
            page.save(currentConfig);
            restoreButton.setText("Restore defaults");
        }
        currentPage = page;
        propertiesTable.setObject(currentPage);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);
        JScrollPane sectionPane = new JScrollPane();
        sectionRoot = new DefaultMutableTreeNode("root");

        sectionTree = new JTree();
        sectionTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        sectionTree.setRootVisible(false);
        sectionTree.setShowsRootHandles(true);

        sectionTree.addTreeSelectionListener(e -> {
            Object userObject = ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();
            if (userObject instanceof SettingsPageNode) {
                Settings page = ((SettingsPageNode) userObject).getPage();
                setObject(page);
            } else {
                setObject(null);
            }
        });

        sectionPane.setViewportView(sectionTree);
        sectionPane.setMinimumSize(new Dimension(100, 0));
        sectionPane.setBorder(SizeHelper.getTitledBorder("Section"));

        JScrollPane propertiesPane = new JScrollPane();
        propertiesPane.setMinimumSize(new Dimension(250, 0));
        propertiesPane.setBorder(SizeHelper.getTitledBorder("Selection properties"));
        propertiesPane.setViewportView(propertiesTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sectionPane, propertiesPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation((int) Math.round(0.3 * getWidth()));
        splitPane.setResizeWeight(0.1);

        JButton okButton = GUI.createDialogButton("OK");
        okButton.addActionListener(event -> actionOk());

        JButton cancelButton = GUI.createDialogButton("Cancel");
        cancelButton.addActionListener(event -> actionCancel());

        restoreButton = GUI.createDialogButton("Restore defaults (all)");
        restoreButton.addActionListener(event -> actionRestore());

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.CENTER, SizeHelper.getLayoutHGap(), SizeHelper.getLayoutVGap()));
        buttonsPane.add(okButton);
        buttonsPane.add(cancelButton);
        buttonsPane.add(restoreButton);
        contentPane.add(splitPane, BorderLayout.CENTER);
        contentPane.add(buttonsPane, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);

        getRootPane().registerKeyboardAction(event -> actionOk(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        getRootPane().registerKeyboardAction(event -> actionCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void actionOk() {
        setObject(null);
        setVisible(false);
    }

    private void actionCancel() {
        if ((currentPage != null) && (currentConfig != null)) {
            currentPage.load(currentConfig);
        }
        setObject(null);
        setVisible(false);
    }

    private void actionRestore() {
        if (currentPage != null) {
            currentPage.load(new Config());
            setObject(currentPage);
        } else {
            final Framework framework = Framework.getInstance();
            String msg = "This will reset all the settings to defaults.\n" + "Continue?";
            if (DialogUtils.showConfirmWarning(msg, DIALOG_RESTORE_SETTINGS, false)) {
                framework.resetConfig();
            }
        }
    }

}
