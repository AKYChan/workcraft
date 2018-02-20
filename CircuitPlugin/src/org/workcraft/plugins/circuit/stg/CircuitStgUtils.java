package org.workcraft.plugins.circuit.stg;

import java.io.File;

import org.workcraft.Framework;
import org.workcraft.PluginManager;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.plugins.pcomp.tasks.PcompTask;
import org.workcraft.plugins.pcomp.tasks.PcompTask.ConversionMode;
import org.workcraft.plugins.shared.tasks.ExportOutput;
import org.workcraft.plugins.shared.tasks.ExportTask;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.interop.StgFormat;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.SubtaskMonitor;
import org.workcraft.tasks.TaskManager;
import org.workcraft.util.ExportUtils;

public class CircuitStgUtils {

    public static Result<? extends ExportOutput> exportStg(Stg stg, File stgFile, File directory,
            ProgressMonitor<?> monitor) {

        Framework framework = Framework.getInstance();
        PluginManager pluginManager = framework.getPluginManager();
        Exporter stgExporter = ExportUtils.chooseBestExporter(pluginManager, stg, StgFormat.getInstance());
        if (stgExporter == null) {
            throw new RuntimeException("Exporter not available: model class " + stg.getClass().getName() + " to .g format.");
        }

        ExportTask exportTask = new ExportTask(stgExporter, stg, stgFile.getAbsolutePath());
        String description = "Exporting " + stgFile.getAbsolutePath();
        SubtaskMonitor<Object> subtaskMonitor = null;
        if (monitor != null) {
            subtaskMonitor = new SubtaskMonitor<>(monitor);
        }
        TaskManager taskManager = framework.getTaskManager();
        return taskManager.execute(exportTask, description, subtaskMonitor);
    }

    public static Result<? extends PcompOutput> composeDevWithEnv(File devStgFile, File envStgFile,
            File sysStgFile, File detailFile, File directory, ProgressMonitor<?> monitor) {

        Framework framework = Framework.getInstance();
        File[] inputFiles = new File[]{devStgFile, envStgFile};
        PcompTask pcompTask = new PcompTask(inputFiles, sysStgFile, detailFile, ConversionMode.OUTPUT, true, false, directory);
        String description = "Running parallel composition [PComp]";
        SubtaskMonitor<Object> subtaskMonitor = null;
        if (monitor != null) {
            subtaskMonitor = new SubtaskMonitor<>(monitor);
        }
        TaskManager taskManager = framework.getTaskManager();
        return taskManager.execute(pcompTask, description, subtaskMonitor);
    }

}
