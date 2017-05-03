package org.workcraft.plugins.petrify.commands;

import java.util.Collection;

import org.workcraft.Framework;
import org.workcraft.plugins.petrify.tasks.PetrifyTransformationResultHandler;
import org.workcraft.plugins.petrify.tasks.PetrifyTransformationTask;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.tasks.TaskManager;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class PetrifyUntoggleConversionCommand extends PetrifyAbstractConversionCommand {

    @Override
    public String getDisplayName() {
        return "Untoggle signal transitions [Petrify]";
    }

    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, StgModel.class);
    }

    @Override
    public WorkspaceEntry execute(WorkspaceEntry we) {
        final Framework framework = Framework.getInstance();
        final TaskManager taskManager = framework.getTaskManager();
        final PetrifyTransformationTask task = new PetrifyTransformationTask(we, "Signal transition untoggle", new String[] {"-untog"});
        boolean hasSignals = hasSignals(we);
        Collection<Mutex> mutexes = getMutexes(we);
        final PetrifyTransformationResultHandler monitor = new PetrifyTransformationResultHandler(we, !hasSignals, mutexes);
        taskManager.execute(task, "Petrify signal transition untoggle", monitor);
        return monitor.getResult();
    }

}
