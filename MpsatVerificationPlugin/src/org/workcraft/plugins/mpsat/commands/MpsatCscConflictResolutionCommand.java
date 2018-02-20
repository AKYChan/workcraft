package org.workcraft.plugins.mpsat.commands;

import java.util.Collection;

import org.workcraft.Framework;
import org.workcraft.commands.ScriptableCommand;
import org.workcraft.plugins.mpsat.MpsatMode;
import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.mpsat.MpsatParameters.SolutionMode;
import org.workcraft.plugins.mpsat.tasks.MpsatChainOutput;
import org.workcraft.plugins.mpsat.tasks.MpsatChainResultHandler;
import org.workcraft.plugins.mpsat.tasks.MpsatChainTask;
import org.workcraft.plugins.mpsat.tasks.MpsatCscConflictResolutionOutputHandler;
import org.workcraft.plugins.mpsat.tasks.MpsatOutput;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.MutexUtils;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.TaskManager;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class MpsatCscConflictResolutionCommand implements ScriptableCommand<WorkspaceEntry> {

    private static final String TITLE = "Resolution of CSC conflicts";

    @Override
    public String getSection() {
        return "Encoding conflicts";
    }

    @Override
    public String getDisplayName() {
        return "Resolve CSC conflicts [MPSat]";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, StgModel.class);
    }

    @Override
    public void run(WorkspaceEntry we) {
        queueCscConflictResolution(we);
    }

    @Override
    public WorkspaceEntry execute(WorkspaceEntry we) {
        MpsatChainResultHandler monitor = queueCscConflictResolution(we);
        Collection<Mutex> mutexes = monitor.getMutexes();
        Result<? extends MpsatChainOutput> chainResult = monitor.waitResult();
        MpsatChainOutput chainOutput = chainResult.getPayload();
        Result<? extends MpsatOutput> mpsatResult = chainOutput.getMpsatResult();
        MpsatOutput mpsatOutput = mpsatResult.getPayload();
        MpsatCscConflictResolutionOutputHandler resultHandler = new MpsatCscConflictResolutionOutputHandler(
                we, mpsatOutput, mutexes);

        resultHandler.run();
        return resultHandler.getResult();
    }

    private MpsatChainResultHandler queueCscConflictResolution(WorkspaceEntry we) {
        MpsatParameters settings = new MpsatParameters(TITLE,
                MpsatMode.RESOLVE_ENCODING_CONFLICTS, 4, SolutionMode.MINIMUM_COST, 1);

        Framework framework = Framework.getInstance();
        TaskManager taskManager = framework.getTaskManager();
        MpsatChainTask task = new MpsatChainTask(we, settings);
        Stg stg = WorkspaceUtils.getAs(we, Stg.class);
        Collection<Mutex> mutexes = MutexUtils.getMutexes(stg);
        MutexUtils.logInfoPossiblyImplementableMutex(mutexes);
        MpsatChainResultHandler monitor = new MpsatChainResultHandler(we, mutexes);
        taskManager.queue(task, TITLE, monitor);
        return monitor;
    }

}
