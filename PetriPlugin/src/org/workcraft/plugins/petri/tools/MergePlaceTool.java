package org.workcraft.plugins.petri.tools;

import java.util.Set;

import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.gui.graph.tools.AbstractMergerTool;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.workspace.ModelEntry;

public final class MergePlaceTool extends AbstractMergerTool {

    @Override
    public String getDisplayName() {
        return "Merge selected places";
    }

    @Override
    public boolean isApplicableTo(ModelEntry me) {
        return me.getMathModel() instanceof PetriNetModel;
    }

    @Override
    public Set<Class<? extends VisualComponent>> getMergableClasses() {
        Set<Class<? extends VisualComponent>> result = super.getMergableClasses();
        result.add(VisualPlace.class);
        return result;
    }

    @Override
    public Position getPosition() {
        return null;
    }

}
