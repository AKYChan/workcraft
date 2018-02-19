package org.workcraft.plugins.mpsat.tasks;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.gui.workspace.Path;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.MutexUtils;
import org.workcraft.plugins.stg.StgDescriptor;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.plugins.stg.interop.StgImporter;
import org.workcraft.tasks.Result;
import org.workcraft.util.DialogUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatCscConflictResolutionResultHandler implements Runnable {

    private final WorkspaceEntry we;
    private final Result<? extends MpsatOutput> result;
    private final Collection<Mutex> mutexes;
    private WorkspaceEntry weResult = null;

    public MpsatCscConflictResolutionResultHandler(final WorkspaceEntry we,
            final Result<? extends MpsatOutput> result, Collection<Mutex> mutexes) {
        this.we = we;
        this.result = result;
        this.mutexes = mutexes;
    }

    private StgModel getResolvedStg() {
        MpsatOutput output = result.getPayload();
        final byte[] content = output.getStgOutput();
        if (content == null) {
            return null;
        }
        try {
            return new StgImporter().importStg(new ByteArrayInputStream(content));
        } catch (final DeserialisationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        final Framework framework = Framework.getInstance();
        final StgModel model = getResolvedStg();
        if (model == null) {
            final String errorMessage = result.getPayload().getErrorsHeadAndTail();
            DialogUtils.showWarning("Conflict resolution failed. MPSat output: \n" + errorMessage);
        } else {
            MutexUtils.restoreMutexPlacesByName(model, mutexes);
            final ModelEntry me = new ModelEntry(new StgDescriptor(), model);
            final Path<String> path = we.getWorkspacePath();
            weResult = framework.createWork(me, path);
        }
    }

    public WorkspaceEntry getResult() {
        return weResult;
    }

}
