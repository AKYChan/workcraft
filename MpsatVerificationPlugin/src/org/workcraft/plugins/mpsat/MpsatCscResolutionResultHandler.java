package org.workcraft.plugins.mpsat;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.gui.workspace.Path;
import org.workcraft.plugins.shared.CommonEditorSettings;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.plugins.stg.STGModel;
import org.workcraft.plugins.stg.StgDescriptor;
import org.workcraft.plugins.stg.interop.DotGImporter;
import org.workcraft.tasks.Result;
import org.workcraft.util.FileUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatCscResolutionResultHandler implements Runnable {

    final WorkspaceEntry we;
    private final Result<? extends ExternalProcessResult> result;

    public MpsatCscResolutionResultHandler(WorkspaceEntry we, Result<? extends ExternalProcessResult> result) {
                this.we = we;
                this.result = result;
    }

    public STGModel getResolvedStg() {
        final byte[] output = result.getReturnValue().getOutputFile("mpsat.g");
        if(output == null) {
            return null;
        }
        try {
            return new DotGImporter().importSTG(new ByteArrayInputStream(output));
        } catch (DeserialisationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        final Framework framework = Framework.getInstance();
        Path<String> path = we.getWorkspacePath();
        String fileName = FileUtils.getFileNameWithoutExtension(new File(path.getNode()));

        STGModel model = getResolvedStg();
        if (model == null) {
            JOptionPane.showMessageDialog(framework.getMainWindow(),
                    "MPSat output: \n\n" + new String(result.getReturnValue().getErrors()),
                    "Conflict resolution failed", JOptionPane.WARNING_MESSAGE );
        } else {
            Path<String> directory = path.getParent();
            String name = fileName + "_resolved";
            ModelEntry me = new ModelEntry(new StgDescriptor(), model);
            Workspace workspace = framework.getWorkspace();
            boolean openInEditor = (me.isVisual() || CommonEditorSettings.getOpenNonvisual());
            workspace.add(directory, name, me, true, openInEditor);
        }
    }
}
