package org.workcraft.plugins.mpsat.tools;

import java.io.File;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.mpsat.MpsatChainResultHandler;
import org.workcraft.plugins.mpsat.MpsatSettings;
import org.workcraft.plugins.mpsat.MpsatSettingsSerialiser;
import org.workcraft.plugins.mpsat.gui.MpsatConfigurationDialog;
import org.workcraft.plugins.mpsat.tasks.MpsatChainTask;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.shared.presets.PresetManager;
import org.workcraft.util.GUI;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatCustomPropertyChecker implements Tool {

	@Override
	public String getSection() {
		return "!Verification";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return WorkspaceUtils.canHas(we, PetriNetModel.class);
	}

	@Override
	public void run(WorkspaceEntry we) {
		PresetManager<MpsatSettings> pmgr = new PresetManager<MpsatSettings>(new File("config/mpsat_presets.xml"), new MpsatSettingsSerialiser());
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

	@Override
	public String getDisplayName() {
		return "Custom properties [MPSat]...";
	}

}