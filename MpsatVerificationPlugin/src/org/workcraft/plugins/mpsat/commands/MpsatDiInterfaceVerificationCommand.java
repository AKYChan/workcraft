package org.workcraft.plugins.mpsat.commands;

import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class MpsatDiInterfaceVerificationCommand extends MpsatAbstractVerificationCommand {

    @Override
    public String getDisplayName() {
        return "Delay insensitive interface (without dummies) [MPSat]";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, StgModel.class);
    }

    @Override
    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public MpsatParameters getSettings() {
        return MpsatParameters.getDiInterfaceSettings();
    }

}
