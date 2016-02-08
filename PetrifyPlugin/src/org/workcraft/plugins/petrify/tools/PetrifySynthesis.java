package org.workcraft.plugins.petrify.tools;

import org.workcraft.Framework;
import org.workcraft.SynthesisTool;
import org.workcraft.plugins.petrify.SynthesisResultHandler;
import org.workcraft.plugins.petrify.tasks.SynthesisTask;
import org.workcraft.plugins.stg.STGModel;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

abstract public class PetrifySynthesis extends SynthesisTool {

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.canHas(we, STGModel.class);
    }

    @Override
    public void run(WorkspaceEntry we) {
        SynthesisTask task = new SynthesisTask(we, getSynthesisParameter());
        final Framework framework = Framework.getInstance();
        framework.getTaskManager().queue(task, "Petrify logic synthesis", new SynthesisResultHandler(we));
    }

    abstract public String[] getSynthesisParameter();

}
