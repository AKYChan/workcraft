package org.workcraft.plugins.mpsat.tasks;

import java.io.File;
import java.util.Collection;

import org.workcraft.Framework;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.mpsat.MpsatSynthesisParameters;
import org.workcraft.plugins.punf.tasks.PunfTask;
import org.workcraft.plugins.shared.tasks.ExternalProcessOutput;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.SignalTransition.Type;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgUtils;
import org.workcraft.plugins.stg.interop.StgFormat;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.tasks.SubtaskMonitor;
import org.workcraft.tasks.Task;
import org.workcraft.util.Export;
import org.workcraft.util.Export.ExportTask;
import org.workcraft.util.FileUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class MpsatSynthesisChainTask implements Task<MpsatSynthesisChainResult> {
    private final WorkspaceEntry we;
    private final MpsatSynthesisParameters settings;
    private final Collection<Mutex> mutexes;

    public MpsatSynthesisChainTask(WorkspaceEntry we, MpsatSynthesisParameters settings, Collection<Mutex> mutexes) {
        this.we = we;
        this.settings = settings;
        this.mutexes = mutexes;
    }

    @Override
    public Result<? extends MpsatSynthesisChainResult> run(ProgressMonitor<? super MpsatSynthesisChainResult> monitor) {
        Framework framework = Framework.getInstance();
        String prefix = FileUtils.getTempPrefix(we.getTitle());
        File directory = FileUtils.createTempDirectory(prefix);
        String stgFileExtension = StgFormat.getInstance().getExtension();
        try {
            Stg model = WorkspaceUtils.getAs(we, Stg.class);
            Exporter exporter = Export.chooseBestExporter(framework.getPluginManager(), model, StgFormat.getInstance());
            if (exporter == null) {
                throw new RuntimeException("Exporter not available: model class " + model.getClass().getName() + " to format STG.");
            }
            SubtaskMonitor<Object> subtaskMonitor = new SubtaskMonitor<>(monitor);

            // Generate .g for the model
            String filePrefix = StgUtils.SPEC_FILE_PREFIX;
            File netFile = new File(directory, filePrefix + stgFileExtension);
            ExportTask exportTask = new ExportTask(exporter, model, netFile.getAbsolutePath());
            Result<? extends Object> exportResult = framework.getTaskManager().execute(
                    exportTask, "Exporting .g", subtaskMonitor);

            if (exportResult.getOutcome() != Outcome.SUCCESS) {
                if (exportResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<MpsatSynthesisChainResult>(Outcome.CANCEL);
                }
                return new Result<MpsatSynthesisChainResult>(Outcome.FAILURE,
                        new MpsatSynthesisChainResult(exportResult, null, null, null, settings));
            }
            if (!mutexes.isEmpty()) {
                model = StgUtils.loadStg(netFile);
                for (Mutex m: mutexes) {
                    model.setSignalType(m.g1.name, Type.INPUT);
                    model.setSignalType(m.g2.name, Type.INPUT);
                }
                filePrefix += StgUtils.MUTEX_FILE_SUFFIX;
                netFile = new File(directory, filePrefix + stgFileExtension);
                exportTask = new ExportTask(exporter, model, netFile.getAbsolutePath());
                exportResult = framework.getTaskManager().execute(exportTask, "Exporting .g");

                if (exportResult.getOutcome() != Outcome.SUCCESS) {
                    if (exportResult.getOutcome() == Outcome.CANCEL) {
                        return new Result<MpsatSynthesisChainResult>(Outcome.CANCEL);
                    }
                    return new Result<MpsatSynthesisChainResult>(Outcome.FAILURE,
                            new MpsatSynthesisChainResult(exportResult, null, null, null, settings));
                }
            }
            monitor.progressUpdate(0.33);

            // Generate unfolding
            File unfoldingFile = new File(directory, filePrefix + PunfTask.PNML_FILE_EXTENSION);
            PunfTask punfTask = new PunfTask(netFile.getAbsolutePath(), unfoldingFile.getAbsolutePath());
            Result<? extends ExternalProcessOutput> punfResult = framework.getTaskManager().execute(punfTask, "Unfolding .g", subtaskMonitor);

            if (punfResult.getOutcome() != Outcome.SUCCESS) {
                if (punfResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<MpsatSynthesisChainResult>(Outcome.CANCEL);
                }
                return new Result<MpsatSynthesisChainResult>(Outcome.FAILURE,
                        new MpsatSynthesisChainResult(exportResult, null, punfResult, null, settings));
            }
            monitor.progressUpdate(0.66);

            // Run MPSat on the generated unfolding
            boolean needLib = settings.getMode().needLib();
            MpsatSynthesisTask mpsatTask = new MpsatSynthesisTask(settings.getMpsatArguments(directory),
                    unfoldingFile.getAbsolutePath(), directory, needLib);
            Result<? extends ExternalProcessOutput> mpsatResult = framework.getTaskManager().execute(
                    mpsatTask, "Running synthesis [MPSat]", subtaskMonitor);

            if (mpsatResult.getOutcome() != Outcome.SUCCESS) {
                if (mpsatResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<MpsatSynthesisChainResult>(Outcome.CANCEL);
                }
                return new Result<MpsatSynthesisChainResult>(Outcome.FAILURE,
                        new MpsatSynthesisChainResult(exportResult, null, punfResult, mpsatResult, settings));
            }
            monitor.progressUpdate(1.0);

            return new Result<MpsatSynthesisChainResult>(Outcome.SUCCESS,
                    new MpsatSynthesisChainResult(exportResult, null, punfResult, mpsatResult, settings));
        } catch (Throwable e) {
            return new Result<MpsatSynthesisChainResult>(e);
        } finally {
            FileUtils.deleteOnExitRecursively(directory);
        }
    }

    public MpsatSynthesisParameters getSettings() {
        return settings;
    }

    public WorkspaceEntry getWorkspaceEntry() {
        return we;
    }

}
