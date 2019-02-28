package org.workcraft.plugins.mpsat.commands;

import org.workcraft.commands.NodeTransformer;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.stg.VisualImplicitPlaceArc;
import org.workcraft.utils.DialogUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.utils.WorkspaceUtils;

import java.util.HashSet;

public class MpsatPlaceRedundancyVerificationCommand extends MpsatAbstractVerificationCommand implements NodeTransformer {

    @Override
    public String getDisplayName() {
        return "Redundancy of selected places [MPSat]";
    }

    @Override
    public String getPopupName() {
        return "Check place redundnacy";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, PetriNetModel.class);
    }

    @Override
    public boolean isApplicableTo(VisualNode node) {
        return (node instanceof VisualPlace) || (node instanceof VisualImplicitPlaceArc);
    }

    @Override
    public boolean isEnabled(ModelEntry me, VisualNode node) {
        return true;
    }

    @Override
    public Position getPosition() {
        return Position.TOP_MIDDLE;
    }

    @Override
    public void run(WorkspaceEntry we) {
        if (getSelectedPlaces(we).size() < 1) {
            DialogUtils.showWarning("At least one place must be selected for redundancy check.");
            return;
        }
        super.run(we);
    }

    @Override
    public MpsatParameters getSettings(WorkspaceEntry we) {
        HashSet<String> placeNames = getSelectedPlaces(we);
        return MpsatParameters.getPlaceRedundancySettings(placeNames);
    }

    protected HashSet<String> getSelectedPlaces(WorkspaceEntry we) {
        VisualModel model = we.getModelEntry().getVisualModel();
        HashSet<String> placeNames = new HashSet<>();
        for (VisualNode node: model.getSelection()) {
            if ((node instanceof VisualPlace) || (node instanceof VisualImplicitPlaceArc)) {
                String placeName = model.getMathReference(node);
                if (placeName != null) {
                    placeNames.add(placeName);
                }
            }
        }
        return placeNames;
    }

    @Override
    public void transform(VisualModel model, VisualNode node) {
        // No transformation applied.
    }

}
