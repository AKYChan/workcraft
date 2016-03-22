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

package org.workcraft.plugins.transform;

import java.util.Collection;
import java.util.HashSet;

import org.workcraft.NodeTransformer;
import org.workcraft.TransformationTool;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.connections.Bezier;
import org.workcraft.dom.visual.connections.ConnectionGraphic;
import org.workcraft.dom.visual.connections.Polyline;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.dom.visual.connections.VisualConnection.ConnectionType;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class StraightenConnectionsTool extends TransformationTool implements NodeTransformer {

    @Override
    public String getDisplayName() {
        return "Straighten connections (selected or all)";
    }

    @Override
    public String getPopupName() {
        return "Straighten connection";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.canHas(we, VisualModel.class);
    }

    @Override
    public boolean isApplicableTo(Node node) {
        return node instanceof VisualConnection;
    }

    @Override
    public boolean isEnabled(WorkspaceEntry we, Node node) {
        boolean result = false;
        if (node instanceof VisualConnection) {
            VisualConnection connection = (VisualConnection) node;
            ConnectionGraphic graphic = connection.getGraphic();
            if (graphic instanceof Bezier) {
                result = true;
            } else if (graphic instanceof Polyline) {
                Polyline polyline = (Polyline) graphic;
                result = polyline.getControlPointCount() > 0;
            }
        }
        return result;
    }

    @Override
    public Position getPosition() {
        return Position.BOTTOM;
    }

    @Override
    public void run(WorkspaceEntry we) {
        VisualModel visualModel = WorkspaceUtils.getAs(we, VisualModel.class);
        if (visualModel != null) {
            Collection<VisualConnection> connections = Hierarchy.getDescendantsOfType(visualModel.getRoot(), VisualConnection.class);
            Collection<Node> selection = visualModel.getSelection();
            if (!selection.isEmpty()) {
                HashSet<Node> selectedConnections = new HashSet<>(selection);
                selectedConnections.retainAll(connections);
                if (!selectedConnections.isEmpty()) {
                    connections.retainAll(selection);
                }
            }
            if (!connections.isEmpty()) {
                we.saveMemento();
                for (VisualConnection connection: connections) {
                    transform(visualModel, connection);
                }
            }
        }
    }

    @Override
    public void transform(Model model, Node node) {
        if (node instanceof VisualConnection) {
            VisualConnection connection = (VisualConnection) node;
            connection.setConnectionType(ConnectionType.BEZIER);
            connection.setConnectionType(ConnectionType.POLYLINE);
        }
    }

}
