package org.workcraft.plugins.circuit.tasks;

import org.workcraft.Framework;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.stg.CircuitStgUtils;
import org.workcraft.plugins.circuit.stg.CircuitToStgConverter;
import org.workcraft.plugins.mpsat_verification.tasks.SpotChainOutput;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.plugins.punf.tasks.Ltl2tgbaOutput;
import org.workcraft.plugins.punf.tasks.Ltl2tgbaTask;
import org.workcraft.plugins.punf.tasks.PunfLtlxTask;
import org.workcraft.plugins.punf.tasks.PunfOutput;
import org.workcraft.plugins.stg.Signal;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.interop.StgFormat;
import org.workcraft.plugins.stg.serialisation.SerialiserUtils;
import org.workcraft.plugins.stg.utils.StgUtils;
import org.workcraft.tasks.*;
import org.workcraft.utils.FileUtils;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

public class SpotChainTask implements Task<SpotChainOutput> {

    private final WorkspaceEntry we;
    private final String data;

    public SpotChainTask(WorkspaceEntry we, String data) {
        this.we = we;
        this.data = data;
    }

    @Override
    public Result<? extends SpotChainOutput> run(ProgressMonitor<? super SpotChainOutput> monitor) {
        Framework framework = Framework.getInstance();
        TaskManager manager = framework.getTaskManager();
        String prefix = FileUtils.getTempPrefix(we.getTitle());
        File directory = FileUtils.createTempDirectory(prefix);
        String stgFileExtension = StgFormat.getInstance().getExtension();

        try {
            // Convert SPOT assertion to Buechi automaton
            File spotFile = new File(directory, "assertion.spot");
            spotFile.deleteOnExit();
            try {
                FileUtils.dumpString(spotFile, data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Ltl2tgbaTask ltl2tgbaTask = new Ltl2tgbaTask(spotFile, directory);
            SubtaskMonitor<Object> ltl2tgbaMonitor = new SubtaskMonitor<>(monitor);
            Result<? extends Ltl2tgbaOutput> ltl2tgbaResult = manager.execute(
                    ltl2tgbaTask, "Converting SPOT assertion to B\u00FCchi automaton", ltl2tgbaMonitor);

            if (ltl2tgbaResult.getOutcome() != Result.Outcome.SUCCESS) {
                if (ltl2tgbaResult.getOutcome() == Result.Outcome.CANCEL) {
                    return new Result<>(Result.Outcome.CANCEL);
                }
                return new Result<>(Result.Outcome.FAILURE,
                        new SpotChainOutput(ltl2tgbaResult, null, null, null));
            }
            monitor.progressUpdate(0.1);

            // Common variables
            VisualCircuit circuit = WorkspaceUtils.getAs(we, VisualCircuit.class);
            File envFile = circuit.getMathModel().getEnvironmentFile();

            // Load device STG
            CircuitToStgConverter converter = new CircuitToStgConverter(circuit);
            Stg devStg = converter.getStg().getMathModel();

            // Load environment STG
            Stg envStg = StgUtils.loadStg(envFile);
            if (envStg != null) {
                // Make sure that input signals of the device STG are also inputs in the environment STG
                Set<String> inputSignalNames = devStg.getSignalNames(Signal.Type.INPUT, null);
                Set<String> outputSignalNames = devStg.getSignalNames(Signal.Type.OUTPUT, null);
                StgUtils.restoreInterfaceSignals(envStg, inputSignalNames, outputSignalNames);
            }

            // Write device STG into a .g file
            String devStgName = (envStg != null ? StgUtils.DEVICE_FILE_PREFIX : StgUtils.SYSTEM_FILE_PREFIX) + stgFileExtension;
            File devStgFile = new File(directory, devStgName);
            Result<? extends ExportOutput> devExportResult = StgUtils.exportStg(devStg, devStgFile, monitor);
            if (devExportResult.getOutcome() != Result.Outcome.SUCCESS) {
                if (devExportResult.getOutcome() == Result.Outcome.CANCEL) {
                    return new Result<>(Result.Outcome.CANCEL);
                }
                return new Result<>(Result.Outcome.FAILURE,
                        new SpotChainOutput(ltl2tgbaResult, devExportResult, null, null));
            }
            monitor.progressUpdate(0.10);

            // Generating system .g for custom property check (only if needed)
            File sysStgFile = null;
            Result<? extends PcompOutput>  pcompResult = null;
            if (envStg == null) {
                sysStgFile = devStgFile;
            } else {
                File envStgFile = new File(directory, StgUtils.ENVIRONMENT_FILE_PREFIX + stgFileExtension);
                Result<? extends ExportOutput> envExportResult = StgUtils.exportStg(envStg, envStgFile, monitor);
                if (envExportResult.getOutcome() != Result.Outcome.SUCCESS) {
                    if (envExportResult.getOutcome() == Result.Outcome.CANCEL) {
                        return new Result<>(Result.Outcome.CANCEL);
                    }
                    return new Result<>(Result.Outcome.FAILURE,
                            new SpotChainOutput(ltl2tgbaResult, envExportResult, null, null));
                }

                // Generating .g for the whole system (circuit and environment)
                pcompResult = CircuitStgUtils.composeDevWithEnv(devStgFile, envStgFile, directory, monitor);
                if (pcompResult.getOutcome() != Result.Outcome.SUCCESS) {
                    if (pcompResult.getOutcome() == Result.Outcome.CANCEL) {
                        return new Result<>(Result.Outcome.CANCEL);
                    }
                    return new Result<>(Result.Outcome.FAILURE,
                            new SpotChainOutput(ltl2tgbaResult, envExportResult, pcompResult, null));
                }
                sysStgFile = pcompResult.getPayload().getOutputFile();
            }
            monitor.progressUpdate(0.20);

            // Add initial states to the system STG
            Stg sysModStg = StgUtils.loadStg(sysStgFile);
            File sysModStgFile = new File(directory, StgUtils.SYSTEM_FILE_PREFIX + StgUtils.MODIFIED_FILE_SUFFIX + stgFileExtension);
            FileOutputStream sysModStgStream = new FileOutputStream(sysModStgFile);
            SerialiserUtils.writeModel(sysModStg, sysModStgStream, SerialiserUtils.Style.STG, true);
            sysModStgStream.close();
            monitor.progressUpdate(0.30);

            // Generate unfolding
            File hoaFile = ltl2tgbaResult.getPayload().getOutputFile();
            PunfLtlxTask punfTask = new PunfLtlxTask(sysModStgFile, hoaFile, directory);
            SubtaskMonitor<Object> punfMonitor = new SubtaskMonitor<>(monitor);
            Result<? extends PunfOutput> punfResult = manager.execute(punfTask, "Unfolding .g", punfMonitor);

            if (punfResult.getOutcome() != Result.Outcome.SUCCESS) {
                if (punfResult.getOutcome() == Result.Outcome.CANCEL) {
                    return new Result<>(Result.Outcome.CANCEL);
                }
                return new Result<>(Result.Outcome.FAILURE,
                        new SpotChainOutput(ltl2tgbaResult, devExportResult, pcompResult, punfResult));
            }
            monitor.progressUpdate(1.0);

            return new Result<>(Result.Outcome.SUCCESS,
                    new SpotChainOutput(ltl2tgbaResult, devExportResult, pcompResult, punfResult));
        } catch (Throwable e) {
            return new Result<>(e);
        } finally {
            FileUtils.deleteOnExitRecursively(directory);
        }
    }

}
