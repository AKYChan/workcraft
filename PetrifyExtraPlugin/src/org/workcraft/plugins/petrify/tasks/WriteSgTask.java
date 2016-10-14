package org.workcraft.plugins.petrify.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.workcraft.interop.ExternalProcessListener;
import org.workcraft.plugins.petrify.PetrifyUtilitySettings;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.plugins.shared.tasks.ExternalProcessTask;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.tasks.Task;
import org.workcraft.util.DataAccumulator;
import org.workcraft.util.ToolUtils;

public class WriteSgTask implements Task<ExternalProcessResult>, ExternalProcessListener {
    private final String inputPath, outputPath;
    private final List<String> options;

    private ProgressMonitor<? super ExternalProcessResult> monitor;

    private final DataAccumulator stdoutAccum = new DataAccumulator();
    private final DataAccumulator stderrAccum = new DataAccumulator();

    public WriteSgTask(String inputPath, String outputPath, List<String> options) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.options = options;
    }

    @Override
    public Result<? extends ExternalProcessResult> run(ProgressMonitor<? super ExternalProcessResult> monitor) {
        this.monitor = monitor;
        ArrayList<String> command = new ArrayList<>();

        // Name of the executable
        String toolName = ToolUtils.getAbsoluteCommandPath(PetrifyUtilitySettings.getCommand());
        command.add(toolName);

        // Built-in arguments
        if (options != null) {
            for (String arg : options) {
                command.add(arg);
            }
        }

        // Input file
        if ((inputPath != null) && !inputPath.isEmpty()) {
            command.add(inputPath);
        }

        // Output file
        if ((outputPath != null) && !outputPath.isEmpty()) {
            command.add("-o");
            command.add(outputPath);
        }

        ExternalProcessTask task = new ExternalProcessTask(command, null);
        Result<? extends ExternalProcessResult> res = task.run(monitor);
        if (res.getOutcome() != Outcome.FINISHED) {
            return res;
        }

        ExternalProcessResult retVal = res.getReturnValue();
        if (retVal.getReturnCode() == 0) {
            return Result.finished(retVal);
        } else {
            return Result.failed(retVal);
        }
    }

    @Override
    public void errorData(byte[] data) {
        try {
            stderrAccum.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        monitor.stderr(data);
    }

    @Override
    public void outputData(byte[] data) {
        try {
            stdoutAccum.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        monitor.stdout(data);
    }

    @Override
    public void processFinished(int returnCode) {
    }

}
