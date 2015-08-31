package org.workcraft.plugins.mpsat.tasks;

import java.io.File;

import org.workcraft.Framework;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.mpsat.MpsatSettings;
import org.workcraft.plugins.mpsat.MpsatUtilitySettings;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.shared.CommonDebugSettings;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.serialisation.Format;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.tasks.SubtaskMonitor;
import org.workcraft.tasks.Task;
import org.workcraft.util.Export;
import org.workcraft.util.Export.ExportTask;
import org.workcraft.util.FileUtils;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatChainTask implements Task<MpsatChainResult> {
	private final WorkspaceEntry we;
	private final MpsatSettings settings;

	public MpsatChainTask(WorkspaceEntry we, MpsatSettings settings) {
		this.we = we;
		this.settings = settings;
	}

	@Override
	public Result<? extends MpsatChainResult> run(ProgressMonitor<? super MpsatChainResult> monitor) {
		File netFile = null;
		File unfoldingFile = null;
		try {
			PetriNetModel model = WorkspaceUtils.getAs(we, PetriNetModel.class);
			Framework framework = Framework.getInstance();
			Exporter exporter = Export.chooseBestExporter(framework.getPluginManager(), model, Format.STG);
			if (exporter == null) {
				throw new RuntimeException ("Exporter not available: model class " + model.getClass().getName() + " to format STG.");
			}
			netFile = File.createTempFile("net", exporter.getExtenstion());
			ExportTask exportTask;
			exportTask = new ExportTask(exporter, model, netFile.getCanonicalPath());
			SubtaskMonitor<Object> mon = new SubtaskMonitor<Object>(monitor);
			Result<? extends Object> exportResult = framework.getTaskManager().execute(exportTask, "Exporting .g", mon);
			if (exportResult.getOutcome() != Outcome.FINISHED) {
				if (exportResult.getOutcome() == Outcome.CANCELLED) {
					return new Result<MpsatChainResult>(Outcome.CANCELLED);
				}
				return new Result<MpsatChainResult>(Outcome.FAILED, new MpsatChainResult(exportResult, null, null, null, settings));
			}
			monitor.progressUpdate(0.33);

			boolean tryPnml = settings.getMode().canPnml();
			unfoldingFile = File.createTempFile("unfolding", MpsatUtilitySettings.getUnfoldingExtension(tryPnml));
			PunfTask punfTask = new PunfTask(netFile.getCanonicalPath(), unfoldingFile.getCanonicalPath(), tryPnml);
			Result<? extends ExternalProcessResult> punfResult = framework.getTaskManager().execute(punfTask, "Unfolding .g", mon);

			if (punfResult.getOutcome() != Outcome.FINISHED) {
				if (punfResult.getOutcome() == Outcome.CANCELLED) {
					return new Result<MpsatChainResult>(Outcome.CANCELLED);
				}
				return new Result<MpsatChainResult>(Outcome.FAILED, new MpsatChainResult(exportResult, null, punfResult, null, settings));
			}

			monitor.progressUpdate(0.66);

			MpsatTask mpsatTask = new MpsatTask(settings.getMpsatArguments(), unfoldingFile.getCanonicalPath(), null, tryPnml);
			Result<? extends ExternalProcessResult> mpsatResult = framework.getTaskManager().execute(mpsatTask, "Running mpsat model-checking", mon);

			if (mpsatResult.getOutcome() != Outcome.FINISHED) {
				if (mpsatResult.getOutcome() == Outcome.CANCELLED) {
					return new Result<MpsatChainResult>(Outcome.CANCELLED);
				}
				return new Result<MpsatChainResult>(Outcome.FAILED, new MpsatChainResult(exportResult, null, punfResult, mpsatResult, settings ));
			}

			monitor.progressUpdate(1.0);

			return new Result<MpsatChainResult>(Outcome.FINISHED, new MpsatChainResult(exportResult, null, punfResult, mpsatResult, settings));
		} catch (Throwable e) {
			return new Result<MpsatChainResult>(e);
		}
		// Clean up
		finally {
			FileUtils.deleteFile(netFile, CommonDebugSettings.getKeepTemporaryFiles());
			FileUtils.deleteFile(unfoldingFile, CommonDebugSettings.getKeepTemporaryFiles());
		}
	}

	public MpsatSettings getSettings() {
		return settings;
	}

	public WorkspaceEntry getWorkspaceEntry() {
		return we;
	}

}
