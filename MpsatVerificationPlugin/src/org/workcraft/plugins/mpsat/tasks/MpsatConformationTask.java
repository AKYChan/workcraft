package org.workcraft.plugins.mpsat.tasks;

import org.workcraft.Framework;
import org.workcraft.PluginManager;
import org.workcraft.exceptions.NoExporterException;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.mpsat.MpsatMode;
import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.pcomp.ComponentData;
import org.workcraft.plugins.pcomp.CompositionData;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.plugins.pcomp.tasks.PcompTask;
import org.workcraft.plugins.pcomp.tasks.PcompTask.ConversionMode;
import org.workcraft.plugins.punf.tasks.PunfOutput;
import org.workcraft.plugins.punf.tasks.PunfTask;
import org.workcraft.plugins.shared.tasks.ExportOutput;
import org.workcraft.plugins.shared.tasks.ExportTask;
import org.workcraft.plugins.stg.Signal;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.interop.StgFormat;
import org.workcraft.plugins.stg.utils.StgUtils;
import org.workcraft.tasks.*;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.util.ExportUtils;
import org.workcraft.util.FileUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

import java.io.File;
import java.util.Set;

public class MpsatConformationTask implements Task<MpsatChainOutput> {

    private final MpsatParameters toolchainPreparationSettings = new MpsatParameters("Toolchain preparation of data",
            MpsatMode.UNDEFINED, 0, null, 0);

    private final WorkspaceEntry we;
    private final File envFile;

    public MpsatConformationTask(WorkspaceEntry we, File envFile) {
        this.we = we;
        this.envFile = envFile;
    }

