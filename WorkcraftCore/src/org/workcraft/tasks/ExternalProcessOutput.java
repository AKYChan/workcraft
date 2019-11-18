package org.workcraft.tasks;

import org.workcraft.utils.TextUtils;

import java.util.Collections;
import java.util.Map;

public class ExternalProcessOutput {

    private final int returnCode;
    private final byte[] stdout;
    private final byte[] stderr;
    private final Map<String, byte[]> fileDataMap;

    public ExternalProcessOutput(int returnCode, byte[] stdout, byte[] stderr) {
        this(returnCode, stdout, stderr, Collections.emptyMap());
    }

    public ExternalProcessOutput(int returnCode, byte[] stdout, byte[] stderr, Map<String, byte[]> fileDataMap) {
        this.returnCode = returnCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.fileDataMap = fileDataMap;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public byte[] getStdout() {
        return stdout;
    }

    public String getStdoutString() {
        return new String(stdout);
    }

    public byte[] getStderr() {
        return stderr;
    }

    public String getStderrString() {
        return new String(stderr);
    }

    public byte[] getFileData(String name) {
        return fileDataMap.get(name);
    }

    public String getErrorsHeadAndTail() {
        return TextUtils.getHeadAndTail(getStderrString(), 10, 10);
    }

}
