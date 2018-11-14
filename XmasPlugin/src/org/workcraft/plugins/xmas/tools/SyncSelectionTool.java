package org.workcraft.plugins.xmas.tools;

import org.workcraft.dom.visual.Positioning;
import org.workcraft.dom.visual.SelectionHelper;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.dom.visual.connections.VisualConnection.ScaleMode;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.graph.tools.SelectionTool;
import org.workcraft.plugins.xmas.components.VisualSyncComponent;
import org.workcraft.plugins.xmas.components.VisualXmasComponent;
import org.workcraft.util.Hierarchy;

import javax.swing.*;
import java.util.*;
import java.util.Map.Entry;

public class SyncSelectionTool extends SelectionTool {

    private HashMap<VisualConnection, ScaleMode> connectionToScaleModeMap = null;

    @Override
    public JPopupMenu createPopupMenu(VisualNode node, final GraphEditor editor) {
        JPopupMenu popup = super.createPopupMenu(node, editor);
        if (node instanceof VisualSyncComponent) {
            final VisualSyncComponent component = (VisualSyncComponent) node;
            if (popup != null) {
                popup.addSeparator();
            } else {
                popup = new JPopupMenu();
                popup.setFocusable(false);
            }
            JMenuItem addInputMenuItem = new JMenuItem("Add input-output pair");
            addInputMenuItem.addActionListener(event -> {
                editor.getWorkspaceEntry().saveMemento();
                component.addInput(Positioning.TOP);
                component.addOutput(Positioning.BOTTOM);
            });
            popup.add(addInputMenuItem);
            return popup;
        }

        return null;
    }

    @Override
    public Collection<VisualNode> getNodeWithAdjacentConnections(VisualModel model, VisualNode node) {
        HashSet<VisualNode> result = new HashSet<>();
        Queue<VisualNode> queue = new LinkedList<>();
        queue.add(node);
        while (!queue.isEmpty()) {
            node = queue.remove();
            if (result.contains(node)) {
                continue;
            }
            result.add(node);
            if (node instanceof VisualXmasComponent) {
                VisualXmasComponent component = (VisualXmasComponent) node;
                queue.addAll(component.getContacts());
            }
        }
        return result;
    }
    @Override
    public void beforeSelectionModification(final GraphEditor editor) {
        super.beforeSelectionModification(editor);
        // FIXME: A hack to preserve the shape of selected connections on relocation of their adjacent components (intro).
        // Flipping/rotation of VisualContacts are processed after ControlPoints of VisualConnections.
        // Therefore the shape of connections may change (e.g. if LOCK_RELATIVELY scale mode is selected).
        // To prevent this, the connection scale mode is temporary changed to NONE, and then restored (in afterSelectionModification).
        VisualModel model = editor.getModel();
        Collection<VisualConnection> connections = Hierarchy.getDescendantsOfType(model.getRoot(), VisualConnection.class);
        Collection<VisualConnection> includedConnections = SelectionHelper.getIncludedConnections(model.getSelection(), connections);
        connectionToScaleModeMap = new HashMap<>();
        for (VisualConnection vc: includedConnections) {
            connectionToScaleModeMap.put(vc, vc.getScaleMode());
            vc.setScaleMode(ScaleMode.NONE);
        }
    }

    @Override
    public void afterSelectionModification(final GraphEditor editor) {
        if (connectionToScaleModeMap != null) {
            for (Entry<VisualConnection, ScaleMode> entry: connectionToScaleModeMap.entrySet()) {
                VisualConnection vc = entry.getKey();
                ScaleMode scaleMode = entry.getValue();
                vc.setScaleMode(scaleMode);
            }
        }
        super.afterSelectionModification(editor);
    }

}
