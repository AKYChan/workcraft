package org.workcraft.plugins.son.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import org.workcraft.dom.Node;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.plugins.son.SON;
import org.workcraft.plugins.son.TimeEstimatorSettings;
import org.workcraft.plugins.son.algorithm.EstimationAlg;
import org.workcraft.plugins.son.exception.AlternativeStructureException;
import org.workcraft.plugins.son.exception.TimeEstimationException;
import org.workcraft.plugins.son.exception.TimeInconsistencyException;
import org.workcraft.plugins.son.exception.TimeOutOfBoundsException;
import org.workcraft.plugins.son.gui.TimeConsistencyDialog.Granularity;
import org.workcraft.plugins.son.util.Interval;
import org.workcraft.plugins.son.util.ScenarioRef;
import org.workcraft.plugins.son.util.ScenarioSaveList;

public class TimeEstimatorDialog extends JDialog{

	private static final long serialVersionUID = 1L;
	protected SON net;
	protected GraphEditor editor;
	protected TimeEstimatorSettings settings;
	protected Node selection;
	protected Granularity granularity;

	protected DefaultDurationPanel defaultDurationPanel;
	protected JScrollPane tabelPanel;
	protected ScenarioTable scenarioTable;

	protected JPanel buttonsPanel, durationPanel;
	protected JButton runButton, cancelButton;
	protected JCheckBox setDuration, intermediate;
	protected Dimension buttonSize = new Dimension(80, 25);
	protected int run = 0;

	public TimeEstimatorDialog (GraphEditor editor, TimeEstimatorSettings settings, Node selection, Granularity g){
		super(editor.getMainWindow(), "Estimator Setting", ModalityType.TOOLKIT_MODAL);
		net = (SON)editor.getModel().getMathModel();
		this.editor = editor;
		this.settings = settings;
		this.selection = selection;
		this.granularity = g;

		createDurationPanel();
		createScenarioTable();
		createButtonsPanel();

		setLayout(new BorderLayout(10, 10));
		add(durationPanel, BorderLayout.NORTH);
		add(tabelPanel, BorderLayout.CENTER);
		add(buttonsPanel, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter()
		{
		  public void windowClosing(WindowEvent e)
		  {
			 setParameters();
		  }
		});

		pack();
	}

	protected void createDurationPanel(){
		setDuration = new JCheckBox("Set default duration for all unspecifed nodes");
		setDuration.setSelected(false);
		setDuration.setLayout(new FlowLayout(FlowLayout.LEFT));

		intermediate = new  JCheckBox("Set values for intermediate nodes");
		intermediate.setSelected(false);
		intermediate.setLayout(new FlowLayout(FlowLayout.LEFT));

		defaultDurationPanel = new DefaultDurationPanel(settings.getDuration());
		defaultDurationPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		durationPanel = new JPanel();
		//durationPanel.setBorder(BorderFactory.createTitledBorder("Default Duration Setting"));
		durationPanel.setLayout(new GridLayout(3,0));
		durationPanel.add(defaultDurationPanel);
		durationPanel.add(setDuration);
		durationPanel.add(intermediate);
	}

	protected void createScenarioTable(){

		ScenarioSaveList saveList = saveListFilter(net.importScenarios(editor.getMainWindow()), selection);
		scenarioTable = new ScenarioTable(saveList, editor, new ScenarioListTableModel(), selection);

		tabelPanel = new JScrollPane(scenarioTable);
		tabelPanel.setPreferredSize(new Dimension(1, 100));

		scenarioTable.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = scenarioTable.getSelectedColumn();
				int row = scenarioTable.getSelectedRow();

				ScenarioSaveList saveList = scenarioTable.getSaveList();;

				if (column == 0 && row < saveList.size()) {
					saveList.setPosition(row);
					Object obj = scenarioTable.getValueAt(row, column);
					if(obj instanceof ScenarioRef){
						scenarioTable.setScenarioRef((ScenarioRef)obj);
						scenarioTable.setIsCellColor(true);
						scenarioTable.updateTable(editor);
						scenarioTable.updateColor(selection);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}
		});
	}

	private ScenarioSaveList saveListFilter(ScenarioSaveList saveList, Node selection){
		ScenarioSaveList result = new ScenarioSaveList();
		for(ScenarioRef ref : saveList){
			for(String str : ref){
				if(str.equals(net.getNodeReference(selection))){
					result.add(ref);
					break;
				}
			}
		}
		return result;
	}

	protected void createButtonsPanel() {
		buttonsPanel = new JPanel (new FlowLayout(FlowLayout.RIGHT));

		runButton = new JButton ("Run");
		runButton.setPreferredSize(buttonSize);
		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setParameters();
				if(defaultDurationPanel.isValidDuration()){
					run = 1;
					EstimationAlg alg = new EstimationAlg(net, getDefaultDuration(), granularity, getScenarioRef());
					setVisible(false);

					if(setDuration.isSelected())
						alg.setDefaultDuration();

					try {
						alg.twoDirEstimation(selection, intermediate.isSelected());
					} catch (AlternativeStructureException e1) {
						errMsg(e1.getMessage());
					} catch (TimeEstimationException e1) {
						errMsg(e1.getMessage());
					} catch (TimeOutOfBoundsException e1) {
						errMsg(e1.getMessage());
					} catch (TimeInconsistencyException e1) {
						errMsg(e1.getMessage());
					}
				}else{
					defaultDurationPanel.getMin().setForeground(Color.RED);
					defaultDurationPanel.getMax().setForeground(Color.RED);
				}
			}
		});

		cancelButton = new JButton ("Cancel");
		cancelButton.setPreferredSize(buttonSize);
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				run = 2;
				setParameters();
				setVisible(false);
			}
		});

		buttonsPanel.add(cancelButton);
		buttonsPanel.add(runButton);
	}

	@SuppressWarnings("serial")
	protected class ScenarioListTableModel extends AbstractTableModel {
		@Override
		public int getColumnCount() {
			return 1;
		}

		@Override
		public String getColumnName(int column) {
			return "Scenario List";
		}

		@Override
		public int getRowCount() {
			return scenarioTable.getSaveList().size();
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (column == 0) {
				if (!scenarioTable.getSaveList().isEmpty() && (row < scenarioTable.getSaveList().size())) {
					return scenarioTable.getSaveList().get(row);
				}
			}
			return "";
		}
	};

	protected void errMsg(String msg){
		JOptionPane.showMessageDialog(editor.getMainWindow(), msg, "Time estimation error", JOptionPane.ERROR_MESSAGE);
	}

	protected void setParameters(){
		settings.setPosition(scenarioTable.getSaveList().getPosition());
		settings.setDuration(defaultDurationPanel.getDefaultDuration());
	}

	public Interval getDefaultDuration(){
		return settings.getDuration();
	}

	public ScenarioTable getScenarioTable() {
		return scenarioTable;
	}

	public ScenarioRef getScenarioRef(){
		if(scenarioTable.getScenarioRef().isEmpty())
			return null;
		else
			return scenarioTable.getScenarioRef();
	}

	public int getRun() {
		return run;
	}
}
