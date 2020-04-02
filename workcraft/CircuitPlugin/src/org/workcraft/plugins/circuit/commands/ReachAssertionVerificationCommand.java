package org.workcraft.plugins.circuit.commands;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractVerificationCommand;
import org.workcraft.commands.ScriptableCommand;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.tasks.AssertionCheckTask;
import org.workcraft.plugins.circuit.utils.VerificationUtils;
import org.workcraft.plugins.mpsat_verification.MpsatDataSerialiser;
import org.workcraft.plugins.mpsat_verification.MpsatPresetManager;
import org.workcraft.plugins.mpsat_verification.VerificationParameters;
import org.workcraft.plugins.mpsat_verification.gui.ReachAssertionDialog;
import org.workcraft.plugins.mpsat_verification.tasks.VerificationChainResultHandlingMonitor;
import org.workcraft.plugins.mpsat_verification.utils.MpsatUtils;
import org.workcraft.tasks.TaskManager;
import org.workcraft.utils.ScriptableCommandUtils;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class ReachAssertionVerificationCommand extends AbstractVerificationCommand
        implements ScriptableCommand<Boolean> {

    private static final String PRESET_KEY = "reach-assertions.xml";
    private static final MpsatDataSerialiser DATA_SERIALISER = new MpsatDataSerialiser();

    private static VerificationParameters preservedData = null;

    @Override
    public String getDisplayName() {
        return "REACH assertion [MPSat]...";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Circuit.class);
    }

    @Override
    public Position getPosition() {
        return Position.BOTTOM;
    }

    @Override
    public Boolean execute(WorkspaceEntry we) {
        ScriptableCommandUtils.showErrorRequiresGui(getClass());
        return null;
    }

    @Override
    public void run(WorkspaceEntry we) {
        if (!checkPrerequisites(we)) {
            return;
        }
        Framework framework = Framework.getInstance();
        MainWindow mainWindow = framework.getMainWindow();
        MpsatPresetManager pmgr = new MpsatPresetManager(we, PRESET_KEY, DATA_SERIALISER, true, preservedData);
        ReachAssertionDialog dialog = new ReachAssertionDialog(mainWindow, pmgr);
        if (dialog.reveal()) {
            TaskManager manager = framework.getTaskManager();
            preservedData = dialog.getPresetData();
            AssertionCheckTask task = new AssertionCheckTask(we, preservedData);
            String description = MpsatUtils.getToolchainDescription(we.getTitle());
            VerificationChainResultHandlingMonitor monitor = new VerificationChainResultHandlingMonitor(we, true);
            manager.queue(task, description, monitor);
        }
    }

    private boolean checkPrerequisites(WorkspaceEntry we) {
        return isApplicableTo(we)
            && VerificationUtils.checkCircuitHasComponents(we)
            && VerificationUtils.checkInterfaceInitialState(we)
            && VerificationUtils.checkInterfaceConstrains(we);
    }

}
