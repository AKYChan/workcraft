package org.workcraft.plugins.pcomp.tasks;

import java.io.File;
import java.util.ArrayList;

import org.workcraft.plugins.pcomp.PcompUtilitySettings;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.plugins.shared.tasks.ExternalProcessTask;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.tasks.Task;
import org.workcraft.util.ToolUtils;

public class PcompTask implements Task<ExternalProcessResult> {

    public enum ConversionMode {
        DUMMY,
        INTERNAL,
        OUTPUT
    }

    private final File[] inputFiles;
    private final File outputFile;
    private final File placesFile;
    private final ConversionMode conversionMode;
    private final boolean useSharedOutputs;
    private final boolean useImprovedComposition;
    private final File directory;

    public PcompTask(File[] inputFiles, File outputFile, File placesFile,
            ConversionMode conversionMode, boolean useSharedOutputs, boolean useImprovedComposition, File directory) {
        this.inputFiles = inputFiles;
        this.outputFile = outputFile;
        this.placesFile = placesFile;
        this.conversionMode = conversionMode;
        this.useSharedOutputs = useSharedOutputs;
        this.useImprovedComposition = useImprovedComposition;
        this.directory = directory;
    }

    @Override
    public Result<? extends ExternalProcessResult> run(ProgressMonitor<? super ExternalProcessResult> monitor) {
        ArrayList<String> command = new ArrayList<>();

        // Name of the executable
        String toolName = ToolUtils.getAbsoluteCommandPath(PcompUtilitySettings.getCommand());
        command.add(toolName);

        // Built-in arguments
        if (conversionMode == ConversionMode.DUMMY) {
            command.add("-d");
            command.add("-r");
        } else if (conversionMode == ConversionMode.INTERNAL) {
            command.add("-i");
        }

        if (useSharedOutputs) {
            command.add("-o");
        }

        if (useImprovedComposition) {
            command.add("-p");
        }

        // Extra arguments (should go before the file parameters)
        for (String arg : PcompUtilitySettings.getExtraArgs().split("\\s")) {
            if (!arg.isEmpty()) {
                command.add(arg);
            }
        }

        // Composed STG output file
        if (outputFile != null) {
            command.add("-f" + outputFile.getAbsolutePath());
        }

        // List of places output file
        if (placesFile != null) {
            command.add("-l" + placesFile.getAbsolutePath());
        }

        // STG input files
        for (File inputFile: inputFiles) {
            if (inputFile != null) {
                command.add(inputFile.getAbsolutePath());
            }
        }

        ExternalProcessTask task = new ExternalProcessTask(command, directory, false, true);
        Result<? extends ExternalProcessResult> res = task.run(monitor);
        if (res.getOutcome() != Outcome.FINISHED) {
            return res;
        }

        ExternalProcessResult retVal = res.getReturnValue();
        ExternalProcessResult result = new ExternalProcessResult(retVal.getReturnCode(), retVal.getOutput(), retVal.getErrors());
        if (retVal.getReturnCode() < 2) {
            return Result.finished(result);
        } else {
            return Result.failed(result);
        }

    }
}