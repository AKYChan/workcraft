package org.workcraft.plugins.fst.tools;

import org.workcraft.AbstractConversionCommand;
import org.workcraft.plugins.fsm.FsmDescriptor;
import org.workcraft.plugins.fsm.VisualFsm;
import org.workcraft.plugins.fst.Fst;
import org.workcraft.plugins.fst.VisualFst;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class FsmToFstConversionCommand extends AbstractConversionCommand {

    @Override
    public String getDisplayName() {
        return "Finite State Transducer";
    }

    @Override
    public boolean isApplicableTo(ModelEntry me) {
        return WorkspaceUtils.isApplicableExact(me, VisualFsm.class);
    }

    @Override
    public ModelEntry convert(ModelEntry me) {
        final VisualFsm src = WorkspaceUtils.getAs(me, VisualFsm.class);
        final VisualFst dst = new VisualFst(new Fst());
        final FsmToFstConverter converter = new FsmToFstConverter(src, dst);
        return new ModelEntry(new FsmDescriptor(), converter.getDstModel());
    }

}
