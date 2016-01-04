package org.workcraft.plugins.mpsat.tools;

import org.workcraft.Tool;
import org.workcraft.plugins.mpsat.MpsatSettings;
import org.workcraft.plugins.stg.STGModel;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatCscChecker extends AbstractMpsatChecker implements Tool {

	@Override
	public String getDisplayName() {
		return "  Complete State Coding (all cores) [MPSat]";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return WorkspaceUtils.canHas(we, STGModel.class);
	}

	@Override
	public MpsatSettings getSettings() {
		return MpsatSettings.getCscSettings();
	}

}
