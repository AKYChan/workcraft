package org.workcraft.plugins.fst.commands;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractConversionCommand;
import org.workcraft.plugins.fst.tasks.StgToFstConversionResultHandler;
import org.workcraft.plugins.fst.tasks.WriteSgConversionTask;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.tasks.TaskManager;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.DialogUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class StgToFstConversionCommand extends AbstractConversionCommand {

    public boolean isBinary() {
        return false;
    }

    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public String getDisplayName() {
        if (isBinary()) {
            return "Finite State Transducer (binary-encoded) [Petrify]";
        } else {
            return "Finite State Transducer (basic) [Petrify]";
        }
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Stg.class);
    }

    @Override
    public WorkspaceEntry execute(WorkspaceEntry we) {
        if (Hierarchy.isHierarchical(we.getModelEntry())) {
            DialogUtils.showError(
                    "Finite State Transducer cannot be derived from a hierarchical Signal Transition Graph.",
                    "Conversion error");
            return null;
        }
        final Framework framework = Framework.getInstance();
        final TaskManager taskManager = framework.getTaskManager();
        final WriteSgConversionTask task = new WriteSgConversionTask(we, isBinary());
        final StgToFstConversionResultHandler monitor = new StgToFstConversionResultHandler(task);
        taskManager.execute(task, "Building state graph", monitor);
        return monitor.getResult();
    }

    @Override
    public ModelEntry convert(ModelEntry me) {
        return null; // !!!
    }

}
