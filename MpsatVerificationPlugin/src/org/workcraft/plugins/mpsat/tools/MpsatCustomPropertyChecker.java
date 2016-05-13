package org.workcraft.plugins.mpsat.tools;

import java.io.File;

import org.workcraft.Framework;
import org.workcraft.VerificationTool;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.mpsat.MpsatChainResultHandler;
import org.workcraft.plugins.mpsat.MpsatPresetManager;
import org.workcraft.plugins.mpsat.MpsatSettingsSerialiser;
import org.workcraft.plugins.mpsat.gui.MpsatConfigurationDialog;
import org.workcraft.plugins.mpsat.tasks.MpsatChainTask;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.util.GUI;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatCustomPropertyChecker extends VerificationTool {

    private static final String MPSAT_PRESETS_FILE = "mpsat_presets.xml";

    @Override
    public String getDisplayName() {
        return "Custom properties [MPSat]...";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.canHas(we, PetriNetModel.class);
    }

    @Override
    public Position getPosition() {
        return Position.BOTTOM;
    }

    @Override
    public void run(WorkspaceEntry we) {
        File presetFile = new File(Framework.SETTINGS_DIRECTORY_PATH, MPSAT_PRESETS_FILE);
        boolean allowStgPresets = WorkspaceUtils.canHas(we, StgModel.class);
        MpsatPresetManager pmgr = new MpsatPresetManager(presetFile, new MpsatSettingsSerialiser(), allowStgPresets);
        final Framework framework = Framework.getInstance();
        MainWindow mainWindow = framework.getMainWindow();
        MpsatConfigurationDialog dialog = new MpsatConfigurationDialog(mainWindow, pmgr);
        GUI.centerToParent(dialog, mainWindow);
        dialog.setVisible(true);
        if (dialog.getModalResult() == 1) {
            final MpsatChainTask mpsatTask = new MpsatChainTask(we, dialog.getSettings());
            framework.getTaskManager().queue(mpsatTask, "MPSat tool chain",
                    new MpsatChainResultHandler(mpsatTask));
        }
    }

}