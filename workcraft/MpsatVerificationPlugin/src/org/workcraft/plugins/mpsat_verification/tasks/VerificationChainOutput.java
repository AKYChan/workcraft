package org.workcraft.plugins.mpsat_verification.tasks;

import org.workcraft.plugins.mpsat_verification.presets.VerificationParameters;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.plugins.punf.tasks.PunfOutput;
import org.workcraft.tasks.ExportOutput;
import org.workcraft.tasks.Result;

public class VerificationChainOutput extends ChainOutput {

    private final Result<? extends MpsatOutput> mpsatResult;
    private final VerificationParameters verificationParameters;

    public VerificationChainOutput(Result<? extends ExportOutput> exportResult,
            Result<? extends PcompOutput> pcompResult,
            Result<? extends PunfOutput> punfResult,
            Result<? extends MpsatOutput> mpsatResult,
            VerificationParameters verificationParameters) {

        this(exportResult, pcompResult, punfResult, mpsatResult, verificationParameters, null);
    }

    public VerificationChainOutput(Result<? extends ExportOutput> exportResult,
            Result<? extends PcompOutput> pcompResult,
            Result<? extends PunfOutput> punfResult,
            Result<? extends MpsatOutput> mpsatResult,
            VerificationParameters verificationParameters,
            String message) {

        super(exportResult, pcompResult, punfResult, message);
        this.mpsatResult = mpsatResult;
        this.verificationParameters = verificationParameters;
    }

    public VerificationParameters getVerificationParameters() {
        return verificationParameters;
    }

    public Result<? extends MpsatOutput> getMpsatResult() {
        return mpsatResult;
    }

}
