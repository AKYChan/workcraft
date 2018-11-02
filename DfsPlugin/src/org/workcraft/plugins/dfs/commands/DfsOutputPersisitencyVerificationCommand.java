package org.workcraft.plugins.dfs.commands;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractVerificationCommand;
import org.workcraft.plugins.dfs.Dfs;
import org.workcraft.plugins.dfs.tasks.DfsOutputPersistencyCheckTask;
import org.workcraft.plugins.mpsat.tasks.MpsatChainOutput;
import org.workcraft.plugins.mpsat.tasks.MpsatChainResultHandler;
import org.workcraft.plugins.mpsat.tasks.MpsatUtils;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.TaskManager;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class DfsOutputPersisitencyVerificationCommand extends AbstractVerificationCommand {

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
    public void run(WorkspaceEntry we) {
        queueVerification(we);
    }

    @Override
    public Boolean execute(WorkspaceEntry we) {
        MpsatChainResultHandler monitor = queueVerification(we);
        Result<? extends MpsatChainOutput> result = null;
        if (monitor != null) {
            result = monitor.waitResult();
        }
        return MpsatUtils.getChainOutcome(result);
    }

    private MpsatChainResultHandler queueVerification(WorkspaceEntry we) {
        if (!isApplicableTo(we)) {
            return null;
        }
        Framework framework = Framework.getInstance();
        TaskManager manager = framework.getTaskManager();
        DfsOutputPersistencyCheckTask task = new DfsOutputPersistencyCheckTask(we);
        String description = MpsatUtils.getToolchainDescription(we.getTitle());
        MpsatChainResultHandler monitor = new MpsatChainResultHandler(we);
        manager.queue(task, description, monitor);
        return monitor;
    }

}
