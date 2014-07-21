package org.workcraft.plugins.son.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.workcraft.Framework;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.AbstractTool;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.gui.graph.tools.Decorator;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.layouts.WrapLayout;
import org.workcraft.plugins.shared.CommonVisualSettings;
import org.workcraft.plugins.son.ONGroup;
import org.workcraft.plugins.son.SONModel;
import org.workcraft.plugins.son.Step;
import org.workcraft.plugins.son.Trace;
import org.workcraft.plugins.son.VisualSON;
import org.workcraft.plugins.son.algorithm.BSONAlg;
import org.workcraft.plugins.son.algorithm.ErrorTracingAlg;
import org.workcraft.plugins.son.algorithm.RelationAlgorithm;
import org.workcraft.plugins.son.algorithm.SimulationAlg;
import org.workcraft.plugins.son.elements.ChannelPlace;
import org.workcraft.plugins.son.elements.Condition;
import org.workcraft.plugins.son.elements.TransitionNode;
import org.workcraft.plugins.son.elements.VisualTransitionNode;
import org.workcraft.plugins.son.gui.ParallelSimDialog;
import org.workcraft.util.Func;
import org.workcraft.util.GUI;

public class SONSimulationTool extends AbstractTool implements ClipboardOwner{

	private SONModel net;
	protected VisualSON visualNet;
	private Framework framework;

	private RelationAlgorithm relationAlg;
	private BSONAlg bsonAlg;
	private SimulationAlg simuAlg;
	private ErrorTracingAlg	errAlg;

	private Collection<ONGroup> abstractGroups = null;
	private Collection<ArrayList<Node>> syncSet = null;
	private Map<Condition, Collection<Condition>> phases = null;
	protected Map<Node, Boolean>initialMarking = null;

	protected JPanel interfacePanel;
	protected JPanel controlPanel;
	protected JScrollPane infoPanel;
	protected JPanel statusPanel;
	protected JTable traceTable;

	private JSlider speedSlider;
	private JButton playButton, stopButton, backwardButton, forwardButton, reverseButton;
	private JButton copyStateButton, pasteStateButton, mergeTraceButton;

	final double DEFAULT_SIMULATION_DELAY = 0.3;
	final double EDGE_SPEED_MULTIPLIER = 10;

	protected final Trace mainTrace = new Trace();
	protected final Trace branchTrace = new Trace();

	protected boolean reverse = false;
	protected boolean conToBlock = true;

	protected Timer timer = null;

