package org.workcraft.presets;

import org.workcraft.gui.dialogs.ModalDialog;
import org.workcraft.utils.DesktopApi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;

public abstract class PresetDialog<T> extends ModalDialog<PresetManager<T>> {

    public PresetDialog(Window owner, String title, PresetManager<T> presetManager) {
        super(owner, title, presetManager);
    }

    public JButton addCheckAction(ActionListener action) {
        return addAction("Check", action);
    }

    public JButton addHelpButton(File helpFile) {
        return addButton("Help", event -> DesktopApi.open(helpFile));
    }

    public abstract T getPresetData();
}
