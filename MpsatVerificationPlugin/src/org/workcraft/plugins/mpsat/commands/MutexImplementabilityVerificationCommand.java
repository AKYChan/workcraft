package org.workcraft.plugins.mpsat.commands;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractVerificationCommand;
import org.workcraft.plugins.mpsat.VerificationParameters;
import org.workcraft.plugins.mpsat.tasks.CombinedChainOutput;
import org.workcraft.plugins.mpsat.tasks.CombinedChainResultHandler;
import org.workcraft.plugins.mpsat.tasks.CombinedChainTask;
import org.workcraft.plugins.mpsat.utils.MpsatUtils;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.utils.MutexUtils;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.TaskManager;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.utils.WorkspaceUtils;

import java.util.ArrayList;
import java.util.Collection;

public class MutexImplementabilityVerificationCommand extends AbstractVerificationCommand {

    @Override
    public String getDisplayName() {
        return "Mutex place implementability [MPSat]";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, StgModel.class);
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public void run(WorkspaceEntry we) {
        queueVerification(we);
    }

    @Override
    public Boolean execute(WorkspaceEntry we) {
        CombinedChainResultHandler monitor = queueVerification(we);
        Result<? extends CombinedChainOutput> result = null;
        if (monitor != null) {
            result = monitor.waitResult();
        }
        return MpsatUtils.getCombinedChainOutcome(result);
    }

    private CombinedChainResultHandler queueVerification(WorkspaceEntry we) {
        CombinedChainResultHandler monitor = null;
        if (isApplicableTo(we)) {
            Stg stg = WorkspaceUtils.getAs(we, Stg.class);
            if (MpsatUtils.mutexStructuralCheck(stg, false)) {
                Framework framework = Framework.getInstance();
                TaskManager manager = framework.getTaskManager();
                Collection<Mutex> mutexes = MutexUtils.getMutexes(stg);
                MutexUtils.logInfoPossiblyImplementableMutex(mutexes);
                ArrayList<VerificationParameters> settingsList = MpsatUtils.getMutexImplementabilitySettings(mutexes);
                CombinedChainTask task = new CombinedChainTask(we, settingsList, null);
                String description = MpsatUtils.getToolchainDescription(we.getTitle());
                monitor = new CombinedChainResultHandler(task, mutexes);
                manager.queue(task, description, monitor);
            }
        }
        return monitor;
    }

}