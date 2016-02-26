package org.workcraft.plugins.mpsat.tasks;

import java.io.File;

import org.workcraft.Framework;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.mpsat.MpsatSynthesisSettings;
import org.workcraft.plugins.punf.PunfUtilitySettings;
import org.workcraft.plugins.punf.tasks.PunfTask;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.plugins.stg.STG;
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

public class MpsatSynthesisChainTask implements Task<MpsatSynthesisChainResult> {
    private final WorkspaceEntry we;
    private final MpsatSynthesisSettings settings;

    public MpsatSynthesisChainTask(WorkspaceEntry we, MpsatSynthesisSettings settings) {
        this.we = we;
        this.settings = settings;
    }

    @Override
    public Result<? extends MpsatSynthesisChainResult> run(ProgressMonitor<? super MpsatSynthesisChainResult> monitor) {
        Framework framework = Framework.getInstance();
        String prefix = FileUtils.getTempPrefix(we.getTitle());
        File directory = FileUtils.createTempDirectory(prefix);
        try {
            STG model = WorkspaceUtils.getAs(we, STG.class);
            Exporter exporter = Export.chooseBestExporter(framework.getPluginManager(), model, Format.STG);
            if (exporter == null) {
                throw new RuntimeException("Exporter not available: model class " + model.getClass().getName() + " to format STG.");
            }
            SubtaskMonitor<Object> subtaskMonitor = new SubtaskMonitor<>(monitor);

            // Generate .g for the model
            File netFile = new File(directory, "net" + exporter.getExtenstion());
            ExportTask exportTask = new ExportTask(exporter, model, netFile.getAbsolutePath());
            Result<? extends Object> exportResult = framework.getTaskManager().execute(
                    exportTask, "Exporting .g", subtaskMonitor);

            if (exportResult.getOutcome() != Outcome.FINISHED) {
                if (exportResult.getOutcome() == Outcome.CANCELLED) {
                    return new Result<MpsatSynthesisChainResult>(Outcome.CANCELLED);
                }
                return new Result<MpsatSynthesisChainResult>(Outcome.FAILED,
                        new MpsatSynthesisChainResult(exportResult, null, null, null, settings));
            }
            monitor.progressUpdate(0.33);

            // Generate unfolding
            boolean tryPnml = settings.getMode().canPnml();
            File unfoldingFile = new File(directory, "unfolding" + PunfUtilitySettings.getUnfoldingExtension(tryPnml));
            PunfTask punfTask = new PunfTask(netFile.getAbsolutePath(), unfoldingFile.getAbsolutePath());
            Result<? extends ExternalProcessResult> punfResult = framework.getTaskManager().execute(punfTask, "Unfolding .g", subtaskMonitor);

            if (punfResult.getOutcome() != Outcome.FINISHED) {
                if (punfResult.getOutcome() == Outcome.CANCELLED) {
                    return new Result<MpsatSynthesisChainResult>(Outcome.CANCELLED);
                }
                return new Result<MpsatSynthesisChainResult>(Outcome.FAILED,
                        new MpsatSynthesisChainResult(exportResult, null, punfResult, null, settings));
            }
            monitor.progressUpdate(0.66);

            // Run MPSat on the generated unfolding
            boolean needLib = settings.getMode().needLib();
            MpsatSynthesisTask mpsatTask = new MpsatSynthesisTask(settings.getMpsatArguments(directory),
                    unfoldingFile.getAbsolutePath(), directory, tryPnml, needLib);
            Result<? extends ExternalProcessResult> mpsatResult = framework.getTaskManager().execute(
                    mpsatTask, "Running synthesis [MPSat]", subtaskMonitor);

            if (mpsatResult.getOutcome() != Outcome.FINISHED) {
                if (mpsatResult.getOutcome() == Outcome.CANCELLED) {
                    return new Result<MpsatSynthesisChainResult>(Outcome.CANCELLED);
                }
                return new Result<MpsatSynthesisChainResult>(Outcome.FAILED,
                        new MpsatSynthesisChainResult(exportResult, null, punfResult, mpsatResult, settings));
            }
            monitor.progressUpdate(1.0);

            return new Result<MpsatSynthesisChainResult>(Outcome.FINISHED,
                    new MpsatSynthesisChainResult(exportResult, null, punfResult, mpsatResult, settings));
        } catch (Throwable e) {
            return new Result<MpsatSynthesisChainResult>(e);
        } finally {
            FileUtils.deleteOnExitRecursively(directory);
        }
    }

    public MpsatSynthesisSettings getSettings() {
        return settings;
    }

    public WorkspaceEntry getWorkspaceEntry() {
        return we;
    }

}