    @Override
    public Result<? extends MpsatChainOutput> run(ProgressMonitor<? super MpsatChainOutput> monitor) {
        Framework framework = Framework.getInstance();
        PluginManager pluginManager = framework.getPluginManager();
        TaskManager taskManager = framework.getTaskManager();
        String prefix = FileUtils.getTempPrefix(we.getTitle());
        File directory = FileUtils.createTempDirectory(prefix);
        StgFormat format = StgFormat.getInstance();
        String stgFileExtension = format.getExtension();
        try {
            Stg devStg = WorkspaceUtils.getAs(we, Stg.class);
            Exporter devStgExporter = ExportUtils.chooseBestExporter(pluginManager, devStg, format);
            if (devStgExporter == null) {
                throw new NoExporterException(devStg, format);
            }
            SubtaskMonitor<Object> subtaskMonitor = new SubtaskMonitor<>(monitor);

            // Generating .g for the model
            File devStgFile = new File(directory, StgUtils.DEVICE_FILE_PREFIX + stgFileExtension);
            ExportTask devExportTask = new ExportTask(devStgExporter, devStg, devStgFile.getAbsolutePath());
            Result<? extends ExportOutput> devExportResult = taskManager.execute(
                    devExportTask, "Exporting circuit .g", subtaskMonitor);

            if (devExportResult.getOutcome() != Outcome.SUCCESS) {
                if (devExportResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<>(Outcome.CANCEL);
                }
                return new Result<>(Outcome.FAILURE,
                        new MpsatChainOutput(devExportResult, null, null, null, toolchainPreparationSettings));
            }
            monitor.progressUpdate(0.30);

            // Generating .g for the environment
            Stg envStg = StgUtils.loadStg(envFile);
            if (envStg == null) {
                return new Result<>(Outcome.FAILURE,
                        new MpsatChainOutput(null, null, null, null, toolchainPreparationSettings));
            }

            // Make sure that input signals of the device STG are also inputs in the environment STG
            Set<String> inputSignals = devStg.getSignalReferences(Signal.Type.INPUT);
            Set<String> outputSignals = devStg.getSignalReferences(Signal.Type.OUTPUT);
            StgUtils.restoreInterfaceSignals(envStg, inputSignals, outputSignals);
            Exporter envStgExporter = ExportUtils.chooseBestExporter(pluginManager, envStg, format);
            File envStgFile = new File(directory, StgUtils.ENVIRONMENT_FILE_PREFIX + stgFileExtension);
            ExportTask envExportTask = new ExportTask(envStgExporter, envStg, envStgFile.getAbsolutePath());
            Result<? extends ExportOutput> envExportResult = taskManager.execute(
                    envExportTask, "Exporting environment .g", subtaskMonitor);

            if (envExportResult.getOutcome() != Outcome.SUCCESS) {
                if (envExportResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<>(Outcome.CANCEL);
                }
                return new Result<>(Outcome.FAILURE,
                        new MpsatChainOutput(envExportResult, null, null, null, toolchainPreparationSettings));
            }
            monitor.progressUpdate(0.40);

            // Generating .g for the whole system (model and environment)
            File detailFile = new File(directory, StgUtils.DETAIL_FILE_PREFIX + StgUtils.XML_FILE_EXTENSION);
            File stgFile = new File(directory, StgUtils.SYSTEM_FILE_PREFIX + stgFileExtension);
            PcompTask pcompTask = new PcompTask(new File[]{devStgFile, envStgFile}, stgFile, detailFile,
                    ConversionMode.OUTPUT, true, false, directory);

            Result<? extends PcompOutput> pcompResult = taskManager.execute(
                    pcompTask, "Running parallel composition [PComp]", subtaskMonitor);

            if (pcompResult.getOutcome() != Outcome.SUCCESS) {
                if (pcompResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<>(Outcome.CANCEL);
                }
                return new Result<>(Outcome.FAILURE,
                        new MpsatChainOutput(devExportResult, pcompResult, null, null, toolchainPreparationSettings));
            }
            monitor.progressUpdate(0.50);

            // Generate unfolding
            File unfoldingFile = new File(directory, StgUtils.SYSTEM_FILE_PREFIX + PunfTask.PNML_FILE_EXTENSION);
            PunfTask punfTask = new PunfTask(stgFile, unfoldingFile, directory);
            Result<? extends PunfOutput> punfResult = taskManager.execute(
                    punfTask, "Unfolding .g", subtaskMonitor);

            if (punfResult.getOutcome() != Outcome.SUCCESS) {
                if (punfResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<>(Outcome.CANCEL);
                }
                return new Result<>(Outcome.FAILURE,
                        new MpsatChainOutput(devExportResult, pcompResult, punfResult, null, toolchainPreparationSettings));
            }
            monitor.progressUpdate(0.60);

            // Check for conformation
            CompositionData compositionData = new CompositionData(detailFile);
            ComponentData devComponentData = compositionData.getComponentData(devStgFile);
            Set<String> devPlaceNames = devComponentData.getDstPlaces();
            MpsatParameters mpsatSettings = MpsatParameters.getConformationSettings(devPlaceNames);
            MpsatTask mpsatTask = new MpsatTask(mpsatSettings.getMpsatArguments(directory),
                    unfoldingFile, directory, stgFile);
            Result<? extends MpsatOutput>  mpsatResult = taskManager.execute(
                    mpsatTask, "Running conformation check [MPSat]", subtaskMonitor);

            if (mpsatResult.getOutcome() != Outcome.SUCCESS) {
                if (mpsatResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<>(Outcome.CANCEL);
                }
                return new Result<>(Outcome.FAILURE,
                        new MpsatChainOutput(devExportResult, pcompResult, punfResult, mpsatResult, mpsatSettings));
            }
            monitor.progressUpdate(0.80);

            MpsatOutputParser mpsatConformationParser = new MpsatOutputParser(mpsatResult.getPayload());
            if (!mpsatConformationParser.getSolutions().isEmpty()) {
                return new Result<>(Outcome.SUCCESS,
                        new MpsatChainOutput(devExportResult, pcompResult, punfResult, mpsatResult, mpsatSettings,
                                "This model does not conform to the environment."));
            }
            monitor.progressUpdate(1.0);

            // Success
            String message = "The model conforms to its environment (" + envFile.getName() + ").";
            return new Result<>(Outcome.SUCCESS,
                    new MpsatChainOutput(devExportResult, pcompResult, punfResult, mpsatResult, mpsatSettings, message));

        } catch (Throwable e) {
            return new Result<>(e);
        } finally {
            FileUtils.deleteOnExitRecursively(directory);
        }
    }

}
