package org.workcraft.plugins.son.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.workcraft.plugins.son.ONGroup;
import org.workcraft.plugins.son.SON;
import org.workcraft.plugins.son.StructureVerifySettings;
import org.workcraft.util.GUI;


@SuppressWarnings("rawtypes")
public class StructureVerifyDialog extends JDialog{

	protected SON net;
	private static final long serialVersionUID = 1L;

	protected JPanel  groupButtonsPanel, groupSelectionPanel, groupPanel, typePanel, settingPanel, confirmButtonsPanel;
	protected JButton runButton, cancelButton;
	protected JComboBox typeCombo;
	protected JList groupList;
	protected JCheckBox highLight, outputBefore;

	protected ArrayList<ONGroup> seletedGroups = new ArrayList<ONGroup>();
	protected Font font = new Font("Arial", Font.PLAIN, 12);
	protected int run = 0;
	protected Window owner;

	class typeMode {
		public int value;
		public String description;

		public typeMode(int value, String description) {
			this.value = value;
			this.description = description;
		}

		public String toString() {
			return description;
		}
	}

	class CheckListItem
	{
		private String label;
		private boolean isSelected = true;
		private ONGroup group;

		public CheckListItem(String label, ONGroup group){
			this.label = label;
			this.group = group;
		}

		public boolean isSelected(){
			return isSelected;
		}

		public void setSelected(boolean isSelected){
			this.isSelected = isSelected;
		}

		public String toString(){
			return label;
		}

		public ONGroup getGroup(){
			return group;
		}

		public void setGroupColor(Color color){
			group.setForegroundColor(color);
		}
	}

