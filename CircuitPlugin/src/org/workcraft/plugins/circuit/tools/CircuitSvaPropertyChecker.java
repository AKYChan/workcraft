package org.workcraft.plugins.circuit.tools;

import java.io.File;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.tasks.CustomCheckCircuitTask;
import org.workcraft.plugins.mpsat.MpsatChainResultHandler;
import org.workcraft.plugins.mpsat.MpsatPresetManager;
import org.workcraft.plugins.mpsat.MpsatSettingsSerialiser;
import org.workcraft.plugins.mpsat.gui.MpsatSvaPropertyDialog;
import org.workcraft.plugins.mpsat.tools.MpsatSvaPropertyChecker;
import org.workcraft.util.GUI;
import org.workcraft.workspace.WorkspaceEntry;

public class CircuitSvaPropertyChecker extends CircuitPropertyChecker {

    private static final String TITLE_VERIFICATION = "Circuit verification";

    @Override
    public String getDisplayName() {
        return "Custom SVA properties [MPSat]...";
    }

    @Override
    public void run(WorkspaceEntry we) {
        final Framework framework = Framework.getInstance();
        MainWindow mainWindow = framework.getMainWindow();

        Circuit circuit = (Circuit) we.getModelEntry().getMathModel();
        if (circuit.getFunctionComponents().isEmpty()) {
            JOptionPane.showMessageDialog(mainWindow, "Error: the circuit must have components.",
                    TITLE_VERIFICATION, JOptionPane.ERROR_MESSAGE);
            return;
        }

        VisualCircuit visualCircuit = (VisualCircuit) we.getModelEntry().getVisualModel();
        File envFile = visualCircuit.getEnvironmentFile();
        if ((envFile == null) || !envFile.exists()) {
            JOptionPane.showMessageDialog(mainWindow,
                    "Warning: the circuit will be verified without environment STG.",
                    TITLE_VERIFICATION, JOptionPane.WARNING_MESSAGE);
        }

        File presetFile = new File(Framework.SETTINGS_DIRECTORY_PATH, MpsatSvaPropertyChecker.MPSAT_SVA_PRESETS_FILE);
        MpsatPresetManager pmgr = new MpsatPresetManager(presetFile, new MpsatSettingsSerialiser(), true);
        MpsatSvaPropertyDialog dialog = new MpsatSvaPropertyDialog(mainWindow, pmgr);
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
            framework.getTaskManager().queue(task, description, new MpsatChainResultHandler(task));
        }
    }

}
