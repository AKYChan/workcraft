package org.workcraft.plugins.mpsat.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.workcraft.Framework;
import org.workcraft.PluginManager;
import org.workcraft.exceptions.NoExporterException;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.mpsat.MpsatMode;
import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.punf.PunfSettings;
import org.workcraft.plugins.punf.tasks.PunfOutput;
import org.workcraft.plugins.punf.tasks.PunfTask;
import org.workcraft.plugins.shared.tasks.ExportOutput;
import org.workcraft.plugins.shared.tasks.ExportTask;
import org.workcraft.plugins.stg.interop.StgFormat;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.tasks.SubtaskMonitor;
import org.workcraft.tasks.Task;
import org.workcraft.tasks.TaskManager;
import org.workcraft.util.ExportUtils;
import org.workcraft.util.FileUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class MpsatCombinedChainTask implements Task<MpsatCombinedChainOutput> {
    private final WorkspaceEntry we;
    private final List<MpsatParameters> settingsList;

    public MpsatCombinedChainTask(WorkspaceEntry we, List<MpsatParameters> settingsList) {
        this.we = we;
        this.settingsList = settingsList;
    }

    @Override
    public Result<? extends MpsatCombinedChainOutput> run(ProgressMonitor<? super MpsatCombinedChainOutput> monitor) {
        Framework framework = Framework.getInstance();
        PluginManager pluginManager = framework.getPluginManager();
        TaskManager taskManager = framework.getTaskManager();
        String prefix = FileUtils.getTempPrefix(we.getTitle());
        File directory = FileUtils.createTempDirectory(prefix);
        try {
            PetriNetModel model = WorkspaceUtils.getAs(we, PetriNetModel.class);
            StgFormat format = StgFormat.getInstance();
            Exporter exporter = ExportUtils.chooseBestExporter(pluginManager, model, format);
            if (exporter == null) {
                throw new NoExporterException(model, format);
            }
            SubtaskMonitor<Object> subtaskMonitor = new SubtaskMonitor<>(monitor);

            // Generate .g for the model
            File netFile = new File(directory, "net" + format.getExtension());
            ExportTask exportTask = new ExportTask(exporter, model, netFile.getAbsolutePath());
            Result<? extends ExportOutput> exportResult = taskManager.execute(
                    exportTask, "Exporting .g", subtaskMonitor);

            if (exportResult.getOutcome() != Outcome.SUCCESS) {
                if (exportResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<>(Outcome.CANCEL);
                }
                return new Result<>(Outcome.FAILURE,
                        new MpsatCombinedChainOutput(exportResult, null, null, null, settingsList));
            }
            monitor.progressUpdate(0.33);

            // Generate unfolding
            boolean useMci = false;
            if (PunfSettings.getUseMciCsc()) {
                useMci = true;
                for (MpsatParameters settings: settingsList) {
                    useMci &= settings.getMode() == MpsatMode.RESOLVE_ENCODING_CONFLICTS;
                }
            }
            String unfoldingExtension = useMci ? PunfTask.MCI_FILE_EXTENSION : PunfTask.PNML_FILE_EXTENSION;

            File unfoldingFile = new File(directory, "unfolding" + unfoldingExtension);
            PunfTask punfTask = new PunfTask(netFile, unfoldingFile, directory);
            Result<? extends PunfOutput> punfResult = taskManager.execute(punfTask, "Unfolding .g", subtaskMonitor);

            if (punfResult.getOutcome() != Outcome.SUCCESS) {
                if (punfResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<>(Outcome.CANCEL);
                }
                return new Result<>(Outcome.FAILURE,
                        new MpsatCombinedChainOutput(exportResult, null, punfResult, null, settingsList));
            }
            monitor.progressUpdate(0.66);

            // Run MPSat on the generated unfolding
            ArrayList<Result<? extends MpsatOutput>> mpsatResultList = new ArrayList<>(settingsList.size());
            for (MpsatParameters settings: settingsList) {
                MpsatTask mpsatTask = new MpsatTask(settings.getMpsatArguments(directory), unfoldingFile, directory, netFile);
                Result<? extends MpsatOutput> mpsatResult = taskManager.execute(
                        mpsatTask, "Running verification [MPSat]", subtaskMonitor);
                mpsatResultList.add(mpsatResult);
                if (mpsatResult.getOutcome() != Outcome.SUCCESS) {
                    if (mpsatResult.getOutcome() == Outcome.CANCEL) {
                        return new Result<>(Outcome.CANCEL);
                    }
                    return new Result<>(Outcome.FAILURE,
                            new MpsatCombinedChainOutput(exportResult, null, punfResult, mpsatResultList, settingsList));
                }
            }
            monitor.progressUpdate(1.0);

            return new Result<>(Outcome.SUCCESS,
                    new MpsatCombinedChainOutput(exportResult, null, punfResult, mpsatResultList, settingsList));
        } catch (Throwable e) {
            return new Result<>(e);
        } finally {
            FileUtils.deleteOnExitRecursively(directory);
        }
    }

    public List<MpsatParameters> getSettingsList() {
        return settingsList;
    }

    public WorkspaceEntry getWorkspaceEntry() {
        return we;
    }

}
