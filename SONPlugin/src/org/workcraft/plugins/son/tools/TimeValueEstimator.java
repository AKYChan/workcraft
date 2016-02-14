package org.workcraft.plugins.son.tools;

import org.workcraft.Tool;
import org.workcraft.gui.ToolboxPanel;
import org.workcraft.plugins.son.SON;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class TimeValueEstimator implements Tool {

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.canHas(we, SON.class);
    }

    @Override
    public String getSection() {
        return "Time analysis";
    }

    @Override
    public String getDisplayName() {
        return "Estimate unspecified values";
    }

    @Override
    public void run(WorkspaceEntry we) {
        final ToolboxPanel toolbox = ToolManager.getToolboxPanel(we);
        final TimeValueSetterTool tool = toolbox.getToolInstance(TimeValueSetterTool.class);
        toolbox.selectTool(tool);
    }

}
