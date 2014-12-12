/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

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
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import org.workcraft.Framework;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.plugins.PluginInfo;
import org.workcraft.util.GUI;

public class CreateWorkDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JPanel optionsPane;
	private JPanel buttonsPane;

	private JList modelList;

	private JButton okButton;
	private JButton cancelButton;
	private JScrollPane modelScroll ;
	private JCheckBox chkVisual;
	private JCheckBox chkOpen;
	private JTextField txtTitle;

	private int modalResult = 0;

	public CreateWorkDialog(MainWindow owner) {
		super(owner);

		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setModal(true);
		setTitle("New work");

		GUI.centerAndSizeToParent(this, owner);
		initComponents();
	}

	static class ListElement implements Comparable<ListElement> {
		public ModelDescriptor descriptor;

		public ListElement(ModelDescriptor descriptor) {
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
		contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);

		modelScroll = new JScrollPane();
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
				if ((e.getClickCount() == 2 ) && (modelList.getSelectedIndex() != -1)) {
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
		final Collection<PluginInfo<? extends ModelDescriptor>> modelDescriptors = framework.getPluginManager().getPlugins(ModelDescriptor.class);
		ArrayList<ListElement> elements = new ArrayList<ListElement>();

		for(PluginInfo<? extends ModelDescriptor> plugin : modelDescriptors) {
			elements.add(new ListElement(plugin.newInstance()));
		}

		Collections.sort(elements);
		for (ListElement element : elements) {
			listModel.addElement(element);
		}

		modelScroll.setViewportView(modelList);
		modelScroll.setBorder(BorderFactory.createTitledBorder("Type"));

		optionsPane = new JPanel();
		optionsPane.setBorder(BorderFactory.createTitledBorder("Creation options"));
		optionsPane.setLayout(new BoxLayout(optionsPane, BoxLayout.Y_AXIS));

		chkVisual = new JCheckBox("create visual model");

		chkVisual.setSelected(true);

		chkOpen = new JCheckBox("open in editor");
		chkOpen.setSelected(true);

		optionsPane.add(chkVisual);
		optionsPane.add(chkOpen);
		optionsPane.add(new JLabel("Title: "));
		txtTitle = new JTextField();
		//txtTitle.setMaximumSize(new Dimension(1000,20));
		optionsPane.add(txtTitle);

		JPanel dummy = new JPanel();
		dummy.setPreferredSize(new Dimension(200,1000));
		dummy.setMaximumSize(new Dimension(200,1000));

		optionsPane.add(dummy);

		buttonsPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

		okButton = new JButton();
		okButton.setPreferredSize(new Dimension(100, 20));
		okButton.setEnabled(false);
		okButton.setText("OK");
		okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				ok();
			}
		});

		cancelButton = new JButton();
		cancelButton.setPreferredSize(new Dimension(100, 20));
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				cancel();
			}
		});

		buttonsPane.add(okButton);
		buttonsPane.add(cancelButton);

		contentPane.add(modelScroll, BorderLayout.CENTER);
		contentPane.add(optionsPane, BorderLayout.WEST);
		contentPane.add(buttonsPane, BorderLayout.SOUTH);

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
		return ((ListElement)modelList.getSelectedValue()).descriptor;
	}

	public int getModalResult() {
		return modalResult;
	}

	public boolean createVisualSelected(){
		return chkVisual.isSelected();
	}

	public boolean openInEditorSelected() {
		return chkOpen.isSelected();
	}

	public String getModelTitle() {
		return txtTitle.getText();
	}
}