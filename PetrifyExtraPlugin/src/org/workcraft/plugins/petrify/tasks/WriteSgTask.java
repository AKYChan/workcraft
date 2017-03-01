package org.workcraft.plugins.petrify.tasks;

import java.io.File;
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
    private final List<String> options;
    private final File inputFile;
    private final File outputFile;
    private final File workingDirectory;

    private ProgressMonitor<? super ExternalProcessResult> monitor;

    private final DataAccumulator stdoutAccum = new DataAccumulator();
    private final DataAccumulator stderrAccum = new DataAccumulator();

    public WriteSgTask(List<String> options, File inputFile, File outputFile, File workingDirectory) {
        this.options = options;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.workingDirectory = workingDirectory;
    }

    @Override
    public Result<? extends ExternalProcessResult> run(ProgressMonitor<? super ExternalProcessResult> monitor) {
        this.monitor = monitor;
        ArrayList<String> command = new ArrayList<>();

        // Name of the executable
        String toolName = ToolUtils.getAbsoluteCommandPath(PetrifyUtilitySettings.getCommand());
        command.add(toolName);
        command.add("-write_sg");

        // Built-in arguments
        if (options != null) {
            for (String arg : options) {
                command.add(arg);
            }
        }

        // Input file
        if (inputFile != null) {
            command.add(inputFile.getAbsolutePath());
        }

        // Output file
        if (outputFile != null) {
            command.add("-o");
            command.add(outputFile.getAbsolutePath());
        }

        ExternalProcessTask task = new ExternalProcessTask(command, workingDirectory);
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