	class CheckListRenderer extends JCheckBox implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(
				JList list, Object value, int index,
				boolean isSelected, boolean hasFocus) {

			setEnabled(list.isEnabled());
			setSelected(((CheckListItem)value).isSelected());
			setFont(list.getFont());

			setBackground(list.getBackground());
			setForeground(list.getForeground());
			setText(value.toString());
			return this;
		}
	}

	protected void createGroupPanel(){

		groupPanel = new JPanel(new BorderLayout(10, 10));
		groupPanel.setBorder(BorderFactory.createTitledBorder(groupPanelTitle()));

		createGroupItemsPanel();
		createGroupButtonsPanel();

		groupPanel.add(groupSelectionPanel,BorderLayout.WEST );
		groupPanel.add(groupButtonsPanel, BorderLayout.CENTER);

	}

	@SuppressWarnings("unchecked")
	protected void createTypePanel(){
		typePanel = new JPanel();

		typeCombo = new JComboBox();
		typeCombo.addItem(new typeMode(0, "Structured Occurrence Nets"));
		typeCombo.addItem(new typeMode(1, "Occurrence Net (Group)"));
		typeCombo.addItem(new typeMode(2, "Communication Structured Occurrence Nets"));
		typeCombo.addItem(new typeMode(3, "Behavioural Structured Occurrence Nets"));
		typeCombo.addItem(new typeMode(4, "Temporal Structured Occurrence Nets"));

		typePanel.add(GUI.createLabeledComponent(typeCombo, "Types:"));

	}

	@SuppressWarnings("unchecked")
	protected void createGroupItemsPanel(){

		groupSelectionPanel = new JPanel();

		JLabel label = new JLabel("Group items:");
		label.setFont(this.getFont());

		DefaultListModel listModel = new DefaultListModel();

		for(ONGroup group : net.getGroups()){
			if(group.getLabel().isEmpty())
				listModel.addElement(new CheckListItem("Group: " + net.getNodeReference(group), group));
			else
				listModel.addElement(new CheckListItem("Group: " + net.getNodeReference(group) + " (" + group.getLabel() + ")", group));
		}

		groupList = new JList (listModel);
		groupList.setCellRenderer(new CheckListRenderer());

		groupList.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent event)
			{
				JList list = (JList) event.getSource();

				int index = list.locationToIndex(event.getPoint());
				try{
					CheckListItem item = (CheckListItem)
							list.getModel().getElementAt(index);
						item.setSelected(!item.isSelected());

						if(item instanceof CheckListItem){
							if(item.isSelected() ){
								seletedGroups.add(item.getGroup());
								item.setGroupColor(Color.GREEN);
								owner.repaint();

							}
							if(!item.isSelected() ){
								seletedGroups.remove(item.getGroup());
								item.setGroupColor(Color.BLACK);
								owner.repaint();
							}
							list.repaint(list.getCellBounds(index, index));
						}
				}catch (ArrayIndexOutOfBoundsException e){}
			}
		});

		groupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane listScroller = new JScrollPane(groupList);
		listScroller.setPreferredSize(new Dimension(250, 150));
		listScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		listScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		listScroller.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		listScroller.getHorizontalScrollBar().setPreferredSize(new Dimension(12, 0));

		groupSelectionPanel.setPreferredSize(new Dimension(350, 175));
		groupSelectionPanel.setLayout(new BorderLayout());
		groupSelectionPanel.add(label, BorderLayout.NORTH);
		groupSelectionPanel.add(listScroller, BorderLayout.CENTER);
	}

	protected void createGroupButtonsPanel(){

		JButton addAllButton = new JButton("Select All");
		addAllButton.setMaximumSize(new Dimension(250, 25));
		addAllButton.setFont(this.getFont());

		addAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				seletedGroups.clear();
				for (int i = 0; i < getList().getModel().getSize(); i++){
					((CheckListItem) getList().getModel().getElementAt(i)).setSelected(true);
					seletedGroups.add(((CheckListItem) getList().getModel().getElementAt(i)).getGroup());
					getList().repaint();

					((CheckListItem) getList().getModel().getElementAt(i)).setGroupColor(Color.GREEN);
					owner.repaint();
				}
			}
		});

		JButton removeAllButton = new JButton("Remove All");
		removeAllButton.setMaximumSize(new Dimension(250, 25));
		removeAllButton.setFont(this.getFont());


		removeAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < getList().getModel().getSize(); i++){
					((CheckListItem) getList().getModel().getElementAt(i)).setSelected(false);
					getList().repaint();

					((CheckListItem) getList().getModel().getElementAt(i)).setGroupColor(Color.BLACK);
					owner.repaint();
				}
				seletedGroups.clear();
			}
		});

		groupButtonsPanel = new JPanel();
		groupButtonsPanel.setLayout(new BoxLayout(groupButtonsPanel, BoxLayout.Y_AXIS));

		groupButtonsPanel.add(Box.createRigidArea(new Dimension(0, 50)));
		groupButtonsPanel.add(addAllButton);

		groupButtonsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		groupButtonsPanel.add(removeAllButton);
	}

	protected void createSettingPanel(){
		settingPanel = new JPanel(new BorderLayout());
		JPanel leftColumn = new JPanel();
		leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));

		settingPanel.setBorder(BorderFactory.createTitledBorder("Setting"));
		highLight = new JCheckBox("Highlight erroneous nodes");
		highLight.setFont(font);
		highLight.setSelected(true);

		outputBefore = new JCheckBox("Output causal dependencies in BSON");
		outputBefore.setFont(font);
		outputBefore.setSelected(false);

		leftColumn.add(highLight);
		leftColumn.add(outputBefore);

		settingPanel.add(leftColumn, BorderLayout.WEST);
	}

	protected void createButtonsPanel() {
		confirmButtonsPanel = new JPanel (new FlowLayout(FlowLayout.RIGHT));

		runButton = new JButton ("Run");
		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				run = 1;
				net.refreshColor();
				setVisible(false);
			}
		});

		cancelButton = new JButton ("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				run = 2;
				net.refreshColor();
				setVisible(false);
			}
		});

		confirmButtonsPanel.add(cancelButton);
		confirmButtonsPanel.add(runButton);
	}

	protected void createInterface(){
		createTypePanel();
		createGroupPanel();
		createButtonsPanel();
		createSettingPanel();

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		content.add(Box.createRigidArea(new Dimension(0, 15)));
		content.add(typePanel);
		content.add(groupPanel);
		content.add(settingPanel);
		content.add(confirmButtonsPanel);

		this.setSize(new Dimension(500, 600));
		this.add(content);
		this.setResizable(false);
		this.pack();
	}

	public StructureVerifyDialog (Window owner, String title, ModalityType modalityType, SON net){
		//super(owner, "Structure Verification Setting",  ModalityType.APPLICATION_MODAL);
		super(owner, title, modalityType);
		this.net = net;
		this.owner = owner;

		createInterface();
	}

	public StructureVerifyDialog (Window owner, SON net){
		this(owner, "Structure Verification Setting",  ModalityType.APPLICATION_MODAL, net);
	}

	protected String groupPanelTitle(){
		return "Group selection";
	}

	public SON getSONModel(){
		return this.net;
	}

	public ArrayList<ONGroup> getSelectedGroup(){
		return this.seletedGroups;
	}

	public JList getList(){
		return this.groupList;
	}

	public StructureVerifySettings getSetting(){
		return new StructureVerifySettings(this.highLight.isSelected(), this.outputBefore.isSelected(),  getSelectedGroup(), this.typeCombo.getSelectedIndex());
	}

	public int getRun(){
		return run;
	}

	public Font getPlainFont(){
		return font;
	}
}
