package org.workcraft.plugins.petrify.tools;

import java.util.ArrayList;

import org.workcraft.ConversionTool;
import org.workcraft.Framework;
import org.workcraft.plugins.fsm.Fsm;
import org.workcraft.plugins.fst.Fst;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petrify.tasks.TransformationResultHandler;
import org.workcraft.plugins.petrify.tasks.TransformationTask;
import org.workcraft.plugins.stg.STGModel;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class PetrifyNetSynthesis extends ConversionTool {

    @Override
    public String getDisplayName() {
        return "Net synthesis [Petrify]";
    }

    @Override
    public Position getPosition() {
        return Position.BOTTOM;
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.canHas(we, PetriNetModel.class) || WorkspaceUtils.canHas(we, Fsm.class);
    }

    @Override
    public void run(WorkspaceEntry we) {
        ArrayList<String> args = getArgs();
        final TransformationTask task = new TransformationTask(we, "Net synthesis", args.toArray(new String[args.size()]));
        final Framework framework = Framework.getInstance();
        boolean hasSignals = WorkspaceUtils.canHas(we, STGModel.class) || WorkspaceUtils.canHas(we, Fst.class);
        TransformationResultHandler monitor = new TransformationResultHandler(we, hasSignals);
        framework.getTaskManager().queue(task, "Petrify net synthesis", monitor);
    }

    public ArrayList<String> getArgs() {
        return new ArrayList<>();
    }

}
