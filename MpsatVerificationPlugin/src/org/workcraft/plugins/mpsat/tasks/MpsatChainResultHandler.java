package org.workcraft.plugins.mpsat.tasks;

import java.util.Collection;

import javax.swing.SwingUtilities;

import org.workcraft.plugins.mpsat.MpsatMode;
import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.mpsat.MpsatSolution;
import org.workcraft.plugins.mpsat.MpsatUtils;
import org.workcraft.plugins.mpsat.PunfResultParser;
import org.workcraft.plugins.mpsat.PunfResultParser.Cause;
import org.workcraft.plugins.punf.tasks.PunfOutput;
import org.workcraft.plugins.shared.tasks.ExportOutput;
import org.workcraft.plugins.shared.tasks.ExternalProcessOutput;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.tasks.AbstractResultHandler;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.util.DialogUtils;
import org.workcraft.util.Pair;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatChainResultHandler extends AbstractResultHandler<MpsatChainResult> {

    private static final String TITLE = "Verification results";
    private static final String CANNOT_VERIFY_PREFIX = "Cannot build unfolding prefix";
    private static final String AFTER_THE_TRACE_SUFFIX = " after the following trace:\n";
    private static final String ASK_SIMULATE_SUFFIX = "\n\nSimulate the problematic trace?";
    private static final String ERROR_CAUSE_PREFIX = "\n\n";

    private final WorkspaceEntry we;
    private final Collection<Mutex> mutexes;

    public MpsatChainResultHandler(WorkspaceEntry we) {
        this(we, null);
    }

    public MpsatChainResultHandler(WorkspaceEntry we, Collection<Mutex> mutexes) {
        this.we = we;
        this.mutexes = mutexes;
    }

    public Collection<Mutex> getMutexes() {
        return mutexes;
    }

    @Override
    public void handleResult(final Result<? extends MpsatChainResult> result) {
        if (result.getOutcome() == Outcome.SUCCESS) {
            handleSuccess(result);
        } else if (result.getOutcome() == Outcome.FAILURE) {
            if (!handlePartialFailure(result)) {
                handleFailure(result);
            }
        }
    }

    private void handleSuccess(final Result<? extends MpsatChainResult> result) {
        MpsatChainResult returnValue = result.getPayload();
        Result<? extends ExternalProcessOutput> mpsatResult = (returnValue == null) ? null : returnValue.getMpsatResult();
        MpsatParameters mpsatSettings = returnValue.getMpsatSettings();
        switch (mpsatSettings.getMode()) {
        case UNDEFINED:
            String undefinedMessage = returnValue.getMessage();
            if ((undefinedMessage == null) && (mpsatSettings != null) && (mpsatSettings.getName() != null)) {
                undefinedMessage = mpsatSettings.getName();
            }
            SwingUtilities.invokeLater(new MpsatUndefinedResultHandler(undefinedMessage));
            break;
        case REACHABILITY:
        case STG_REACHABILITY:
        case STG_REACHABILITY_CONSISTENCY:
        case STG_REACHABILITY_OUTPUT_PERSISTENCY:
        case STG_REACHABILITY_CONFORMATION:
        case NORMALCY:
        case ASSERTION:
            SwingUtilities.invokeLater(new MpsatReachabilityResultHandler(we, mpsatResult, mpsatSettings));
            break;
        case CSC_CONFLICT_DETECTION:
        case USC_CONFLICT_DETECTION:
            SwingUtilities.invokeLater(new MpsatEncodingConflictResultHandler(we, mpsatResult));
            break;
        case DEADLOCK:
            SwingUtilities.invokeLater(new MpsatDeadlockResultHandler(we, mpsatResult));
            break;
        case RESOLVE_ENCODING_CONFLICTS:
            SwingUtilities.invokeLater(new MpsatCscConflictResolutionResultHandler(we, mpsatResult, mutexes));
            break;
        default:
            String modeString = mpsatSettings.getMode().getArgument();
            DialogUtils.showError("MPSat verification mode '" + modeString + "' is not (yet) supported.");
            break;
        }
    }

    private boolean handlePartialFailure(final Result<? extends MpsatChainResult> result) {
        MpsatChainResult returnValue = result.getPayload();
        Result<? extends PunfOutput> punfResult = (returnValue == null) ? null : returnValue.getPunfResult();
        if ((punfResult != null) && (punfResult.getOutcome() == Outcome.FAILURE)) {
            PunfResultParser prp = new PunfResultParser(punfResult.getPayload());
            Pair<MpsatSolution, PunfResultParser.Cause> punfOutcome = prp.getOutcome();
            if (punfOutcome != null) {
                MpsatSolution solution = punfOutcome.getFirst();
                Cause cause = punfOutcome.getSecond();
                MpsatParameters mpsatSettings = returnValue.getMpsatSettings();
                boolean isConsistencyCheck = (cause == Cause.INCONSISTENT)
                        && (mpsatSettings.getMode() == MpsatMode.STG_REACHABILITY_CONSISTENCY);

                if (isConsistencyCheck) {
                    int cost = solution.getMainTrace().size();
                    String mpsatFakeOutput = "SOLUTION 0\n" + solution + "\npath cost: " + cost + "\n";
                    Result<? extends ExternalProcessOutput> mpsatFakeResult = Result.success(
                            new ExternalProcessOutput(0, mpsatFakeOutput.getBytes(), new byte[0]));

                    SwingUtilities.invokeLater(new MpsatReachabilityResultHandler(
                            we, mpsatFakeResult, MpsatParameters.getConsistencySettings()));
                } else {
                    String comment = solution.getComment();
                    String message = CANNOT_VERIFY_PREFIX;
                    switch (cause) {
                    case INCONSISTENT:
                        message += " for the inconsistent STG.\n\n";
                        message += comment + AFTER_THE_TRACE_SUFFIX;
                        message += solution + ASK_SIMULATE_SUFFIX;
                        if (DialogUtils.showConfirmError(message, TITLE, true)) {
                            MpsatUtils.playTrace(we, solution);
                        }
                        break;
                    case NOT_SAFE:
                        message += "for the unsafe net.\n\n";
                        message +=  comment + AFTER_THE_TRACE_SUFFIX;
                        message += solution + ASK_SIMULATE_SUFFIX;
                        if (DialogUtils.showConfirmError(message, TITLE, true)) {
                            MpsatUtils.playTrace(we, solution);
                        }
                        break;
                    case EMPTY_PRESET:
                        message += " for the malformd net.\n\n";
                        message += comment;
                        DialogUtils.showError(message, TITLE);
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void handleFailure(final Result<? extends MpsatChainResult> result) {
        String errorMessage = "MPSat verification failed.";
        Throwable genericCause = result.getCause();
        if (genericCause != null) {
            // Exception was thrown somewhere in the chain task run() method (not in any of the subtasks)
            errorMessage += ERROR_CAUSE_PREFIX + genericCause.toString();
        } else {
            MpsatChainResult returnValue = result.getPayload();
            Result<? extends ExportOutput> exportResult = (returnValue == null) ? null : returnValue.getExportResult();
            Result<? extends PunfOutput> punfResult = (returnValue == null) ? null : returnValue.getPunfResult();
            Result<? extends ExternalProcessOutput> mpsatResult = (returnValue == null) ? null : returnValue.getMpsatResult();
            if ((exportResult != null) && (exportResult.getOutcome() == Outcome.FAILURE)) {
                errorMessage += "\n\nCould not export the model as a .g file.";
                Throwable exportCause = exportResult.getCause();
                if (exportCause != null) {
                    errorMessage += ERROR_CAUSE_PREFIX + exportCause.toString();
                }
            } else if ((punfResult != null) && (punfResult.getOutcome() == Outcome.FAILURE)) {
                errorMessage += "\n\nPunf could not build the unfolding prefix.";
                Throwable punfCause = punfResult.getCause();
                if (punfCause != null) {
                    errorMessage += ERROR_CAUSE_PREFIX + punfCause.toString();
                } else {
                    ExternalProcessOutput punfReturnValue = punfResult.getPayload();
                    if (punfReturnValue != null) {
                        String punfError = punfReturnValue.getErrorsHeadAndTail();
                        errorMessage += ERROR_CAUSE_PREFIX + punfError;
                    }
                }
            } else if ((mpsatResult != null) && (mpsatResult.getOutcome() == Outcome.FAILURE)) {
                errorMessage += "\n\nMPSat did not execute as expected.";
                Throwable mpsatCause = mpsatResult.getCause();
                if (mpsatCause != null) {
                    errorMessage += ERROR_CAUSE_PREFIX + mpsatCause.toString();
                } else {
                    ExternalProcessOutput mpsatReturnValue = mpsatResult.getPayload();
                    if (mpsatReturnValue != null) {
                        String mpsatError = mpsatReturnValue.getErrorsHeadAndTail();
                        errorMessage += ERROR_CAUSE_PREFIX + mpsatError;
                    }
                }
            } else {
                errorMessage += "\n\nMPSat chain task returned failure status without further explanation.";
            }
        }
        DialogUtils.showError(errorMessage);
    }

}
