package org.workcraft.plugins.stg.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.workcraft.dom.Node;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.plugins.petri.PetriNetSettings;
import org.workcraft.plugins.petri.tools.PetriNetSimulationTool;
import org.workcraft.plugins.stg.SignalTransition;


public class STGSimulationTool extends PetriNetSimulationTool {
	private static Color inputsColor = Color.RED.darker();
	private static Color outputsColor = Color.BLUE.darker();
	private static Color internalsColor = Color.GREEN.darker();
	private static Color dummyColor = Color.BLACK.darker();

	public class SignalState {
		public String name = "";
		public Color color = Color.BLACK;
		public boolean excited = false;
		public int value = -1;
	}

	protected Map<String, SignalState> stateMap;
	protected JTable stateTable;

	public STGSimulationTool() {
		super();
		createInterface();
	}

	@SuppressWarnings("serial")
	private class StateTableModel extends AbstractTableModel {
		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int column) {
			if (column==0) return "Signal";
			return "State";
		}

		@Override
		public int getRowCount() {
			return stateMap.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			Object[] names = stateMap.keySet().toArray();
			return stateMap.get(names[row]);
		}
	}


	private void createInterface() {
		stateMap = new HashMap<String, SignalState>();
		stateTable = new JTable(new StateTableModel());
		statusPanel.setLayout(new BorderLayout());
		statusPanel.add(stateTable.getTableHeader(), BorderLayout.PAGE_START);
		statusPanel.add(stateTable, BorderLayout.CENTER);
		stateTable.setFillsViewportHeight(true);
		stateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		stateTable.setDefaultRenderer(Object.class,
				new TableCellRenderer() {

			JLabel label = new JLabel() {
				@Override
				public void paint( Graphics g ) {
					g.setColor( getBackground() );
					g.fillRect( 0, 0, getWidth() - 1, getHeight() - 1 );
					super.paint( g );
				}
			};

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				if (!(value instanceof SignalState)) return null;
				SignalState st = (SignalState)value;
				if (column == 0) {
					label.setText(st.name);
					label.setForeground(st.color);
					label.setFont(label.getFont().deriveFont(Font.PLAIN));
				} else {
					if (st.value < 0) {
						label.setText("?");
					} else {
						label.setText(Integer.toString(st.value));
					}
					label.setForeground(Color.BLACK);
					if (st.excited) {
						label.setFont(label.getFont().deriveFont(Font.BOLD));
					}
				}
				return label;
			}
	});

		traceTable.setDefaultRenderer(Object.class,
			new TableCellRenderer() {
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
						if (trace!=null&&branchTrace==null)
							return row==traceStep;
					} else {
						if (branchTrace!=null&&row>=traceStep&&row<traceStep+branchTrace.size()) {
							return (row-traceStep)==branchStep;
						}
					}
					return false;
				}

				@Override
				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {

					if (!(value instanceof String)) return null;

					label.setText((String)value);
					label.setForeground(Color.BLACK);

					Color fore = PetriNetSettings.getEnabledForegroundColor();
					Color back = PetriNetSettings.getEnabledBackgroundColor();

					Node node = net.getNodeByReference((String)value);
					if (node instanceof SignalTransition) {
						SignalTransition st = (SignalTransition)node;
						switch (st.getSignalType()) {
							case INPUT:    label.setForeground(inputsColor); break;
							case OUTPUT:   label.setForeground(outputsColor); break;
							case INTERNAL: label.setForeground(internalsColor); break;
							case DUMMY: 	label.setForeground(dummyColor); break;
						}
					}

					if (isActive(row, column)) {
						if (fore!=null&&back!=null) {
							label.setBackground(fore);
							label.setForeground(back);
						} else {
							label.setBackground(Color.YELLOW);
						}
					} else {
						label.setBackground(Color.WHITE);
					}
					return label;
				}
		});
	}

	@Override
	protected void update() {
		super.update();
		ArrayList<String> combinedTrace = new ArrayList<String>();
		if (trace != null) {
			combinedTrace.addAll(trace.subList(0, traceStep));
		}
		if (branchTrace != null) {
			combinedTrace.addAll(branchTrace.subList(0, branchStep));
		}

		for (String signalName: stateMap.keySet()) {
			SignalState signalState = stateMap.get(signalName);
			signalState.value = -1;
			signalState.excited = false;
		}

		for (String ref: combinedTrace) {
			Node node = net.getNodeByReference(ref);
			if (node instanceof SignalTransition) {
				SignalTransition transition = (SignalTransition)node;
				SignalState st = stateMap.get(transition.getSignalName());
				switch (transition.getDirection()) {
					case MINUS: 	st.value = 0; break;
					case PLUS:		st.value = 1;break;
					case TOGGLE:
						if (st.value == 1) {
							st.value = 0;
						} else if (st.value == 0) {
							st.value = 1;
						}
						break;
					default:
						break;
				}
			}
		}

		for(Node node: net.getTransitions()) {
			if (node instanceof SignalTransition) {
				SignalTransition transition = (SignalTransition)node;
				SignalState st = stateMap.get(transition.getSignalName());
				if (st != null) {
					st.excited |= net.isEnabled(transition);
				}
			}
		}

		stateTable.tableChanged(new TableModelEvent(traceTable.getModel()));
	}

	@Override
	public void activated(GraphEditor editor) {
		super.activated(editor);
		for(Node node: net.getTransitions()) {
			if (node instanceof SignalTransition) {
				SignalTransition transition = (SignalTransition)node;
				if (!stateMap.containsKey(transition.getSignalName())) {
					SignalState signalState = new SignalState();
					signalState.name = transition.getSignalName();
					switch (transition.getSignalType()) {
						case INPUT:    signalState.color = inputsColor; break;
						case OUTPUT:   signalState.color = outputsColor; break;
						case INTERNAL: signalState.color = internalsColor; break;
						case DUMMY:    signalState.color = dummyColor; break;
					}
					stateMap.put(signalState.name, signalState);
				}
			}
		}
		update();
	}

}
