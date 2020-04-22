package org.workcraft.plugins.wtg.commands;

import org.workcraft.commands.AbstractVerificationCommand;
import org.workcraft.commands.ScriptableCommand;
import org.workcraft.plugins.wtg.Wtg;
import org.workcraft.plugins.wtg.utils.VerificationUtils;
import org.workcraft.utils.DialogUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.utils.WorkspaceUtils;

public class ReachabilityVerificationCommand extends AbstractVerificationCommand implements ScriptableCommand<Boolean> {
    private static final String TITLE = "Verification result";

    @Override
    public String getDisplayName() {
        return "Reachability";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Wtg.class);
    }

    @Override
    public void run(WorkspaceEntry we) {
        execute(we);
    }

    @Override
    public Boolean execute(WorkspaceEntry we) {
        if (!isApplicableTo(we)) {
            return null;
        }
        final Wtg wtg = WorkspaceUtils.getAs(we, Wtg.class);
        boolean result = VerificationUtils.checkReachability(wtg);
        if (result) {
            DialogUtils.showInfo("All nodes and transitions are reachable.", TITLE);
        }
        return result;
    }
}
