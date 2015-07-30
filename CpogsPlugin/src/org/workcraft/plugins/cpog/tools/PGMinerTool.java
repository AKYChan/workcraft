package org.workcraft.plugins.cpog.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.plugins.cpog.CpogSettings;
import org.workcraft.plugins.cpog.VisualCPOG;
import org.workcraft.plugins.cpog.tasks.PGMinerResultHandler;
import org.workcraft.plugins.cpog.tasks.PGMinerTask;
import org.workcraft.plugins.stg.STGModel;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public abstract class PGMinerTool implements Tool {

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		if (we.getModelEntry() == null) return false;
		if (we.getModelEntry().getVisualModel() instanceof VisualCPOG) return true;
		return false;
	}

	@Override
	public String getSection() {
		return "!Process Mining";
	}

	abstract public File getInputFile(WorkspaceEntry we);


	@Override
	public void run(WorkspaceEntry we) {

		File inputFile = getInputFile(we);
		PGMinerTask task = new PGMinerTask(inputFile);

		final Framework framework = Framework.getInstance();
		PGMinerResultHandler result = new PGMinerResultHandler((VisualCPOG) we.getModelEntry().getVisualModel());
		framework.getTaskManager().queue(task, "PGMiner", result);

	}


}
