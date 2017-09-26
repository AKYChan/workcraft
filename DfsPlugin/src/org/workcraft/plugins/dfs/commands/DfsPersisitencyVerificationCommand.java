package org.workcraft.plugins.dfs.commands;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractVerificationCommand;
import org.workcraft.plugins.dfs.Dfs;
import org.workcraft.plugins.dfs.tasks.CheckDataflowPersistencydTask;
import org.workcraft.plugins.mpsat.MpsatChainResultHandler;
import org.workcraft.plugins.mpsat.MpsatUtils;
import org.workcraft.plugins.mpsat.tasks.MpsatChainResult;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.TaskManager;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class DfsPersisitencyVerificationCommand extends AbstractVerificationCommand {

    @Override
    public int getPriority() {
        return 1;
    }

    public String getDisplayName() {
        return "Output persistency [MPSat]";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Dfs.class);
    }

    @Override
    public Boolean execute(WorkspaceEntry we) {
        Framework framework = Framework.getInstance();
        TaskManager manager = framework.getTaskManager();
        CheckDataflowPersistencydTask task = new CheckDataflowPersistencydTask(we);
        String description = MpsatUtils.getToolchainDescription(we.getTitle());
        Result<? extends MpsatChainResult> result = manager.execute(task, description);
        return MpsatUtils.getChainOutcome(result);
    }

    @Override
    public void run(WorkspaceEntry we) {
        Framework framework = Framework.getInstance();
        TaskManager manager = framework.getTaskManager();
        CheckDataflowPersistencydTask task = new CheckDataflowPersistencydTask(we);
        String description = MpsatUtils.getToolchainDescription(we.getTitle());
        MpsatChainResultHandler monitor = new MpsatChainResultHandler(task);
        manager.queue(task, description, monitor);
    }

}
