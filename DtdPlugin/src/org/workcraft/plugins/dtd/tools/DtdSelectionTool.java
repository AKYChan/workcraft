package org.workcraft.plugins.dtd.tools;

import org.workcraft.dom.Node;
import org.workcraft.dom.visual.ConnectionHelper;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.connections.ControlPoint;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.DesktopApi;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.graph.tools.SelectionTool;
import org.workcraft.plugins.dtd.TransitionEvent;
import org.workcraft.plugins.dtd.VisualDtd;
import org.workcraft.plugins.dtd.VisualLevelConnection;
import org.workcraft.plugins.dtd.VisualSignal;
import org.workcraft.util.DialogUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class DtdSelectionTool extends SelectionTool {

    public DtdSelectionTool() {
        super(false, false, false, false);
    }

    @Override
    public void setPermissions(final GraphEditor editor) {
        WorkspaceEntry we = editor.getWorkspaceEntry();
        we.setCanModify(true);
        we.setCanSelect(true);
        we.setCanCopy(false);
    }

    @Override
    public void mouseClicked(GraphEditorMouseEvent e) {
        boolean processed = false;
        WorkspaceEntry we = e.getEditor().getWorkspaceEntry();
        VisualDtd model = (VisualDtd) e.getModel();
        if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() > 1)) {
            Node node = HitMan.hitFirstInCurrentLevel(e.getPosition(), model);
            if (node instanceof VisualSignal) {
                we.saveMemento();
                VisualSignal signal = (VisualSignal) node;
                try {
                    VisualDtd.SignalEvent signalEvent = model.appendSignalEvent(signal, null);
                    TransitionEvent.Direction direction = getDesiredDirection(e);
                    if ((signalEvent != null) && (direction != null)) {
                        signalEvent.edge.setDirection(direction);
                    }
                } catch (Throwable t) {
                    DialogUtils.showError(t.getMessage());
                }
                processed = true;
            }
        }
        if (!processed) {
            super.mouseClicked(e);
        }
    }

    private TransitionEvent.Direction getDesiredDirection(GraphEditorMouseEvent e) {
        switch (e.getKeyModifiers()) {
        case MouseEvent.SHIFT_DOWN_MASK:
            return TransitionEvent.Direction.RISE;
        case MouseEvent.CTRL_DOWN_MASK:
            return TransitionEvent.Direction.FALL;
        case MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK:
            return TransitionEvent.Direction.DESTABILISE;
        default:
            return null;
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

    @Override
    public String getHintText(final GraphEditor editor) {
        return "Double-click on a signal to add its transition. Hold Shift for rising edge, " +
                DesktopApi.getMenuKeyMaskName() + " for falling edge, or both keys for unstable state.";
    }

}
