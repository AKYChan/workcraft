package org.workcraft.plugins.mpsat.commands;

import java.io.File;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractVerificationCommand;
import org.workcraft.utils.ScriptableCommandUtils;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.mpsat.MpsatPresetManager;
import org.workcraft.plugins.mpsat.MpsatSettingsSerialiser;
import org.workcraft.plugins.mpsat.gui.PropertyDialog;
import org.workcraft.plugins.mpsat.tasks.VerificationChainResultHandler;
import org.workcraft.plugins.mpsat.tasks.VerificationChainTask;
import org.workcraft.plugins.mpsat.utils.MpsatUtils;
import org.workcraft.plugins.petri.PetriModel;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.tasks.TaskManager;
import org.workcraft.utils.GuiUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.utils.WorkspaceUtils;

public class PropertyVerificationCommand extends AbstractVerificationCommand {

    public static final String MPSAT_PROPERTY_PRESETS_FILE = "mpsat-property-presets.xml";

    @Override
    public String getDisplayName() {
        return "Custom property [MPSat]...";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, PetriModel.class);
    }

    @Override
    public Position getPosition() {
        return Position.BOTTOM;
    }

    @Override
    public void run(WorkspaceEntry we) {
        Framework framework = Framework.getInstance();
        MainWindow mainWindow = framework.getMainWindow();
        File presetFile = new File(Framework.SETTINGS_DIRECTORY_PATH, MPSAT_PROPERTY_PRESETS_FILE);
        boolean allowStgPresets = WorkspaceUtils.isApplicable(we, StgModel.class);
        MpsatPresetManager pmgr = new MpsatPresetManager(presetFile, new MpsatSettingsSerialiser(), allowStgPresets);
        PropertyDialog dialog = new PropertyDialog(mainWindow, pmgr);
        dialog.pack();
        GuiUtils.centerToParent(dialog, mainWindow);
        dialog.setVisible(true);
        if (dialog.getModalResult() == 1) {
            TaskManager manager = framework.getTaskManager();
            VerificationChainTask task = new VerificationChainTask(we, dialog.getSettings());
            String description = MpsatUtils.getToolchainDescription(we.getTitle());
            VerificationChainResultHandler monitor = new VerificationChainResultHandler(we);
            manager.queue(task, description, monitor);
        }
    }

    @Override
    public Boolean execute(WorkspaceEntry we) {
        ScriptableCommandUtils.showErrorRequiresGui(getClass().getSimpleName());
        return null;
    }

}
