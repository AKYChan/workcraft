package org.workcraft.plugins.punf.tasks;

import java.util.ArrayList;

import org.workcraft.plugins.punf.PunfSettings;
import org.workcraft.plugins.shared.tasks.ExternalProcessOutput;
import org.workcraft.plugins.shared.tasks.ExternalProcessTask;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.tasks.Task;
import org.workcraft.util.ToolUtils;

public class PunfTask implements Task<ExternalProcessOutput> {
    public static final String PNML_FILE_EXTENSION = ".pnml";
    public static final String MCI_FILE_EXTENSION = ".mci";
    public static final String LEGACY_TOOL_SUFFIX = "-mci";

    private final String inputPath;
    private final String outputPath;
    private final boolean useLegacyMci;

    public PunfTask(String inputPath, String outputPath) {
        this(inputPath, outputPath, false);
    }

    public PunfTask(String inputPath, String outputPath, boolean useLegacyMci) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.useLegacyMci = useLegacyMci;
    }

    @Override
    public Result<? extends ExternalProcessOutput> run(ProgressMonitor<? super ExternalProcessOutput> monitor) {
        ArrayList<String> command = new ArrayList<>();

        // Name of the executable
        String toolPrefix = PunfSettings.getCommand();
        String toolSuffix = useLegacyMci ? LEGACY_TOOL_SUFFIX : "";
        String toolName = ToolUtils.getAbsoluteCommandWithSuffixPath(toolPrefix, toolSuffix);
        command.add(toolName);

        // Extra arguments (should go before the file parameters)
        for (String arg : PunfSettings.getArgs().split("\\s")) {
            if (!arg.isEmpty()) {
                command.add(arg);
            }
        }

        // Built-in arguments
        command.add("-m=" + outputPath);
        command.add(inputPath);

        boolean printStdout = PunfSettings.getPrintStdout();
        boolean printStderr = PunfSettings.getPrintStderr();
        ExternalProcessTask task = new ExternalProcessTask(command, null, printStdout, printStderr);
        Result<? extends ExternalProcessOutput> result = task.run(monitor);

        if (result.getOutcome() != Outcome.SUCCESS) {
            return result;
        }

        ExternalProcessOutput returnValue = result.getPayload();
        int returnCode = returnValue.getReturnCode();
        if ((returnCode == 0) || (returnCode == 1)) {
            return Result.success(returnValue);
        } else {
            return Result.failure(returnValue);
        }
    }

}
