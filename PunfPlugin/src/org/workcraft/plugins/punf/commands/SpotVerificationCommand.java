package org.workcraft.plugins.punf.commands;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractVerificationCommand;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.punf.tasks.SpotChainResultHandler;
import org.workcraft.plugins.punf.tasks.SpotChainTask;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.presets.PresetManager;
import org.workcraft.presets.TextDataSerialiser;
import org.workcraft.presets.TextPresetDialog;
import org.workcraft.tasks.TaskManager;
import org.workcraft.utils.ScriptableCommandUtils;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.io.File;

public class SpotVerificationCommand extends AbstractVerificationCommand {

    private static final File PRESET_FILE = new File(Framework.SETTINGS_DIRECTORY_PATH, "spot-presets.xml");
    private static final TextDataSerialiser DATA_SERIALISER = new TextDataSerialiser();

    private static String preservedData = null;

    @Override
    public String getDisplayName() {
        return "Custom Spot property [Spot]...";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Stg.class);
    }

    @Override
    public Position getPosition() {
        return Position.BOTTOM;
    }

    @Override
    public Boolean execute(WorkspaceEntry we) {
        ScriptableCommandUtils.showErrorRequiresGui(getClass().getSimpleName());
        return null;
    }

    @Override
    public void run(WorkspaceEntry we) {
        Framework framework = Framework.getInstance();
        MainWindow mainWindow = framework.getMainWindow();
        PresetManager<String> pmgr = new PresetManager<>(PRESET_FILE, DATA_SERIALISER, preservedData);
        TextPresetDialog dialog = new TextPresetDialog(mainWindow, "Custom Spot assertion", pmgr, null);
        if (dialog.reveal()) {
            preservedData = dialog.getData();
            TaskManager manager = framework.getTaskManager();
            SpotChainTask task = new SpotChainTask(we, preservedData);
            SpotChainResultHandler monitor = new SpotChainResultHandler(we);
            manager.queue(task, "Running Spot assertion [Spot]", monitor);
        }
    }

}
