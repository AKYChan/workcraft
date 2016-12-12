package org.workcraft.plugins.son.tools;

import org.workcraft.Tool;
import org.workcraft.plugins.son.SON;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

public class ClearMarkingTool implements Tool {

    @Override
    public boolean isApplicableTo(ModelEntry me) {
        return WorkspaceUtils.isApplicable(me, SON.class);
    }

    @Override
    public String getSection() {
        return "Custom tools";
    }

    @Override
    public String getDisplayName() {
        return "Reset marking";
    }

    @Override
    public ModelEntry run(ModelEntry me) {
        SON net = (SON) me.getMathModel();
        net.clearMarking();
        return me;
    }

    @Override
    public WorkspaceEntry run(WorkspaceEntry we) {
        run(we.getModelEntry());
        return we;
    }

}