	@Override
	public void createInterfacePanel(final GraphEditor editor) {
		super.createInterfacePanel(editor);

		playButton = GUI.createIconButton(GUI.createIconFromSVG("images/icons/svg/simulation-play.svg"), "Automatic trace playback");
		stopButton = GUI.createIconButton(GUI.createIconFromSVG("images/icons/svg/simulation-stop.svg"), "Reset trace playback");
		backwardButton = GUI.createIconButton(GUI.createIconFromSVG("images/icons/svg/simulation-backward.svg"), "Step backward");
		forwardButton = GUI.createIconButton(GUI.createIconFromSVG("images/icons/svg/simulation-forward.svg"), "Step forward");
		reverseButton = GUI.createIconButton(GUI.createIconFromSVG("images/icons/svg/son-reverse-simulation.svg"), "Reverse simulation");

		speedSlider = new JSlider(-1000, 1000, 0);
		speedSlider.setToolTipText("Simulation playback speed");

		copyStateButton = GUI.createIconButton(GUI.createIconFromSVG("images/icons/svg/simulation-trace-copy.svg"), "Copy trace to clipboard");
		pasteStateButton = GUI.createIconButton(GUI.createIconFromSVG("images/icons/svg/simulation-trace-paste.svg"), "Paste trace from clipboard");
		mergeTraceButton = GUI.createIconButton(GUI.createIconFromSVG("images/icons/svg/simulation-trace-merge.svg"), "Merge branch into trace");

		int buttonWidth = (int)Math.round(playButton.getPreferredSize().getWidth() + 5);
		int buttonHeight = (int)Math.round(playButton.getPreferredSize().getHeight() + 5);
		Dimension panelSize = new Dimension(buttonWidth * 5, buttonHeight);

		JPanel simulationControl = new JPanel();
		simulationControl.setLayout(new FlowLayout());
		simulationControl.setPreferredSize(panelSize);
		simulationControl.setMaximumSize(panelSize);
		simulationControl.add(playButton);
		simulationControl.add(stopButton);
		simulationControl.add(backwardButton);
		simulationControl.add(forwardButton);
		simulationControl.add(reverseButton);

		JPanel speedControl = new JPanel();
		speedControl.setLayout(new BorderLayout());
		speedControl.setPreferredSize(panelSize);
		speedControl.setMaximumSize(panelSize);
		speedControl.add(speedSlider, BorderLayout.CENTER);

		JPanel traceControl = new JPanel();
		traceControl.setLayout(new FlowLayout());
		traceControl.setPreferredSize(panelSize);
		traceControl.add(new JSeparator());
		traceControl.add(copyStateButton);
		traceControl.add(pasteStateButton);
		traceControl.add(mergeTraceButton);

		controlPanel = new JPanel();
		controlPanel.setLayout(new WrapLayout());
		controlPanel.add(simulationControl);
		controlPanel.add(speedControl);
		controlPanel.add(traceControl);

		traceTable = new JTable(new TraceTableModel());
		traceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		infoPanel = new JScrollPane(traceTable);
		infoPanel.setPreferredSize(new Dimension(1, 1));

		statusPanel = new JPanel();
		interfacePanel = new JPanel();
		interfacePanel.setLayout(new BorderLayout());
		interfacePanel.add(controlPanel, BorderLayout.PAGE_START);
		interfacePanel.add(infoPanel, BorderLayout.CENTER);
		interfacePanel.add(statusPanel, BorderLayout.PAGE_END);

		speedSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(timer != null) {
					timer.stop();
					timer.setInitialDelay(getAnimationDelay());
					timer.setDelay(getAnimationDelay());
					timer.start();
				}
				updateState(editor);
			}
		});

		playButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if (timer == null) {
					timer = new Timer(getAnimationDelay(), new ActionListener()	{
						@Override
						public void actionPerformed(ActionEvent e) {
							step(editor);
						}
					});
					timer.start();
				} else  {
					timer.stop();
					timer = null;
				}
				updateState(editor);
			}
		});

		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reset(editor);
			}
		});

		backwardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stepBack(editor);
			}
		});

		forwardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				step(editor);
			}
		});

		reverseButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				 Map<Node, Boolean> currentMarking = readMarking();
					setReverse(editor, !reverse);
					if(!reverse)
						initialMarking = currentMarking;
					else
						initialMarking = currentMarking;
					branchTrace.clear();
					mainTrace.clear();
					//clear clip board contents
					StringSelection stringSelection = new StringSelection("");
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
					            stringSelection, null);
					reset(editor);
					updateState(editor);
			}
		});

		copyStateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copyState(editor);
			}
		});

		pasteStateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pasteState(editor);
			}
		});

		mergeTraceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mergeTrace(editor);
			}
		});

		traceTable.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = traceTable.getSelectedColumn();
				int row = traceTable.getSelectedRow();
				if (column == 0) {
					if (row < mainTrace.size()) {
						boolean work = true;
						while (work && (branchTrace.getPosition() > 0)) {
							work = quietStepBack();
						}
						while (work && (mainTrace.getPosition() > row)) {
							work = quietStepBack();
						}
						while (work && (mainTrace.getPosition() < row)) {
							work = quietStep();
						}
					}
				} else {
					if ((row >= mainTrace.getPosition()) && (row < mainTrace.getPosition() + branchTrace.size())) {
						boolean work = true;
						while (work && (mainTrace.getPosition() + branchTrace.getPosition() > row)) {
							work = quietStepBack();
						}
						while (work && (mainTrace.getPosition() + branchTrace.getPosition() < row)) {
							work = quietStep();
						}
					}
				}
				updateState(editor);
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
		traceTable.setDefaultRenderer(Object.class,	new TraceTableCellRendererImplementation());
	}

	@Override
	public void activated(final GraphEditor editor) {
		super.activated(editor);

		visualNet = (VisualSON)editor.getModel();
		framework = editor.getFramework();
		net = (SONModel)visualNet.getMathModel();
		relationAlg = new RelationAlgorithm(net);
		bsonAlg = new BSONAlg(net);
		simuAlg = new SimulationAlg(net);
		errAlg = new ErrorTracingAlg(net);

		if (visualNet == editor.getModel()) {
			editor.getWorkspaceEntry().captureMemento();
		}
		editor.getWorkspaceEntry().setCanModify(false);

		initialMarking = autoInitalMarking();
		mainTrace.clear();
		branchTrace.clear();

		if(!visualNet.connectToBlocks()){
			conToBlock = false;
			return;
		}

		syncSet = getSyncCycles();
		abstractGroups =bsonAlg.getAbstractGroups(net.getGroups());
		phases = new HashMap<Condition, Collection<Condition>>();
		for(ONGroup group : abstractGroups){
			for(Condition c : group.getConditions())
				phases.put(c, bsonAlg.getPhase(c));
		}
		if (ErrTracingDisable.showErrorTracing()) {
			net.resetConditionErrStates();
		}

		updateState(editor);
	}

	@Override
	public void deactivated(final GraphEditor editor) {
		//super.deactivated(editor);
		visualNet.connectToBlocksInside();
		if (timer != null) {
			timer.stop();
			timer = null;
		}
		if (visualNet == editor.getModel()) {
			editor.getWorkspaceEntry().cancelMemento();
		}
		this.visualNet = null;
		this.net = null;

	}

	public void updateState(final GraphEditor editor) {
		if (timer == null) {
			playButton.setIcon(GUI.createIconFromSVG("images/icons/svg/simulation-play.svg"));
		} else {
			if (branchTrace.canProgress() || (branchTrace.isEmpty() && mainTrace.canProgress())) {
				playButton.setIcon(GUI.createIconFromSVG("images/icons/svg/simulation-pause.svg"));
				timer.setDelay(getAnimationDelay());
			} else {
				playButton.setIcon(GUI.createIconFromSVG("images/icons/svg/simulation-play.svg"));
				timer.stop();
				timer = null;
			}
		}
		playButton.setEnabled(branchTrace.canProgress() || (branchTrace.isEmpty() && mainTrace.canProgress()));
		stopButton.setEnabled(!mainTrace.isEmpty() || !branchTrace.isEmpty());
		backwardButton.setEnabled((mainTrace.getPosition() > 0) || (branchTrace.getPosition() > 0));
		forwardButton.setEnabled(branchTrace.canProgress() || (branchTrace.isEmpty() && mainTrace.canProgress()));
		traceTable.tableChanged(new TableModelEvent(traceTable.getModel()));
		if(!reverse){
			reverseButton.setIcon(GUI.createIconFromSVG("images/icons/svg/son-reverse-simulation.svg"));
			reverseButton.setToolTipText("Reverse simulation");
		}

		else{
			reverseButton.setIcon(GUI.createIconFromSVG("images/icons/svg/son-forward-simulation.svg"));
			reverseButton.setToolTipText("Forward simulation");
		}
		editor.requestFocus();
		editor.repaint();
	}

	private int getAnimationDelay() {
		return (int)(1000.0 * DEFAULT_SIMULATION_DELAY * Math.pow(EDGE_SPEED_MULTIPLIER, -speedSlider.getValue() / 1000.0));
	}

	@SuppressWarnings("serial")
	private class TraceTableModel extends AbstractTableModel {
		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int column) {
			if (column == 0) return "Trace";
			return "Branch";
		}

		@Override
		public int getRowCount() {
			return Math.max(mainTrace.size(), mainTrace.getPosition() + branchTrace.size());
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (column == 0) {
				if (!mainTrace.isEmpty() && (row < mainTrace.size())) {
					return mainTrace.get(row);
				}
			} else {
				if (!branchTrace.isEmpty() && (row >= mainTrace.getPosition()) && (row < mainTrace.getPosition() + branchTrace.size())) {
					return branchTrace.get(row - mainTrace.getPosition());
				}
			}
			return "";
		}
	};

	//set initial marking
	protected Map<Node, Boolean> autoInitalMarking(){
		HashMap<Node, Boolean> result = new HashMap<Node, Boolean>();

		for (Condition c : net.getConditions()) {
			c.setMarked(false);
			result.put(c, false);
		}
		for(ChannelPlace cp : net.getChannelPlace()){
			cp.setMarked(false);
			result.put(cp, false);
		}
		//initial marking for abstract groups and behavioral groups
		for(ONGroup abstractGroup : bsonAlg.getAbstractGroups(net.getGroups())){
			for(Node c : relationAlg.getInitial(abstractGroup.getComponents())){
				if(c instanceof Condition){
					result.put(c, true);
					((Condition) c).setMarked(true);
					Collection<ONGroup> bhvGroup = bsonAlg.getBhvGroups((Condition) c);
					if(bhvGroup.size() != 1)
						JOptionPane.showMessageDialog(null, "Incorrect BSON structure (disjoint phase/empty phase), run structure verification.", "error", JOptionPane.WARNING_MESSAGE);
					else
						for(ONGroup group : bhvGroup){
							//can optimize
							Collection<Node> initial = relationAlg.getInitial(group.getComponents());
							if(bsonAlg.getPhase((Condition)c).containsAll(initial))
								for(Node c1 : relationAlg.getInitial(group.getComponents())){
									result.put(c1, true);
									((Condition) c1).setMarked(true);}
							else
								JOptionPane.showMessageDialog(null, "Incorrect BSON structure (minimal phase), run structure verification.", "error", JOptionPane.WARNING_MESSAGE);
						}
				}
			}
		}
		//initial marking for channel places
		for(Node c : relationAlg.getInitial(net.getComponents())){
			if(c instanceof ChannelPlace){
				result.put(c, true);
				((ChannelPlace)c).setMarked(true);}
		}

		//initial marking for other groups.
		for(ONGroup group : net.getGroups()){
			boolean hasBhvLine = false;
			for(Condition c : group.getConditions())
				if(net.getSONConnectionTypes(c).contains("BHVLINE"))
					hasBhvLine = true;
			if(!hasBhvLine){
				for(Node c : relationAlg.getInitial(group.getComponents())){
					if(c instanceof Condition){
						result.put(c, true);
						((Condition)c).setMarked(true);}
				}
			}
		}
		return result;
	}

	protected Map<Node, Boolean> readMarking() {
		HashMap<Node, Boolean> result = new HashMap<Node, Boolean>();
		for (Condition c : net.getConditions()) {
			result.put(c, c.isMarked());
		}
		for(ChannelPlace cp : net.getChannelPlace()){
			result.put(cp, cp.isMarked());
		}
		return result;
	}

	private Collection<ArrayList<Node>> getSyncCycles(){

		HashSet<Node> nodes = new HashSet<Node>();
		nodes.addAll(net.getConditions());
		nodes.addAll(net.getEventNodes());

		return simuAlg.getSyncCycles(nodes);
	}

	private boolean quietStep() {
		boolean result = false;
		List<TransitionNode> runList = null;
		int mainInc = 0;
		int branchInc = 0;
		if (branchTrace.canProgress()) {
			Step step = branchTrace.getCurrent();
			runList=this.getRunList(step);
			branchInc = 1;
		} else if (mainTrace.canProgress()) {
			Step step = mainTrace.getCurrent();
			runList=this.getRunList(step);
			mainInc = 1;
		}


		if (runList != null && !reverse) {
			simuAlg.fire(runList);
			setErrNum(runList, reverse);
			mainTrace.incPosition(mainInc);
			branchTrace.incPosition(branchInc);
			result = true;
		}
		if (runList!= null && reverse) {
			simuAlg.unFire(runList);
			setErrNum(runList, reverse);
			mainTrace.incPosition(mainInc);
			branchTrace.incPosition(branchInc);
			result = true;
		}
		return result;
	}

	private boolean step(final GraphEditor editor) {
		boolean ret = quietStep();
		updateState(editor);
		return ret;
	}

	private boolean stepBack(final GraphEditor editor) {
		boolean ret = quietStepBack();
		updateState(editor);
		return ret;
	}

	private boolean quietStepBack() {
		boolean result = false;
		List<TransitionNode> runList = null;
		int mainDec = 0;
		int branchDec = 0;
		if (branchTrace.getPosition() > 0) {
			Step step = branchTrace.get(branchTrace.getPosition()-1);
			runList=this.getRunList(step);
			branchDec = 1;
		} else if (mainTrace.getPosition() > 0) {
			Step step = mainTrace.get(mainTrace.getPosition() - 1);
			runList=this.getRunList(step);
			mainDec = 1;
		}

		if (runList != null && !reverse) {
			simuAlg.unFire(runList);
			mainTrace.decPosition(mainDec);
			branchTrace.decPosition(branchDec);
			if ((branchTrace.getPosition() == 0) && !mainTrace.isEmpty()) {
				branchTrace.clear();
			}
			result = true;
			this.setErrNum(runList, !reverse);
		}
		if (runList != null && reverse) {
			simuAlg.fire(runList);
			mainTrace.decPosition(mainDec);
			branchTrace.decPosition(branchDec);
			if ((branchTrace.getPosition() == 0) && !mainTrace.isEmpty()) {
				branchTrace.clear();
			}
			result = true;
			this.setErrNum(runList, !reverse);
		}
		return result;
	}


	private void reset(final GraphEditor editor) {
		applyMarking(initialMarking);
		mainTrace.clear();
		branchTrace.clear();
		if (timer != null) 	{
			timer.stop();
			timer = null;
		}
		updateState(editor);
	}

	private void copyState(final GraphEditor editor) {
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection stringSelection = new StringSelection(
				mainTrace.toString() + "\n" + branchTrace.toString() + "\n");
		clip.setContents(stringSelection, this);
		updateState(editor);
	}

	private void pasteState(final GraphEditor editor) {
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clip.getContents(null);
		boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		String str="";
		if (hasTransferableText) {
			try {
				str = (String)contents.getTransferData(DataFlavor.stringFlavor);
			}
			catch (UnsupportedFlavorException ex){
				System.out.println(ex);
				ex.printStackTrace();
			}
			catch (IOException ex) {
				System.out.println(ex);
				ex.printStackTrace();
			}
		}

		applyMarking(initialMarking);
		mainTrace.clear();
		branchTrace.clear();
		boolean first = true;
		for (String s: str.split("\n")) {
			if (first) {
				mainTrace.fromString(s);
				int mainTracePosition = mainTrace.getPosition();
				mainTrace.setPosition(0);
				boolean work = true;
				while (work && (mainTrace.getPosition() < mainTracePosition)) {
					work = quietStep();
				}
			} else {
				branchTrace.fromString(s);
				int branchTracePosition = branchTrace.getPosition();
				branchTrace.setPosition(0);
				boolean work = true;
				while (work && (branchTrace.getPosition() < branchTracePosition)) {
					work = quietStep();
				}
				break;
			}
			first = false;
		}
		updateState(editor);
	}

	private void mergeTrace(final GraphEditor editor) {
		if (!branchTrace.isEmpty()) {
			while (mainTrace.getPosition() < mainTrace.size()) {
				mainTrace.removeCurrent();
			}
			mainTrace.addAll(branchTrace);
			mainTrace.incPosition(branchTrace.getPosition());
			branchTrace.clear();
		}
		updateState(editor);
	}

	private void setErrNum(List<TransitionNode> runList, boolean reverse){
		if (ErrTracingDisable.showErrorTracing()){
			Collection<TransitionNode> abstractEvents = new ArrayList<TransitionNode>();
			//get high level events
			for(TransitionNode absEvent : runList){
				for(ONGroup group : abstractGroups){
					if(group.getEventNodes().contains(absEvent))
						abstractEvents.add(absEvent);
				}
			}
			//get low level events
			runList.removeAll(abstractEvents);
			if(!reverse){
				errAlg.setErrNum(abstractEvents, syncSet, false);
				errAlg.setErrNum(runList, syncSet, true);
			}
			else{
				errAlg.setReverseErrNum(abstractEvents, syncSet, false);
				errAlg.setReverseErrNum(runList, syncSet, true);
			}
		}
	}

	private void applyMarking(Map<Node, Boolean> marking){
		for (Node c: marking.keySet()) {
			if(c instanceof Condition)
				if (net.getConditions().contains(c)) {
					((Condition)c).setMarked(marking.get((Condition)c));
				} else {
					//ExceptionDialog.show(null, new RuntimeException("Place "+p.toString()+" is not in the model"));
				}
			if(c instanceof ChannelPlace)
				if (net.getChannelPlace().contains(c)){
					((ChannelPlace)c).setMarked(marking.get((ChannelPlace)c));
				}
		}
	}


	public void executeEvent(final GraphEditor editor, List<TransitionNode> runList) {
		if (runList.isEmpty()) return;
		List<TransitionNode> traceList = new ArrayList<TransitionNode>();
		// if clicked on the trace event, do the step forward
		if (branchTrace.isEmpty() && !mainTrace.isEmpty() && (mainTrace.getPosition() < mainTrace.size())) {
			Step step = mainTrace.get(mainTrace.getPosition());
			traceList=getRunList(step);
		}
		// otherwise form/use the branch trace
		if (!branchTrace.isEmpty() && (branchTrace.getPosition() < branchTrace.size())) {
			Step step = branchTrace.get(branchTrace.getPosition());
			traceList=getRunList(step);
		}
		if (!traceList.isEmpty() && traceList.containsAll(runList) && runList.containsAll(traceList)){
				step(editor);
				return;
		}
		while (branchTrace.getPosition() < branchTrace.size()) {
			branchTrace.removeCurrent();
		}

		Step newStep = new Step();
		for(TransitionNode e : runList)
			newStep.add(net.getNodeReference(e));

		branchTrace.add(newStep);
		step(editor);
		return;
	}

	private ArrayList<TransitionNode> getRunList(Step step){
		ArrayList<TransitionNode> result = new ArrayList<TransitionNode>();
		for(int i =0; i<step.size(); i++){
			final Node node = net.getNodeByReference(step.get(i));
			if(node instanceof TransitionNode)
				result.add((TransitionNode)node);
		}
		return result;
	}

	@SuppressWarnings("serial")
	private final class TraceTableCellRendererImplementation implements TableCellRenderer {
		JLabel label = new JLabel() {
			@Override
			public void paint( Graphics g ) {
				g.setColor( getBackground() );
				g.fillRect( 0, 0, getWidth() - 1, getHeight() - 1 );
				super.paint( g );
			}
		};

		boolean isActive(int row, int column) {
			if (column==0) {
				if (!mainTrace.isEmpty() && branchTrace.isEmpty()) {
					return row == mainTrace.getPosition();
				}
			} else {
				if (!branchTrace.isEmpty() && (row >= mainTrace.getPosition()) && (row < mainTrace.getPosition() + branchTrace.size())) {
					return (row == mainTrace.getPosition() + branchTrace.getPosition());
				}
			}
			return false;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus,	int row, int column) {

			if (!(value instanceof Step)) return null;

			label.setText(((Step)value).toString());


			if (isActive(row, column)) {
				label.setBackground(Color.YELLOW);
			} else {
				label.setBackground(Color.WHITE);
			}

			return label;
		}
	}

	@Override
	public void mousePressed(GraphEditorMouseEvent e) {
		Node node = HitMan.hitDeepest(e.getPosition(), e.getModel().getRoot(),
			new Func<Node, Boolean>() {
			@Override
			public Boolean eval(Node node) {

				if(node instanceof VisualTransitionNode && simuAlg.isEnabled(((VisualTransitionNode)node).getMathEventNode(), syncSet, phases) && !reverse){
					return true;
				}
				if(node instanceof VisualTransitionNode && simuAlg.isUnfireEnabled(((VisualTransitionNode)node).getMathEventNode(), syncSet, phases) && reverse){
					return true;
				}
				return false;

			}
		});

		if (node instanceof VisualTransitionNode && conToBlock){

			Collection<TransitionNode> enabledEvents = new ArrayList<TransitionNode>();
			TransitionNode event = ((VisualTransitionNode)node).getMathEventNode();

			if(reverse){
				for(TransitionNode enable : net.getEventNodes())
					if(simuAlg.isUnfireEnabled(enable, syncSet, phases))
						enabledEvents.add(enable);
				}else{
				for(TransitionNode enable : net.getEventNodes())
					if(simuAlg.isEnabled(enable, syncSet, phases))
						enabledEvents.add(enable);
				}

			List<TransitionNode> minimalEvents = simuAlg.getMinimalExeResult(event, syncSet, enabledEvents);
			List<TransitionNode> minimalReverseEvents = simuAlg.getMinimalReverseExeResult(event, syncSet, enabledEvents);

			if(!reverse){
				List<TransitionNode> possibleEvents = new ArrayList<TransitionNode>();
				for(TransitionNode psbE : enabledEvents)
					if(!minimalEvents.contains(psbE))
						possibleEvents.add(psbE);

				minimalEvents.remove(event);

				List<TransitionNode> runList = new ArrayList<TransitionNode>();

				if(possibleEvents.isEmpty() && minimalEvents.isEmpty()){
					runList.add(event);
					executeEvent(e.getEditor(),runList);

				}else{
					e.getEditor().requestFocus();
					ParallelSimDialog dialog = new ParallelSimDialog(this.getFramework().getMainWindow(), net, possibleEvents, minimalEvents, event, syncSet, enabledEvents, reverse);
					GUI.centerToParent(dialog, this.getFramework().getMainWindow());
					dialog.setVisible(true);

					runList.addAll(minimalEvents);
					runList.add(event);

					if (dialog.getRun() == 1){
						runList.addAll(dialog.getSelectedEvent());
						executeEvent(e.getEditor(),runList);
					}
					if(dialog.getRun()==2){
						simuAlg.clearAll();
						return;
						}
					}
				//Error tracing
			//	setErrNum(runList, reverse);
				simuAlg.clearAll();

			}else{
				//reverse simulation

				List<TransitionNode> possibleEvents = new ArrayList<TransitionNode>();
				for(TransitionNode psbE : enabledEvents)
					if(!minimalReverseEvents.contains(psbE))
						possibleEvents.add(psbE);

						minimalReverseEvents.remove(event);

				List<TransitionNode> runList = new ArrayList<TransitionNode>();

				if(possibleEvents.isEmpty() && minimalReverseEvents.isEmpty()){
					runList.add(event);
					executeEvent(e.getEditor(),runList);
					simuAlg.clearAll();
				} else {
					e.getEditor().requestFocus();
					ParallelSimDialog dialog = new ParallelSimDialog(this.getFramework().getMainWindow(), net, possibleEvents, minimalReverseEvents, event, syncSet, enabledEvents, reverse);

					GUI.centerToParent(dialog, this.getFramework().getMainWindow());
					dialog.setVisible(true);

					runList.addAll(minimalReverseEvents);
					runList.add(event);

					if (dialog.getRun() == 1){
						runList.addAll(dialog.getSelectedEvent());
						executeEvent(e.getEditor(),runList);
					}
					if(dialog.getRun()==2){
						simuAlg.clearAll();
						return;
					}
				}
				//Reverse error tracing
				//setErrNum(runList, reverse);
				simuAlg.clearAll();
			}
		}
	}

	public Framework getFramework(){
		return this.framework;
	}

	public void setFramework(Framework framework){
		this.framework =framework;
	}

	public boolean isReverse(){
		return reverse;
	}

	public void setReverse(final GraphEditor editor, boolean reverse){
		this.reverse = reverse;
		updateState(editor);
	}

	@Override
	public Icon getIcon() {
		return GUI.createIconFromSVG("images/icons/svg/tool-simulation.svg");
	}

	@Override
	public JPanel getInterfacePanel() {
		return interfacePanel;
	}

	public void setTrace(Trace t) {
		mainTrace.clear();
		mainTrace.addAll(t);
		branchTrace.clear();
	}

	public String getLabel() {
		return "Simulation";
	}

	public int getHotKeyCode() {
		return KeyEvent.VK_M;
	}

	@Override
	public void drawInScreenSpace(GraphEditor editor, Graphics2D g) {
		GUI.drawEditorMessage(editor, g, Color.BLACK, "Simulation: click on the highlighted transitions to fire them");
	}

	@Override
	public Decorator getDecorator(final GraphEditor editor) {
		return new Decorator() {
			@Override
			public Decoration getDecoration(Node node) {
				if(node instanceof VisualTransitionNode && conToBlock) {
					TransitionNode event = ((VisualTransitionNode)node).getMathEventNode();
					Node event2 = null;
					if (branchTrace.canProgress()) {
						Step step = branchTrace.get(branchTrace.getPosition());
						if (step.contains(net.getName(event)))
							event2 = net.getNodeByReference(net.getName(event));
					} else if (branchTrace.isEmpty() && mainTrace.canProgress()) {
						Step step = mainTrace.get(mainTrace.getPosition());
						if (step.contains(net.getName(event)))
							event2 = net.getNodeByReference(net.getName(event));
					}


					if (event==event2) {
						return new Decoration(){
							@Override
							public Color getColorisation() {
								return CommonVisualSettings.getEnabledBackgroundColor();
							}

							@Override
							public Color getBackground() {
								return CommonVisualSettings.getEnabledForegroundColor();
							}
						};

					}
					if (simuAlg.isEnabled(event, syncSet, phases)&& !reverse)
						return new Decoration(){
							@Override
							public Color getColorisation() {
								return CommonVisualSettings.getEnabledForegroundColor();
							}

							@Override
							public Color getBackground() {
								return CommonVisualSettings.getEnabledBackgroundColor();
							}
						};
					if (simuAlg.isUnfireEnabled(event, syncSet, phases)&& reverse)
							return new Decoration(){
								@Override
								public Color getColorisation() {
									return CommonVisualSettings.getEnabledForegroundColor();
								}

								@Override
								public Color getBackground() {
									return CommonVisualSettings.getEnabledBackgroundColor();
								}
							};
				}
				return null;
			}
		};
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// TODO Auto-generated method stub
	}
}
