package org.workcraft.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import org.workcraft.Framework;
import org.workcraft.PluginManager;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.dom.visual.SizeHelper;
import org.workcraft.plugins.PluginInfo;
import org.workcraft.util.GUI;

public class CreateWorkDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private JList modelList;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox chkVisual;
    private JCheckBox chkOpen;
    private JTextField txtTitle;
    private int modalResult = 0;

    public CreateWorkDialog(MainWindow owner) {
        super(owner);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setModal(true);
        setTitle("New work");

        setMinimumSize(new Dimension(300, 200));
        GUI.centerAndSizeToParent(this, owner);

        initComponents();
    }

    static class ListElement implements Comparable<ListElement> {
        public ModelDescriptor descriptor;

        ListElement(ModelDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public String toString() {
            return descriptor.getDisplayName();
        }

        @Override
        public int compareTo(ListElement o) {
            return toString().compareTo(o.toString());
        }
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        JScrollPane modelScroll = new JScrollPane();
        DefaultListModel listModel = new DefaultListModel();

        modelList = new JList(listModel);
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modelList.setLayoutOrientation(JList.VERTICAL_WRAP);
        modelList.setVisibleRowCount(0);

        modelList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (modelList.getSelectedIndex() == -1) {
                    okButton.setEnabled(false);
                } else {
                    okButton.setEnabled(true);
                }
            }
        }
        );

        modelList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (modelList.getSelectedIndex() != -1)) {
                    ok();
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
            }
            @Override
            public void mouseExited(MouseEvent e) {
            }
            @Override
            public void mousePressed(MouseEvent e) {
            }
            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });

        final Framework framework = Framework.getInstance();
        PluginManager pm = framework.getPluginManager();
        final Collection<PluginInfo<? extends ModelDescriptor>> modelDescriptors = pm.getPlugins(ModelDescriptor.class);
        ArrayList<ListElement> elements = new ArrayList<>();

        for (PluginInfo<? extends ModelDescriptor> plugin : modelDescriptors) {
            elements.add(new ListElement(plugin.newInstance()));
        }

        Collections.sort(elements);
        for (ListElement element : elements) {
            listModel.addElement(element);
        }

        modelScroll.setViewportView(modelList);
        modelScroll.setBorder(BorderFactory.createTitledBorder("Type"));
        modelScroll.setMinimumSize(new Dimension(150, 0));
        modelScroll.setPreferredSize(new Dimension(250, 0));

        JPanel optionsPane = new JPanel();
        optionsPane.setBorder(BorderFactory.createTitledBorder("Creation options"));
        optionsPane.setLayout(new BoxLayout(optionsPane, BoxLayout.Y_AXIS));
        optionsPane.setMinimumSize(new Dimension(150, 0));
        optionsPane.setPreferredSize(new Dimension(250, 0));

        chkVisual = new JCheckBox("create visual model");
        chkVisual.setSelected(true);

        chkOpen = new JCheckBox("open in editor");
        chkOpen.setSelected(true);

        optionsPane.add(chkVisual);
        optionsPane.add(chkOpen);
        optionsPane.add(new JLabel("Title: "));
        txtTitle = new JTextField();
        //txtTitle.setMaximumSize(new Dimension(1000, 20));
        optionsPane.add(txtTitle);

        JPanel dummy = new JPanel();
        dummy.setPreferredSize(new Dimension(200, 1000));
        dummy.setMaximumSize(new Dimension(200, 1000));
        optionsPane.add(dummy);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, optionsPane, modelScroll);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation((int) Math.round(0.3 * getWidth()));
        splitPane.setResizeWeight(0.1);

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.CENTER, SizeHelper.getLayoutHGap(), SizeHelper.getLayoutVGap()));

        okButton = GUI.createDialogButton("OK");
        okButton.setEnabled(false);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton = GUI.createDialogButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        buttonsPane.add(okButton);
        buttonsPane.add(cancelButton);
        contentPane.add(splitPane, BorderLayout.CENTER);
        contentPane.add(buttonsPane, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);

        getRootPane().registerKeyboardAction(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ok();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        getRootPane().registerKeyboardAction(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        cancel();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void ok() {
        if (okButton.isEnabled()) {
            modalResult = 1;
            setVisible(false);
        }
    }

    private void cancel() {
        if (cancelButton.isEnabled()) {
            modalResult = 0;
            setVisible(false);
        }
    }

    public ModelDescriptor getSelectedModel() {
        return ((ListElement) modelList.getSelectedValue()).descriptor;
    }

    public int getModalResult() {
        return modalResult;
    }

    public boolean createVisualSelected() {
        return chkVisual.isSelected();
    }

    public boolean openInEditorSelected() {
        return chkOpen.isSelected();
    }

    public String getModelTitle() {
        return txtTitle.getText();
    }
}
