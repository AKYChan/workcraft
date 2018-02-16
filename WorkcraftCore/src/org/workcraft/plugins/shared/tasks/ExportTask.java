package org.workcraft.plugins.shared.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.workcraft.dom.Model;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.interop.Exporter;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.tasks.Task;
import org.workcraft.util.LogUtils;

public class ExportTask implements Task<ExportOutput> {
    Exporter exporter;
    Model model;
    File file;

    public ExportTask(Exporter exporter, Model model, String path) {
        this.exporter = exporter;
        this.model = model;
        this.file = new File(path);
    }

    @Override
    public Result<? extends ExportOutput> run(ProgressMonitor<? super ExportOutput> monitor) {
        String message = "Exporting model ";
        String title = model.getTitle();
        if (!title.isEmpty()) {
            message += "\'" + title + "\' ";
        }
        message += "to file \'" + file.getAbsolutePath() + "\'.";
        LogUtils.logInfo(message);

        FileOutputStream fos;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            return new Result<ExportOutput>(e);
        }

        boolean success = false;
        try {
            // For incompatible visual model try exporting its underlying math model.
            if ((model instanceof VisualModel) && !exporter.isCompatible(model)) {
                MathModel mathModel = ((VisualModel) model).getMathModel();
                if (exporter.isCompatible(mathModel)) {
                    model = mathModel;
                } else {
                    String exporterName = exporter.getFormat().getDescription();
                    String modelName = model.getDisplayName();
                    String text = "Exporter to " + exporterName + " is not compatible with " + modelName + " model.";
                    // FIXME: Is it really necessary to nest the exceptions?
                    Exception nestedException = new Exception(new RuntimeException(text));
                    return new Result<ExportOutput>(nestedException);
                }
            }
            exporter.export(model, fos);
            success = true;
        } catch (Throwable e) {
            return new Result<ExportOutput>(e);
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                return new Result<ExportOutput>(e);
            }
            if (!success) {
                file.delete();
            }
        }

        return new Result<ExportOutput>(Outcome.SUCCESS, new ExportOutput(file));
    }

}
