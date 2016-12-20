package org.workcraft.plugins.cpog.commands;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.graph.commands.AbstractConversionCommand;
import org.workcraft.plugins.cpog.CpogDescriptor;
import org.workcraft.plugins.cpog.PetriToCpogSettings;
import org.workcraft.plugins.cpog.VisualCpog;
import org.workcraft.plugins.cpog.gui.PetriToCpogDialog;
import org.workcraft.plugins.cpog.untangling.PetriToCpogConverter;
import org.workcraft.plugins.petri.VisualPetriNet;
import org.workcraft.util.GUI;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class PetriToCpogConversionCommand extends AbstractConversionCommand {

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, VisualPetriNet.class);
    }

    @Override
    public String getDisplayName() {
        return "Conditional Partial Order Graph [Untanglings]";
    }

    @Override
    public ModelEntry convert(ModelEntry me) {
        final Framework framework = Framework.getInstance();
        final MainWindow mainWindow = framework.getMainWindow();
        PetriToCpogSettings settings = new PetriToCpogSettings();
        PetriToCpogDialog dialog = new PetriToCpogDialog(mainWindow, settings);
        GUI.centerToParent(dialog, mainWindow);
        dialog.setVisible(true);
        if (dialog.getModalResult() != 1) {
            return null;
        } else {
            VisualPetriNet src = me.getAs(VisualPetriNet.class);
            PetriToCpogConverter converter = new PetriToCpogConverter(src);
            VisualCpog dst = converter.run(settings);
            return new ModelEntry(new CpogDescriptor(), dst);
        }
    }

}