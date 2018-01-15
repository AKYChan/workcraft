package org.workcraft.plugins.wtg.tasks;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.plugins.stg.interop.StgImporter;
import org.workcraft.plugins.wtg.Wtg;
import org.workcraft.plugins.wtg.interop.WtgFormat;
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

public class WaverConversionTask implements Task<WaverConversionResult> {

    private final WorkspaceEntry we;

    public WaverConversionTask(WorkspaceEntry we) {
        this.we = we;
    }

    public WorkspaceEntry getWorkspaceEntry() {
        return we;
    }

    @Override
    public Result<? extends WaverConversionResult> run(ProgressMonitor<? super WaverConversionResult> monitor) {
        final Framework framework = Framework.getInstance();
        try {
            // Common variables
            monitor.progressUpdate(0.05);
            Wtg wtg = WorkspaceUtils.getAs(we, Wtg.class);
            Exporter wtgExporter = Export.chooseBestExporter(framework.getPluginManager(), wtg, WtgFormat.getInstance());
            if (wtgExporter == null) {
                throw new RuntimeException("Exporter not available: model class " + wtg.getClass().getName() + " to format STG.");
            }
            SubtaskMonitor<Object> subtaskMonitor = new SubtaskMonitor<>(monitor);
            monitor.progressUpdate(0.10);

            // Generating .wtg file
            File wtgFile = FileUtils.createTempFile("wtg-", ".g");
            wtgFile.deleteOnExit();
            ExportTask wtgExportTask = new ExportTask(wtgExporter, wtg, wtgFile.getAbsolutePath());
            Result<? extends Object> wtgExportResult = framework.getTaskManager().execute(
                    wtgExportTask, "Exporting .wtg", subtaskMonitor);

            if (wtgExportResult.getOutcome() != Outcome.SUCCESS) {
                if (wtgExportResult.getOutcome() == Outcome.CANCEL) {
                    return new Result<WaverConversionResult>(Outcome.CANCEL);
                }
                return new Result<WaverConversionResult>(Outcome.FAILURE);
            }
            monitor.progressUpdate(0.20);

            // Generate STG
            WaverTask waverTask = new WaverTask(wtgFile, null, null);
            Result<? extends ExternalProcessResult> waverResult = framework.getTaskManager().execute(
                    waverTask, "Building state graph", subtaskMonitor);

            if (waverResult.getOutcome() == Outcome.SUCCESS) {
                try {
                    ByteArrayInputStream in = new ByteArrayInputStream(waverResult.getReturnValue().getOutput());
                    final StgModel stg = new StgImporter().importStg(in);
                    return Result.success(new WaverConversionResult(null, (Stg) stg));
                } catch (DeserialisationException e) {
                    return Result.exception(e);
                }
            }
            if (waverResult.getOutcome() == Outcome.CANCEL) {
                return Result.cancelation();
            }
            if (waverResult.getCause() != null) {
                return Result.exception(waverResult.getCause());
            } else {
                return Result.failure(new WaverConversionResult(waverResult, null));
            }
        } catch (Throwable e) {
            return new Result<WaverConversionResult>(e);
        }
    }

}
