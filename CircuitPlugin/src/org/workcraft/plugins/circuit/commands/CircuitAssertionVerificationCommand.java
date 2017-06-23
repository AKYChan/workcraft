package org.workcraft.plugins.circuit.commands;

import java.io.File;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.graph.commands.AbstractVerificationCommand;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.tasks.CustomCheckCircuitTask;
import org.workcraft.plugins.mpsat.MpsatChainResultHandler;
import org.workcraft.plugins.mpsat.MpsatPresetManager;
import org.workcraft.plugins.mpsat.MpsatSettingsSerialiser;
import org.workcraft.plugins.mpsat.commands.MpsatAssertionVerificationCommand;
import org.workcraft.plugins.mpsat.gui.MpsatAssertionDialog;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgUtils;
import org.workcraft.tasks.TaskManager;
import org.workcraft.util.GUI;
import org.workcraft.util.MessageUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class CircuitAssertionVerificationCommand extends AbstractVerificationCommand {

    @Override
    public String getDisplayName() {
        return "Custom assertion [MPSat]...";
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
    public void run(WorkspaceEntry we) {
        final Framework framework = Framework.getInstance();
        final MainWindow mainWindow = framework.getMainWindow();

        Circuit circuit = WorkspaceUtils.getAs(we, Circuit.class);
        if (circuit.getFunctionComponents().isEmpty()) {
            MessageUtils.showError("The circuit must have components.");
        } else {
            VisualCircuit visualCircuit = WorkspaceUtils.getAs(we, VisualCircuit.class);
            File envFile = visualCircuit.getEnvironmentFile();
            Stg envStg = StgUtils.loadStg(envFile);
            if (envStg == null) {
                String messagePrefix = "";
                if (envFile != null) {
                    messagePrefix = "Cannot read an STG model from the file:\n" + envFile.getAbsolutePath() + "\n\n";
                }
                MessageUtils.showWarning(messagePrefix + "The circuit will be verified without environment STG.");
            }
            File presetFile = new File(Framework.SETTINGS_DIRECTORY_PATH, MpsatAssertionVerificationCommand.MPSAT_ASSERTION_PRESETS_FILE);
            MpsatPresetManager pmgr = new MpsatPresetManager(presetFile, new MpsatSettingsSerialiser(), true);
            MpsatAssertionDialog dialog = new MpsatAssertionDialog(mainWindow, pmgr);
            dialog.pack();
            GUI.centerToParent(dialog, mainWindow);
            dialog.setVisible(true);
            if (dialog.getModalResult() == 1) {
                final CustomCheckCircuitTask task = new CustomCheckCircuitTask(we, dialog.getSettings());
                String description = "MPSat tool chain";
                String title = we.getTitle();
                if (!title.isEmpty()) {
                    description += "(" + title + ")";
                }
                final TaskManager taskManager = framework.getTaskManager();
                final MpsatChainResultHandler monitor = new MpsatChainResultHandler(task);
                taskManager.queue(task, description, monitor);
            }
        }
    }

}
