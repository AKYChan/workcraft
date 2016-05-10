package org.workcraft.plugins.stg.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.workcraft.Framework;
import org.workcraft.dom.Connection;
import org.workcraft.dom.Node;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.visual.FontHelper;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.gui.Coloriser;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.propertyeditor.PropertyEditorTable;
import org.workcraft.gui.workspace.Path;
import org.workcraft.plugins.dtd.Dtd;
import org.workcraft.plugins.dtd.DtdDescriptor;
import org.workcraft.plugins.dtd.Transition.Direction;
import org.workcraft.plugins.dtd.VisualDtd;
import org.workcraft.plugins.dtd.VisualDtd.SignalEvent;
import org.workcraft.plugins.dtd.VisualSignal;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualTransition;
import org.workcraft.plugins.petri.tools.PetriNetSimulationTool;
import org.workcraft.plugins.stg.LabelParser;
import org.workcraft.plugins.stg.STG;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.VisualImplicitPlaceArc;
import org.workcraft.plugins.stg.VisualSTG;
import org.workcraft.util.ColorGenerator;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;

public class StgSimulationTool extends PetriNetSimulationTool {
    private static final Color COLOR_INPUT = Color.RED.darker();
    private static final Color COLOR_OUTPUT = Color.BLUE.darker();
    private static final Color COLOR_INTERNAL = Color.GREEN.darker();

    protected Map<String, SignalState> stateMap;
    protected JTable stateTable;

    public final class SignalState {
        public String name = "";
        public Color color = Color.BLACK;
        public boolean excited = false;
        public int value = -1;
    }

    @SuppressWarnings("serial")
    private final class StateTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return (column == 0) ? "Signal" : "State";
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

