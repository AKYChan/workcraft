package org.workcraft.plugins.son.commands;

import org.workcraft.commands.Command;
import org.workcraft.gui.Toolbox;
import org.workcraft.plugins.son.SON;
import org.workcraft.plugins.son.tools.TimeValueSetterTool;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class TimeValueSetterCommand implements Command {

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, SON.class);
    }

    @Override
    public String getSection() {
        return "Time analysis";
    }

    @Override
    public String getDisplayName() {
        return "Set time value";
    }

    @Override
    public void run(WorkspaceEntry we) {
        final Toolbox toolbox = ToolManager.getToolboxPanel(we);
        final TimeValueSetterTool tool = toolbox.getToolInstance(TimeValueSetterTool.class);
        toolbox.selectTool(tool);
    }

}
