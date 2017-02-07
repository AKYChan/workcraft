package org.workcraft.plugins.wtg.tools;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.workcraft.dom.Node;
import org.workcraft.dom.visual.ConnectionHelper;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.connections.ControlPoint;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.graph.tools.SelectionTool;
import org.workcraft.plugins.dtd.Transition.Direction;
import org.workcraft.plugins.dtd.VisualLevelConnection;
import org.workcraft.plugins.dtd.VisualSignal;
import org.workcraft.plugins.wtg.VisualState;
import org.workcraft.plugins.wtg.VisualWtg;
import org.workcraft.workspace.WorkspaceEntry;

public class WtgSelectionTool extends SelectionTool {

    public WtgSelectionTool() {
        super(false, false, false, false);
    }

    @Override
    public void setup(final GraphEditor editor) {
        super.setup(editor);
        editor.getWorkspaceEntry().setCanCopy(false);
    }

    @Override
    public void mouseClicked(GraphEditorMouseEvent e) {
        boolean processed = false;
        WorkspaceEntry we = e.getEditor().getWorkspaceEntry();
        VisualWtg model = (VisualWtg) e.getModel();
        if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() > 1)) {
            Node node = HitMan.hitTestCurrentLevelFirst(e.getPosition(), model);
            if (node instanceof VisualState) {
                we.saveMemento();
                VisualState state = (VisualState) node;
                boolean isInitial = state.getReferencedState().isInitial();
                state.getReferencedState().setInitial(!isInitial);
                processed = true;
            } else if (node instanceof VisualSignal) {
                we.saveMemento();
                VisualSignal signal = (VisualSignal) node;
                Direction direction = null;
                switch (e.getKeyModifiers()) {
                case MouseEvent.SHIFT_DOWN_MASK:
                    direction = Direction.RISE;
                    break;
                case MouseEvent.CTRL_DOWN_MASK:
                    direction = Direction.FALL;
                    break;
                case MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK:
                    direction = Direction.DESTABILISE;
                    break;
                }
                model.appendSignalEvent(signal, direction);
                processed = true;
            } else if (node instanceof VisualLevelConnection) {
                we.saveMemento();
                VisualLevelConnection connection = (VisualLevelConnection) node;
                model.insetrSignalPulse(connection);
                processed = true;
            }
        }
        if (!processed) {
            super.mouseClicked(e);
        }
    }

    @Override
    public void beforeSelectionModification(final GraphEditor editor) {
        super.beforeSelectionModification(editor);
        VisualModel model = editor.getModel();
        ArrayList<Node> selection = new ArrayList<>(model.getSelection());
        for (Node node : selection) {
            VisualConnection connection = null;
            if (node instanceof VisualConnection) {
                connection = (VisualConnection) node;
            } else if (node instanceof ControlPoint) {
                connection = ConnectionHelper.getParentConnection((ControlPoint) node);
            }
            if (connection instanceof VisualLevelConnection) {
                model.removeFromSelection(node);
            }
        }
    }

}