    private final class StateTableCellRendererImplementation implements    TableCellRenderer {
        final JLabel label = new JLabel() {
            @Override
            public void paint(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
                super.paint(g);
            }
        };

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel result = null;
            label.setBorder(PropertyEditorTable.BORDER_RENDER);
            label.setBackground(table.getBackground());
            if ((net != null) && (value instanceof SignalState)) {
                SignalState st = (SignalState) value;
                if (column == 0) {
                    label.setText(st.name);
                    label.setForeground(st.color);
                    Font plainFont = table.getFont().deriveFont(Font.PLAIN);
                    label.setFont(plainFont);
                } else {
                    if (st.value < 0) {
                        label.setText("?");
                    } else {
                        label.setText(Integer.toString(st.value));
                    }
                    label.setForeground(table.getForeground());
                    if (st.excited) {
                        Font boldFont = table.getFont().deriveFont(Font.BOLD);
                        label.setFont(boldFont);
                    }
                }
                result = label;
            }
            return result;
        }
    }

    private final class TraceTableCellRendererImplementation implements    TableCellRenderer {
        private final JLabel label = new JLabel() {
            @Override
            public void paint(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
                super.paint(g);
            }
        };

        boolean isActive(int row, int column) {
            if (column == 0) {
                if (!mainTrace.isEmpty() && branchTrace.isEmpty()) {
                    return row == mainTrace.getPosition();
                }
            } else {
                int absoluteBranchSize = mainTrace.getPosition() + branchTrace.size();
                int absoluteBranchPosition = mainTrace.getPosition() + branchTrace.getPosition();
                if (!branchTrace.isEmpty() && (row >= mainTrace.getPosition()) && (row < absoluteBranchSize)) {
                    return row == absoluteBranchPosition;
                }
            }
            return false;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel result = null;
            label.setBorder(PropertyEditorTable.BORDER_RENDER);
            if ((net != null) && (value instanceof String)) {
                label.setText(value.toString());
                Node node = net.getNodeByReference(value.toString());
                if (node instanceof SignalTransition) {
                    SignalTransition st = (SignalTransition) node;
                    Color color = getTypeColor(st.getSignalType());
                    label.setForeground(color);
                }
                if (isActive(row, column)) {
                    label.setBackground(table.getSelectionBackground());
                } else {
                    label.setBackground(table.getBackground());
                }
                result = label;
            }
            return result;
        }
    }

    @Override
    public void createInterfacePanel(final GraphEditor editor) {
        super.createInterfacePanel(editor);
        stateMap = new HashMap<String, SignalState>();
        stateTable = new JTable(new StateTableModel());
        stateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stateTable.setRowHeight(FontHelper.getFontSizeInPixels(stateTable.getFont()));
        stateTable.setDefaultRenderer(Object.class, new StateTableCellRendererImplementation());
        statePane.setViewportView(stateTable);
        traceTable.setDefaultRenderer(Object.class, new TraceTableCellRendererImplementation());
    }

    @Override
    public void updateState(final GraphEditor editor) {
        super.updateState(editor);
        updateSignalState();
        stateTable.tableChanged(new TableModelEvent(traceTable.getModel()));
    }

    public void updateSignalState() {
        initialiseSignalState();
        ArrayList<String> combinedTrace = new ArrayList<>();
        if (!mainTrace.isEmpty()) {
            combinedTrace.addAll(mainTrace.subList(0, mainTrace.getPosition()));
        }
        if (!branchTrace.isEmpty()) {
            combinedTrace.addAll(branchTrace.subList(0, branchTrace.getPosition()));
        }

        for (String ref : combinedTrace) {
            Node node = net.getNodeByReference(ref);
            if (node instanceof SignalTransition) {
                SignalTransition transition = (SignalTransition) node;
                String signalReference = ((STG) net).getSignalReference(transition);
                SignalState signalState = stateMap.get(signalReference);
                if (signalState != null) {
                    switch (transition.getDirection()) {
                    case MINUS:
                        signalState.value = 0;
                        break;
                    case PLUS:
                        signalState.value = 1;
                        break;
                    case TOGGLE:
                        if (signalState.value == 1) {
                            signalState.value = 0;
                        } else if (signalState.value == 0) {
                            signalState.value = 1;
                        }
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        for (Node node: net.getTransitions()) {
            if (node instanceof SignalTransition) {
                SignalTransition transition = (SignalTransition) node;
                String signalReference = ((STG) net).getSignalReference(transition);
                SignalState st = stateMap.get(signalReference);
                if (st != null) {
                    st.excited |= net.isEnabled(transition);
                }
            }
        }
    }

    public void initialiseSignalState() {
        for (String signalName: stateMap.keySet()) {
            SignalState signalState = stateMap.get(signalName);
            signalState.value = -1;
            signalState.excited = false;
        }
    }

    @Override
    public void activated(final GraphEditor editor) {
        super.activated(editor);
        initialiseStateMap();
        setStatePaneVisibility(true);
    }

    @Override
    public void deactivated(final GraphEditor editor) {
        WorkspaceEntry we = editor.getWorkspaceEntry();
        Framework framework = Framework.getInstance();
        final Workspace workspace = framework.getWorkspace();
        final Path<String> directory = we.getWorkspacePath().getParent();
        final String desiredName = we.getWorkspacePath().getNode();
        VisualDtd dtd = new VisualDtd(new Dtd());
        VisualSTG stg = (VisualSTG) visualNet;
        HashMap<String, VisualSignal> signalMap = new HashMap<>();
        HashMap<SignalTransition, SignalEvent> transitionMap = new HashMap<>();
        double x = 0.0;
        for (String transitionName: mainTrace) {
            Node node = net.getNodeByReference(transitionName);
            if (node instanceof SignalTransition) {
                SignalTransition transition = (SignalTransition) node;
                String signalName = transition.getSignalName();
                
                VisualSignal signal = signalMap.get(signalName);
                if (signal == null) {
                    signal = dtd.createVisualSignal(signalName);
                    signal.setPosition(new Point2D.Double(0.0, 2.0 * signalMap.size()));
                    signalMap.put(signalName, signal);
                }
                Direction direction = (transition.getDirection() == SignalTransition.Direction.PLUS) ? Direction.PLUS : Direction.MINUS;
                SignalEvent signalEvent = dtd.appendSignalEvent(signal, direction);
                x += 2.0;
                signalEvent.edge.setX(x);
                transitionMap.put(transition, signalEvent);
                for (Node predPlace: net.getPreset(transition)) {
                    for (Node predTransition: net.getPreset(predPlace)) {
                        if (predTransition instanceof SignalTransition) {
                            SignalEvent predEvent = transitionMap.get(predTransition);
                            if (predEvent != null) {
                                try {
                                    dtd.connect(predEvent.edge, signalEvent.edge);
                                } catch (InvalidConnectionException e) {
                                }
                            }
                        }
                    }
                }
            }
        }
        final ModelEntry me = new ModelEntry(new DtdDescriptor(), dtd);
        workspace.add(directory, desiredName, me, true, true);
        super.deactivated(editor);
    }

    public String getTraceLabelByReference(String ref) {
        String result = null;
        if (ref != null) {
            String name = NamespaceHelper.getReferenceName(ref);
            String nameWithoutInstance = LabelParser.getTransitionName(name);
            if (nameWithoutInstance != null) {
                String path = NamespaceHelper.getReferencePath(ref);
                result = path + nameWithoutInstance;
            }
        }
        return result;
    }

    protected void initialiseStateMap() {
        stateMap.clear();
        for (Node node : net.getTransitions()) {
            if (node instanceof SignalTransition) {
                SignalTransition transition = (SignalTransition) node;
                String signalReference = ((STG) net).getSignalReference(transition);
                if (!stateMap.containsKey(signalReference)) {
                    SignalState signalState = new SignalState();
                    signalState.name = signalReference;
                    signalState.color = getTypeColor(transition.getSignalType());
                    stateMap.put(signalReference, signalState);
                }
            }
        }
        updateSignalState();
    }

    private Color getTypeColor(SignalTransition.Type type) {
        Color result = Color.BLACK;
        switch (type) {
        case INPUT:
            result = COLOR_INPUT;
            break;
        case OUTPUT:
            result = COLOR_OUTPUT;
            break;
        case INTERNAL:
            result = COLOR_INTERNAL;
            break;
        default:
            result = Color.BLACK;
        }
        return result;
    }

    @Override
    protected void coloriseTokens(Transition t) {
        VisualTransition vt = ((VisualSTG) visualNet).getVisualTransition(t);
        if (vt == null) return;
        Color tokenColor = Color.black;
        ColorGenerator tokenColorGenerator = vt.getTokenColorGenerator();
        if (tokenColorGenerator != null) {
            // generate token colour
            tokenColor = tokenColorGenerator.updateColor();
        } else {
            // combine preset token colours
            for (Connection c: visualNet.getConnections(vt)) {
                if ((c.getSecond() == vt) && (c instanceof VisualConnection)) {
                    VisualConnection vc = (VisualConnection) c;
                    if (vc.isTokenColorPropagator()) {
                        if (vc.getFirst() instanceof VisualPlace) {
                            VisualPlace vp = (VisualPlace) c.getFirst();
                            tokenColor = Coloriser.colorise(tokenColor, vp.getTokenColor());
                        } else if (vc instanceof VisualImplicitPlaceArc) {
                            VisualImplicitPlaceArc vipa = (VisualImplicitPlaceArc) vc;
                            tokenColor = Coloriser.colorise(tokenColor, vipa.getTokenColor());
                        }
                    }
                }
            }
        }
        // propagate the colour to postset tokens
        for (Connection c: visualNet.getConnections(vt)) {
            if ((c.getFirst() == vt) && (c instanceof VisualConnection)) {
                VisualConnection vc = (VisualConnection) c;
                if (vc.isTokenColorPropagator()) {
                    if (vc.getSecond() instanceof VisualPlace) {
                        VisualPlace vp = (VisualPlace) c.getSecond();
                        vp.setTokenColor(tokenColor);
                    } else if (vc instanceof VisualImplicitPlaceArc) {
                        VisualImplicitPlaceArc vipa = (VisualImplicitPlaceArc) vc;
                        vipa.setTokenColor(tokenColor);
                    }
                }
            }
        }
    }

}
