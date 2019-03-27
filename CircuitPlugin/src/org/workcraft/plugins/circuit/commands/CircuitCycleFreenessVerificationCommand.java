package org.workcraft.plugins.circuit.commands;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractVerificationCommand;
import org.workcraft.dom.references.ReferenceHelper;
import org.workcraft.gui.Toolbox;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.FunctionComponent;
import org.workcraft.plugins.circuit.tools.CycleAnalyserTool;
import org.workcraft.plugins.circuit.utils.CycleUtils;
import org.workcraft.plugins.circuit.utils.VerificationUtils;
import org.workcraft.utils.DialogUtils;
import org.workcraft.utils.LogUtils;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.util.Collection;
import java.util.List;

public class CircuitCycleFreenessVerificationCommand extends AbstractVerificationCommand {

    @Override
    public String getDisplayName() {
        return "Absence of unbroken cycles";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Circuit.class);
    }

    @Override
    public Position getPosition() {
        return Position.BOTTOM_MIDDLE;
    }

    @Override
    public Boolean execute(WorkspaceEntry we) {
        if (!checkPrerequisites(we)) {
            return null;
        }
        Circuit circuit = WorkspaceUtils.getAs(we, Circuit.class);
        Collection<FunctionComponent> cycledComponents = CycleUtils.getCycledComponents(circuit);
        if (cycledComponents.isEmpty()) {
            DialogUtils.showInfo("The circuit does not have unbroken cycles.");
            return true;
        } else {
            final Framework framework = Framework.getInstance();
            if (framework.isInGuiMode()) {
                final Toolbox toolbox = framework.getMainWindow().getCurrentToolbox();
                toolbox.selectTool(toolbox.getToolInstance(CycleAnalyserTool.class));
            }
            List<String> loopedComponentRefs = ReferenceHelper.getReferenceList(circuit, cycledComponents);
            String msg = "The circuit has unbroken cycles.\n" +
                    LogUtils.getTextWithRefs("Problematic components", loopedComponentRefs);

            DialogUtils.showError(msg);
            return false;
        }
    }

    private boolean checkPrerequisites(WorkspaceEntry we) {
        return isApplicableTo(we) && VerificationUtils.checkCircuitHasComponents(we);
    }

}
