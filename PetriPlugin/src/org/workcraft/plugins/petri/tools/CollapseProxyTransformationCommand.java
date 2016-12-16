package org.workcraft.plugins.petri.tools;

import java.util.Collection;
import java.util.HashSet;

import org.workcraft.AbstractTransformationCommand;
import org.workcraft.NodeTransformer;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petri.PetriNetUtils;
import org.workcraft.plugins.petri.VisualReadArc;
import org.workcraft.plugins.petri.VisualReplicaPlace;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class CollapseProxyTransformationCommand extends AbstractTransformationCommand implements NodeTransformer {

    @Override
    public String getDisplayName() {
        return "Collapse proxy places (selected or all)";
    }

    @Override
    public String getPopupName() {
        return "Collapse proxy place";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, PetriNetModel.class);
    }

    @Override
    public boolean isApplicableTo(Node node) {
        return node instanceof VisualReplicaPlace;
    }

    @Override
    public boolean isEnabled(ModelEntry me, Node node) {
        return true;
    }

    @Override
    public Position getPosition() {
        return Position.MIDDLE;
    }

    @Override
    public Collection<Node> collect(Model model) {
        Collection<Node> replicas = new HashSet<>();
        if (model instanceof VisualModel) {
            VisualModel visualModel = (VisualModel) model;
            // Collect selected (or all) replicas
            replicas.addAll(PetriNetUtils.getVisualReplicaPlaces(visualModel));
            Collection<Node> selection = visualModel.getSelection();
            if (!selection.isEmpty()) {
                replicas.retainAll(selection);
            }
            // Collect replicas on selected (or all) read-arcs
            HashSet<VisualReadArc> readArcs = PetriNetUtils.getVisualReadArcs(visualModel);
            if (!selection.isEmpty()) {
                readArcs.retainAll(selection);
            }
            if (!readArcs.isEmpty()) {
                for (VisualReadArc readArc: readArcs) {
                    if (readArc.getFirst() instanceof VisualReplicaPlace) {
                        VisualReplicaPlace replica = (VisualReplicaPlace) readArc.getFirst();
                        replicas.add(replica);
                    }
                }
            }
        }
        return replicas;
    }

    @Override
    public void transform(Model model, Node node) {
        if ((model instanceof VisualModel) && (node instanceof VisualReplicaPlace)) {
            VisualModel visualModel = (VisualModel) model;
            VisualReplicaPlace replicaPlace = (VisualReplicaPlace) node;
            PetriNetUtils.collapseReplicaPlace(visualModel, replicaPlace);
        }
    }

}
