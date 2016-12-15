package org.workcraft.plugins.dfs.tools;

import org.workcraft.AbstractConversionCommand;
import org.workcraft.plugins.dfs.Dfs;
import org.workcraft.plugins.dfs.VisualDfs;
import org.workcraft.plugins.dfs.stg.DfsToStgConverter;
import org.workcraft.plugins.stg.StgDescriptor;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class DfsToStgConversionCommand extends AbstractConversionCommand {

    @Override
    public String getDisplayName() {
        return "Signal Transition Graph";
    }

    @Override
    public boolean isApplicableTo(ModelEntry me) {
        return WorkspaceUtils.isApplicableExact(me, Dfs.class);
    }

    @Override
    public ModelEntry convert(ModelEntry me) {
        final VisualDfs dfs = (VisualDfs) me.getVisualModel();
        final DfsToStgConverter converter = new DfsToStgConverter(dfs);
        return new ModelEntry(new StgDescriptor(), converter.getStgModel());
    }

}
