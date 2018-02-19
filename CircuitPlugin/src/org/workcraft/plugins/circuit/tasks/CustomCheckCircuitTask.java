package org.workcraft.plugins.circuit.tasks;

import java.io.File;
import java.util.Set;

import org.workcraft.Framework;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.stg.CircuitStgUtils;
import org.workcraft.plugins.circuit.stg.CircuitToStgConverter;
import org.workcraft.plugins.mpsat.MpsatMode;
import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.mpsat.tasks.MpsatChainOutput;
import org.workcraft.plugins.mpsat.tasks.MpsatChainTask;
import org.workcraft.plugins.mpsat.tasks.MpsatOutoutParser;
import org.workcraft.plugins.mpsat.tasks.MpsatOutput;
import org.workcraft.plugins.mpsat.tasks.MpsatTask;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.plugins.punf.tasks.PunfOutput;
import org.workcraft.plugins.punf.tasks.PunfTask;
import org.workcraft.plugins.shared.tasks.ExportOutput;
import org.workcraft.plugins.stg.SignalTransition.Type;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgUtils;
import org.workcraft.plugins.stg.interop.StgFormat;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.tasks.SubtaskMonitor;
import org.workcraft.tasks.TaskManager;
import org.workcraft.util.FileUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class CustomCheckCircuitTask extends MpsatChainTask {

    private final MpsatParameters toolchainPreparationSettings = new MpsatParameters("Toolchain preparation of data",
            MpsatMode.UNDEFINED, 0, null, 0);

    private final MpsatParameters toolchainCompletionSettings = new MpsatParameters("Toolchain completion",
            MpsatMode.UNDEFINED, 0, null, 0);

    public CustomCheckCircuitTask(WorkspaceEntry we, MpsatParameters settings) {
        super(we, settings);
    }

    @Override
    public Result<? extends MpsatChainOutput> run(ProgressMonitor<? super MpsatChainOutput> monitor) {
        Framework framework = Framework.getInstance();
        TaskManager manager = framework.getTaskManager();
        WorkspaceEntry we = getWorkspaceEntry();
        String prefix = FileUtils.getTempPrefix(we.getTitle());
        File directory = FileUtils.createTempDirectory(prefix);
        String stgFileExtension = StgFormat.getInstance().getExtension();
        try {
            // Common variables
            VisualCircuit visualCircuit = WorkspaceUtils.getAs(we, VisualCircuit.class);
            File envFile = visualCircuit.getEnvironmentFile();

            // Load device STG
            CircuitToStgConverter converter = new CircuitToStgConverter(visualCircuit);
            Stg devStg = (Stg) converter.getStg().getMathModel();

            // Load environment STG
            Stg envStg = StgUtils.loadStg(envFile);
            if (envStg != null) {
                // Make sure that input signals of the device STG are also inputs in the environment STG
                Set<String> inputSignalNames = devStg.getSignalNames(Type.INPUT, null);
                Set<String> outputSignalNames = devStg.getSignalNames(Type.OUTPUT, null);
                StgUtils.restoreInterfaceSignals(envStg, inputSignalNames, outputSignalNames);
            }

            // Write device STG into a .g file
            String devStgName = (envStg != null ? StgUtils.DEVICE_FILE_PREFIX : StgUtils.SYSTEM_FILE_PREFIX) + stgFileExtension;
            File devStgFile = new File(directory, devStgName);
            Result<? extends ExportOutput> devExportResult = CircuitStgUtils.exportStg(devStg, devStgFile, directory, monitor);
            if (devExportResult.getOutcome() != Outcome.SUCCESS) {
                if (devExportResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<MpsatChainOutput>(Outcome.CANCEL);
                }
                return new Result<MpsatChainOutput>(Outcome.FAILURE,
                        new MpsatChainOutput(devExportResult, null, null, null, toolchainPreparationSettings));
            }
            monitor.progressUpdate(0.10);

            // Generating system .g for custom property check (only if needed)
            File sysStgFile = null;
            File detailsFile = null;
            Result<? extends PcompOutput>  pcompResult = null;
            if (envStg == null) {
                sysStgFile = devStgFile;
            } else {
                File envStgFile = new File(directory, StgUtils.ENVIRONMENT_FILE_PREFIX + stgFileExtension);
                Result<? extends ExportOutput> envExportResult = CircuitStgUtils.exportStg(envStg, envStgFile, directory, monitor);
                if (envExportResult.getOutcome() != Outcome.SUCCESS) {
                    if (envExportResult.getOutcome() == Outcome.CANCEL) {
                        return new Result<MpsatChainOutput>(Outcome.CANCEL);
                    }
                    return new Result<MpsatChainOutput>(Outcome.FAILURE,
                            new MpsatChainOutput(envExportResult, null, null, null, toolchainPreparationSettings));
                }

                // Generating .g for the whole system (circuit and environment)
                sysStgFile = new File(directory, StgUtils.SYSTEM_FILE_PREFIX + stgFileExtension);
                detailsFile = new File(directory, StgUtils.DETAILS_FILE_PREFIX + StgUtils.XML_FILE_EXTENSION);
                pcompResult = CircuitStgUtils.composeDevWithEnv(devStgFile, envStgFile, sysStgFile, detailsFile, directory, monitor);
                if (pcompResult.getOutcome() != Outcome.SUCCESS) {
                    if (pcompResult.getOutcome() == Outcome.CANCEL) {
                        return new Result<MpsatChainOutput>(Outcome.CANCEL);
                    }
                    return new Result<MpsatChainOutput>(Outcome.FAILURE,
                            new MpsatChainOutput(devExportResult, pcompResult, null, null, toolchainPreparationSettings));
                }
            }
            monitor.progressUpdate(0.20);

            // Generate unfolding (only if needed)
            File unfoldingFile = new File(directory, StgUtils.SYSTEM_FILE_PREFIX + PunfTask.PNML_FILE_EXTENSION);
            PunfTask punfTask = new PunfTask(sysStgFile.getAbsolutePath(), unfoldingFile.getAbsolutePath());
            SubtaskMonitor<Object> punfMonitor = new SubtaskMonitor<>(monitor);
            Result<? extends PunfOutput> punfResult = manager.execute(punfTask, "Unfolding .g", punfMonitor);

            if (punfResult.getOutcome() != Outcome.SUCCESS) {
                if (punfResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<MpsatChainOutput>(Outcome.CANCEL);
                }
                return new Result<MpsatChainOutput>(Outcome.FAILURE,
                        new MpsatChainOutput(devExportResult, pcompResult, punfResult, null, toolchainPreparationSettings));
            }
            monitor.progressUpdate(0.40);

            // Check custom property (if requested)
            MpsatParameters settings = getSettings();
            MpsatTask mpsatTask = new MpsatTask(settings.getMpsatArguments(directory), unfoldingFile, directory);
            SubtaskMonitor<Object> mpsatMonitor = new SubtaskMonitor<>(monitor);
            Result<? extends MpsatOutput> mpsatResult = manager.execute(
                    mpsatTask, "Running custom property check [MPSat]", mpsatMonitor);

            if (mpsatResult.getOutcome() != Outcome.SUCCESS) {
                if (mpsatResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<MpsatChainOutput>(Outcome.CANCEL);
                }
                return new Result<MpsatChainOutput>(Outcome.FAILURE,
                        new MpsatChainOutput(devExportResult, pcompResult, punfResult, mpsatResult, settings));
            }
            monitor.progressUpdate(0.50);

            MpsatOutoutParser mpsatParser = new MpsatOutoutParser(mpsatResult.getPayload());
            if (!mpsatParser.getSolutions().isEmpty()) {
                return new Result<MpsatChainOutput>(Outcome.SUCCESS,
                        new MpsatChainOutput(devExportResult, pcompResult, punfResult, mpsatResult, settings,
                                "Custom property is violated after the following trace(s):"));
            }
            monitor.progressUpdate(1.00);

            // Success
            return new Result<MpsatChainOutput>(Outcome.SUCCESS,
                    new MpsatChainOutput(devExportResult, pcompResult, punfResult, mpsatResult, toolchainCompletionSettings,
                            "Custom property holds"));

        } catch (Throwable e) {
            return new Result<MpsatChainOutput>(e);
        } finally {
            FileUtils.deleteOnExitRecursively(directory);
        }
    }

}
