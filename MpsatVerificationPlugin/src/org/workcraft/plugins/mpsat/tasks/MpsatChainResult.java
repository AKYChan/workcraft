package org.workcraft.plugins.mpsat.tasks;

import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.plugins.punf.tasks.PunfOutput;
import org.workcraft.plugins.shared.tasks.ExportOutput;
import org.workcraft.plugins.shared.tasks.ExternalProcessOutput;
import org.workcraft.tasks.Result;

public class MpsatChainResult {
    private Result<? extends ExportOutput> exportResult;
    private Result<? extends PcompOutput> pcompResult;
    private Result<? extends PunfOutput> punfResult;
    private Result<? extends ExternalProcessOutput> mpsatResult;
    private MpsatParameters mpsatSettings;
    private String message;

    public MpsatChainResult(Result<? extends ExportOutput> exportResult,
            Result<? extends PcompOutput> pcompResult,
            Result<? extends PunfOutput> punfResult,
            Result<? extends ExternalProcessOutput> mpsatResult,
            MpsatParameters mpsatSettings, String message) {

        this.exportResult = exportResult;
        this.pcompResult = pcompResult;
        this.punfResult = punfResult;
        this.mpsatResult = mpsatResult;
        this.mpsatSettings = mpsatSettings;
        this.message = message;
    }

    public MpsatChainResult(Result<? extends ExportOutput> exportResult,
            Result<? extends PcompOutput> pcompResult,
            Result<? extends PunfOutput> punfResult,
            Result<? extends ExternalProcessOutput> mpsatResult,
            MpsatParameters mpsatSettings) {

        this(exportResult, pcompResult, punfResult, mpsatResult, mpsatSettings, null);
    }

    public MpsatParameters getMpsatSettings() {
        return mpsatSettings;
    }

    public Result<? extends ExportOutput> getExportResult() {
        return exportResult;
    }

    public Result<? extends PcompOutput> getPcompResult() {
        return pcompResult;
    }

    public Result<? extends PunfOutput> getPunfResult() {
        return punfResult;
    }

    public Result<? extends ExternalProcessOutput> getMpsatResult() {
        return mpsatResult;
    }

    public String getMessage() {
        return message;
    }

}
