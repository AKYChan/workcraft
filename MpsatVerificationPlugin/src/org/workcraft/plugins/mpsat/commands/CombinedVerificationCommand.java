package org.workcraft.plugins.mpsat.commands;

import java.util.ArrayList;

import org.workcraft.Framework;
import org.workcraft.gui.graph.commands.AbstractVerificationCommand;
import org.workcraft.plugins.mpsat.MpsatCombinedChainResultHandler;
import org.workcraft.plugins.mpsat.MpsatSettings;
import org.workcraft.plugins.mpsat.tasks.MpsatCombinedChainTask;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.tasks.TaskManager;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class CombinedVerificationCommand extends AbstractVerificationCommand {

    @Override
    public String getDisplayName() {
        return "Consistency, deadlock freeness, input properness and output persistency (reuse unfolding) [MPSat]";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, StgModel.class);
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public Position getPosition() {
        return null;
    }

    @Override
    public final void run(WorkspaceEntry we) {
        final ArrayList<MpsatSettings> settingsList = new ArrayList<>();
        settingsList.add(MpsatSettings.getConsistencySettings());
        settingsList.add(MpsatSettings.getDeadlockSettings());
        settingsList.add(MpsatSettings.getInputPropernessSettings());
        settingsList.add(MpsatSettings.getOutputPersistencySettings());

        final MpsatCombinedChainTask mpsatTask = new MpsatCombinedChainTask(we, settingsList);

        String description = "MPSat tool chain";
        String title = we.getTitle();
        if (!title.isEmpty()) {
            description += "(" + title + ")";
        }
        final Framework framework = Framework.getInstance();
        final TaskManager taskManager = framework.getTaskManager();
        final MpsatCombinedChainResultHandler monitor = new MpsatCombinedChainResultHandler(mpsatTask);
        taskManager.queue(mpsatTask, description, monitor);
    }

}
