package org.workcraft.plugins.circuit.commands;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.workcraft.NodeTransformer;
import org.workcraft.commands.AbstractTransformationCommand;
import org.workcraft.dom.Connection;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.ConnectionHelper;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.VisualCircuitConnection;
import org.workcraft.plugins.circuit.VisualJoint;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class DissolveJointTransformationCommand extends AbstractTransformationCommand implements NodeTransformer {

    @Override
    public String getDisplayName() {
        return "Dissolve joints (selected or all)";
    }

    @Override
    public String getPopupName() {
        return "Dissolve joint";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, VisualCircuit.class);
    }

    @Override
    public boolean isApplicableTo(Node node) {
        return node instanceof VisualJoint;
    }

    @Override
    public boolean isEnabled(ModelEntry me, Node node) {
        boolean result = false;
        if (node instanceof VisualJoint) {
            VisualModel visualModel = me.getVisualModel();
            if (visualModel != null) {
                result = (visualModel.getPreset(node).size() == 1) && (visualModel.getPostset(node).size() > 1);
            }
        }
        return result;
    }

    @Override
    public Position getPosition() {
        return Position.BOTTOM_MIDDLE;
    }

    public Collection<Node> collect(Model model) {
        Collection<Node> joints = new HashSet<>();
        if (model instanceof VisualCircuit) {
            VisualCircuit circuit = (VisualCircuit) model;
            joints.addAll(Hierarchy.getDescendantsOfType(circuit.getRoot(), VisualJoint.class));
            Collection<Node> selection = circuit.getSelection();
            if (!selection.isEmpty()) {
                HashSet<Node> selectedJoints = new HashSet<>(selection);
                selectedJoints.retainAll(joints);
                if (!selectedJoints.isEmpty()) {
                    joints.retainAll(selection);
                }
            }
        }
        return joints;
    }

    @Override
    public void transform(Model model, Node node) {
        if ((model instanceof VisualCircuit) && (node instanceof VisualJoint)) {
            VisualCircuit circuit = (VisualCircuit) model;
            VisualJoint joint = (VisualJoint) node;
            Set<Connection> connections = new HashSet<>(model.getConnections(node));
            VisualCircuitConnection predConnection = null;
            for (Connection connection: connections) {
                if (!(connection instanceof VisualCircuitConnection)) continue;
                if (connection.getSecond() == node) {
                    predConnection = (VisualCircuitConnection) connection;
                }
            }
            if (predConnection != null) {
                for (Connection connection: connections) {
                    if (!(connection instanceof VisualCircuitConnection)) continue;
                    if (connection.getFirst() == node) {
                        VisualCircuitConnection succConnection = (VisualCircuitConnection) connection;

                        LinkedList<Point2D> locations = ConnectionHelper.getMergedControlPoints(joint, predConnection, succConnection);
                        circuit.remove(succConnection);

                        Node fromNode = predConnection instanceof VisualCircuitConnection ? predConnection.getFirst() : null;
                        Node toNode = succConnection instanceof VisualCircuitConnection ? succConnection.getSecond() : null;
                        try {
                            VisualCircuitConnection newConnection = (VisualCircuitConnection) circuit.connect(fromNode, toNode);
                            newConnection.mixStyle(predConnection, succConnection);
                            ConnectionHelper.addControlPoints(newConnection, locations);
                        } catch (InvalidConnectionException e) {
                            LogUtils.logWarning(e.getMessage());
                        }
                    }
                }
            }
            circuit.remove(joint);
        }
    }

}
