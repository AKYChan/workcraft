package org.workcraft.gui;

import org.workcraft.gui.actions.ActionButton;
import org.workcraft.gui.actions.ActionToggle;
import org.workcraft.plugins.builtin.settings.EditorCommonSettings;
import org.workcraft.plugins.builtin.settings.VisualCommonSettings;
import org.workcraft.utils.GuiUtils;

import javax.swing.*;

@SuppressWarnings("serial")
public class ToolBar extends JToolBar {

    private final MainWindow mainWindow;

    private ActionToggle gridToggle;
    private ActionToggle rulerToggle;
    private ActionToggle nameToggle;
    private ActionToggle labelToggle;

    public ToolBar(final MainWindow mainWindow) {
        super("Global");
        this.mainWindow = mainWindow;
        addFileButtons();
        addSeparator();
        addEditButtons();
        addSeparator();
        addViewButtons();
        addSeparator();
        addShowToggles();
        addSeparator();
        refreshToggles();
    }

    private void addFileButtons() {
        ActionButton createButton = new ActionButton(MainWindowActions.CREATE_WORK_ACTION,
                GuiUtils.createIconFromSVG("images/toolbar-file-create.svg"));
        createButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(createButton);

        ActionButton openButton = new ActionButton(MainWindowActions.OPEN_WORK_ACTION,
                GuiUtils.createIconFromSVG("images/toolbar-file-open.svg"));
        openButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(openButton);

        ActionButton saveButton = new ActionButton(MainWindowActions.SAVE_WORK_ACTION,
                GuiUtils.createIconFromSVG("images/toolbar-file-save.svg"));
        saveButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(saveButton);
    }

    private void addEditButtons() {
        ActionButton undoButton = new ActionButton(MainWindowActions.EDIT_UNDO_ACTION,
                GuiUtils.createIconFromSVG("images/toolbar-edit-undo.svg"));
        undoButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(undoButton);

        ActionButton redoButton = new ActionButton(MainWindowActions.EDIT_REDO_ACTION,
                GuiUtils.createIconFromSVG("images/toolbar-edit-redo.svg"));
        redoButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(redoButton);

        ActionButton copyButton = new ActionButton(MainWindowActions.EDIT_COPY_ACTION,
                GuiUtils.createIconFromSVG("images/toolbar-edit-copy.svg"));
        copyButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(copyButton);

        ActionButton pasteButton = new ActionButton(MainWindowActions.EDIT_PASTE_ACTION,
                GuiUtils.createIconFromSVG("images/toolbar-edit-paste.svg"));
        pasteButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(pasteButton);
    }

    private void addViewButtons() {
        ActionButton zoomInButton = new ActionButton(MainWindowActions.VIEW_ZOOM_IN,
                GuiUtils.createIconFromSVG("images/toolbar-view-zoom_in.svg"));
        zoomInButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(zoomInButton);

        ActionButton zoomOutButton = new ActionButton(MainWindowActions.VIEW_ZOOM_OUT,
                GuiUtils.createIconFromSVG("images/toolbar-view-zoom_out.svg"));
        zoomOutButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(zoomOutButton);

        ActionButton zoomDefaultButton = new ActionButton(MainWindowActions.VIEW_ZOOM_DEFAULT,
                GuiUtils.createIconFromSVG("images/toolbar-view-zoom_default.svg"));
        zoomDefaultButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(zoomDefaultButton);

        ActionButton zoomFitButton = new ActionButton(MainWindowActions.VIEW_ZOOM_FIT,
                GuiUtils.createIconFromSVG("images/toolbar-view-zoom_fit.svg"));
        zoomFitButton.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(zoomFitButton);
    }

    private void addShowToggles() {
        gridToggle = new ActionToggle(MainWindowActions.TOGGLE_GRID,
                GuiUtils.createIconFromSVG("images/toolbar-toggle-grid.svg"));
        gridToggle.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(gridToggle);

        rulerToggle = new ActionToggle(MainWindowActions.TOGGLE_RULER,
                GuiUtils.createIconFromSVG("images/toolbar-toggle-ruler.svg"));
        rulerToggle.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(rulerToggle);

        nameToggle = new ActionToggle(MainWindowActions.TOGGLE_NAME,
                GuiUtils.createIconFromSVG("images/toolbar-toggle-name.svg"));
        nameToggle.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(nameToggle);

        labelToggle = new ActionToggle(MainWindowActions.TOGGLE_LABEL,
                GuiUtils.createIconFromSVG("images/toolbar-toggle-label.svg"));
        labelToggle.addScriptedActionListener(mainWindow.getDefaultActionListener());
        add(labelToggle);
    }

    public void refreshToggles() {
        gridToggle.setSelected(EditorCommonSettings.getGridVisibility());
        rulerToggle.setSelected(EditorCommonSettings.getRulerVisibility());
        nameToggle.setSelected(VisualCommonSettings.getNameVisibility());
        labelToggle.setSelected(VisualCommonSettings.getLabelVisibility());
    }

}
