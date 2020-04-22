package org.workcraft.plugins.punf.tasks;

import org.workcraft.plugins.punf.PunfSettings;
import org.workcraft.tasks.*;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.utils.ExecutableUtils;
import org.workcraft.utils.TextUtils;

import java.io.File;
import java.util.ArrayList;

public class PunfTask implements Task<PunfOutput> {

    public static final String PNML_FILE_EXTENSION = ".pnml";
    public static final String MCI_FILE_EXTENSION = ".mci";
    public static final String LEGACY_TOOL_SUFFIX = "-mci";

    private final File inputFile;
    private final File outputFile;
    private final File directory;

    public PunfTask(File inputFile, File outputFile, File directory) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.directory = directory;
    }

    @Override
    public Result<? extends PunfOutput> run(ProgressMonitor<? super PunfOutput> monitor) {
        ArrayList<String> command = new ArrayList<>();

        // Name of the executable
        String toolPrefix = PunfSettings.getCommand();
        String toolSuffix = outputFile.getName().endsWith(MCI_FILE_EXTENSION) ? LEGACY_TOOL_SUFFIX : "";
        String toolName = ExecutableUtils.getAbsoluteCommandWithSuffixPath(toolPrefix, toolSuffix);
        command.add(toolName);

        // Extra arguments (should go before the file parameters)
        command.addAll(TextUtils.splitWords(PunfSettings.getArgs()));

        // Built-in arguments
        command.add("-m=" + outputFile.getAbsolutePath());
        command.add(inputFile.getAbsolutePath());

        boolean printStdout = PunfSettings.getPrintStdout();
        boolean printStderr = PunfSettings.getPrintStderr();
        ExternalProcessTask task = new ExternalProcessTask(command, directory, printStdout, printStderr);
        SubtaskMonitor<? super ExternalProcessOutput> subtaskMonitor = new SubtaskMonitor<>(monitor);
        Result<? extends ExternalProcessOutput> result = task.run(subtaskMonitor);

        if (result.getOutcome() == Outcome.SUCCESS) {
            ExternalProcessOutput output = result.getPayload();
            if (output != null) {
                PunfOutput punfOutput = new PunfOutput(output);
                int returnCode = output.getReturnCode();
                if ((returnCode == 0) || (returnCode == 1)) {
                    return Result.success(punfOutput);
                }
                return Result.failure(punfOutput);
            }
        }

        if (result.getOutcome() == Outcome.CANCEL) {
            return Result.cancelation();
        }

        return Result.exception(result.getCause());
    }

}
