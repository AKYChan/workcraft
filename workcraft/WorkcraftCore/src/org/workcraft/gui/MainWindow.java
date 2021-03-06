package org.workcraft.gui;

import org.flexdock.docking.DockingConstants;
import org.flexdock.docking.DockingManager;
import org.flexdock.docking.defaults.DefaultDockingPort;
import org.flexdock.docking.defaults.StandardBorderManager;
import org.flexdock.docking.drag.effects.EffectsManager;
import org.flexdock.docking.drag.preview.GhostPreview;
import org.flexdock.docking.props.PropertyManager;
import org.flexdock.docking.state.PersistenceException;
import org.flexdock.perspective.Perspective;
import org.flexdock.perspective.PerspectiveManager;
import org.flexdock.perspective.persist.FilePersistenceHandler;
import org.flexdock.perspective.persist.PersistenceHandler;
import org.flexdock.perspective.persist.xml.XMLPersister;
import org.flexdock.plaf.common.border.ShadowBorder;
import org.workcraft.Framework;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.dom.VisualModelDescriptor;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.exceptions.OperationCancelledException;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.exceptions.VisualModelInstantiationException;
import org.workcraft.gui.dialogs.CreateWorkDialog;
import org.workcraft.gui.editor.GraphEditorPanel;
import org.workcraft.gui.layouts.MultiBorderLayout;
import org.workcraft.gui.properties.SettingsEditorDialog;
import org.workcraft.gui.tabs.ContentPanel;
import org.workcraft.gui.tabs.DockableWindow;
import org.workcraft.gui.tabs.DockingUtils;
import org.workcraft.gui.tasks.TaskFailureNotifier;
import org.workcraft.gui.tasks.TaskManagerWindow;
import org.workcraft.gui.tools.GraphEditor;
import org.workcraft.gui.workspace.Path;
import org.workcraft.gui.workspace.WorkspaceWindow;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.Format;
import org.workcraft.interop.FormatFileFilter;
import org.workcraft.interop.Importer;
import org.workcraft.tasks.ExportTask;
import org.workcraft.tasks.TaskManager;
import org.workcraft.types.ListMap;
import org.workcraft.utils.*;
import org.workcraft.workspace.FileFilters;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;

import javax.swing.*;
import javax.swing.plaf.MenuBarUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {

    private static final String FLEXDOCK_WORKSPACE = "defaultWorkspace";
    private static final String FLEXDOCK_DOCKING_PORT = "defaultDockingPort";

    private static final String CONFIG_WINDOW_MAXIMISED = "window.maximised";
    private static final String CONFIG_WINDOW_WIDTH = "window.width";
    private static final String CONFIG_WINDOW_HEIGHT = "window.height";
    private static final String CONFIG_TOOLBAR_GLOBAL_VISIBILITY = "toolbar.global.visibility";
    private static final String CONFIG_TOOLBAR_GLOBAL_POSITION = "toolbar.global.position";
    private static final String CONFIG_TOOLBAR_MODEL_VISIBILITY = "toolbar.model.visibility";
    private static final String CONFIG_TOOLBAR_MODEL_POSITION = "toolbar.model.position";
    private static final String CONFIG_TOOLBAR_CONTROL_VISIBILITY = "toolbar.tool.visibility";
    private static final String CONFIG_TOOLBAR_CONTROL_POSITION = "toolbar.tool.position";

    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 450;

    public static final String TITLE_WORKCRAFT = "Workcraft";
    public static final String TITLE_OUTPUT = "Output";
    public static final String TITLE_PROBLEMS = "Problems";
    public static final String TITLE_JAVASCRIPT = "Javascript";
    public static final String TITLE_TASKS = "Tasks";
    public static final String TITLE_WORKSPACE = "Workspace";
    public static final String TITLE_PROPERTY_EDITOR = "Property editor";
    public static final String TITLE_MODEL_TOOLS = "Model tools";
    public static final String TITLE_TOOL_CONTROLS = "Tool controls";
    public static final String TITLE_PLACEHOLDER = "";
    public static final String PREFIX_DOCUMENT = "Document";

    private MultiBorderLayout layout;
    private JPanel content;

    private DefaultDockingPort defaultDockingPort;
    private DockableWindow outputDockable;
    private DockableWindow propertyEditorDockable;
    private DockableWindow toolControlsDockable;
    private DockableWindow documentPlaceholder;

    private OutputWindow outputWindow;
    private ErrorWindow errorWindow;
    private JavaScriptWindow javaScriptWindow;
    private PropertyEditorWindow propertyEditorWindow;
    private ToolControlsWindow toolControlsWindow;
    private WorkspaceWindow workspaceWindow;

    private final ListMap<WorkspaceEntry, DockableWindow> weWindowsMap = new ListMap<>();
    private final LinkedList<DockableWindow> utilityWindows = new LinkedList<>();

    private GraphEditorPanel editorInFocus;
    private MainMenu mainMenu;
    private ToolBar globalToolbar;
    private JToolBar modelToolbar;
    private JToolBar controlToolbar;

    public MainWindow() {
        super();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Framework.getInstance().shutdown();
            }
        });
    }

    private void createWindows() {
        workspaceWindow = new WorkspaceWindow();
        workspaceWindow.setVisible(true);

        outputWindow = new OutputWindow();
        outputWindow.captureStream();

        errorWindow = new ErrorWindow();
        errorWindow.captureStream();

        javaScriptWindow = new JavaScriptWindow();

        propertyEditorWindow = new PropertyEditorWindow();
        toolControlsWindow = new ToolControlsWindow();
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
    }

    public GraphEditorPanel createEditorWindow(final WorkspaceEntry we) {
        final GraphEditorPanel editor = new GraphEditorPanel(we);
        String title = we.getTitleAndModel();
        final DockableWindow editorWindow;
        String persistentID = PREFIX_DOCUMENT + we.getWorkspacePath();
        if (weWindowsMap.isEmpty()) {
            editorWindow = DockingUtils.createEditorDockable(editor, title,
                    documentPlaceholder, persistentID);

            DockingManager.close(documentPlaceholder);
            DockingManager.unregisterDockable(documentPlaceholder);
            utilityWindows.remove(documentPlaceholder);
        } else {
            Collection<DockableWindow> editorWindows = weWindowsMap.values();
            DockingUtils.unmaximise(editorWindows);
            DockableWindow firstEditorWindow = editorWindows.iterator().next();
            editorWindow = DockingUtils.createEditorDockable(editor, title,
                    firstEditorWindow, persistentID);
        }
        editorWindow.addTabListener(new EditorWindowDockableListener(editor));
        weWindowsMap.put(we, editorWindow);
        requestFocus(editor);
        setWorkActionsEnableness(true);
        editor.zoomFit();
        return editor;
    }

    private void registerUtilityWindow(DockableWindow dockableWindow) {
        if (!defaultDockingPort.getDockables().contains(dockableWindow)) {
            dockableWindow.setClosed(true);
            DockingManager.close(dockableWindow);
        }
        mainMenu.registerUtilityWindow(dockableWindow);
        utilityWindows.add(dockableWindow);
    }

    public void startup() {
        MainWindowIconManager.apply(this);
        JDialog.setDefaultLookAndFeelDecorated(true);
        setTitle(TITLE_WORKCRAFT);

        // Create main menu
        mainMenu = new MainMenu();
        MenuBarUI menuUI = mainMenu.getUI();
        setJMenuBar(mainMenu);
        mainMenu.updateRecentMenu();

        // Tweak look-and-feel
        SilverOceanTheme.enable();
        LookAndFeelHelper.setDefaultLookAndFeel();
        SwingUtilities.updateComponentTreeUI(this);
        if (DesktopApi.getOs().isMac()) {
            // Menu UI needs to be restored for OSX (global menu Look-and-Feel)
            mainMenu.setUI(menuUI);
        }

        // Create content panel and docking ports
        layout = new MultiBorderLayout();
        content = new JPanel(layout);
        setContentPane(content);
        defaultDockingPort = new DefaultDockingPort(FLEXDOCK_DOCKING_PORT);
        content.add(defaultDockingPort, BorderLayout.CENTER);
        StandardBorderManager borderManager = new StandardBorderManager(new ShadowBorder());
        defaultDockingPort.setBorderManager(borderManager);

        // Create toolbars
        globalToolbar = new ToolBar();
        modelToolbar = new JToolBar(TITLE_MODEL_TOOLS);
        controlToolbar = new JToolBar(TITLE_TOOL_CONTROLS);
        mainMenu.registerToolbar(globalToolbar);
        mainMenu.registerToolbar(modelToolbar);
        mainMenu.registerToolbar(controlToolbar);
        loadToolbarParametersFromConfig();

        // Create dockable windows
        createWindows();
        createDockingLayout();
        loadWindowGeometryFromConfig();

        // Display window in its default state
        setVisible(true);
        DockingUtils.updateHeaders(defaultDockingPort);
        DockingManager.display(outputDockable);
        utilityWindows.add(documentPlaceholder);
        setWorkActionsEnableness(false);
        updateDockableWindowVisibility();

        new Thread(() -> {
            // Hack to fix the annoying delay occurring when createGlyphVector is called for the first time
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
            FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
            font.createGlyphVector(frc, TITLE_PLACEHOLDER);
            // Force SVG rendering classes to load
            GuiUtils.createIconFromSVG("images/icon.svg");
        }).start();
    }

    private void setWorkActionsEnableness(boolean enable) {
        mainMenu.setExportMenuState(enable);
        MainWindowActions.MERGE_WORK_ACTION.setEnabled(enable);
        MainWindowActions.CLOSE_ACTIVE_EDITOR_ACTION.setEnabled(enable);
        MainWindowActions.CLOSE_ALL_EDITORS_ACTION.setEnabled(enable);
        MainWindowActions.SAVE_WORK_ACTION.setEnabled(enable);
        MainWindowActions.SAVE_WORK_AS_ACTION.setEnabled(enable);
        MainWindowActions.EDIT_DELETE_ACTION.setEnabled(enable);
        MainWindowActions.EDIT_SELECT_ALL_ACTION.setEnabled(enable);
        MainWindowActions.EDIT_SELECT_INVERSE_ACTION.setEnabled(enable);
        MainWindowActions.EDIT_SELECT_NONE_ACTION.setEnabled(enable);
        if (!enable) {
            MainWindowActions.EDIT_UNDO_ACTION.setEnabled(false);
            MainWindowActions.EDIT_REDO_ACTION.setEnabled(false);
            MainWindowActions.EDIT_CUT_ACTION.setEnabled(false);
            MainWindowActions.EDIT_COPY_ACTION.setEnabled(false);
            MainWindowActions.EDIT_PASTE_ACTION.setEnabled(false);
        }
        MainWindowActions.VIEW_ZOOM_IN.setEnabled(enable);
        MainWindowActions.VIEW_ZOOM_OUT.setEnabled(enable);
        MainWindowActions.VIEW_ZOOM_DEFAULT.setEnabled(enable);
        MainWindowActions.VIEW_PAN_CENTER.setEnabled(enable);
        MainWindowActions.VIEW_ZOOM_FIT.setEnabled(enable);
        MainWindowActions.VIEW_PAN_LEFT.setEnabled(enable);
        MainWindowActions.VIEW_PAN_UP.setEnabled(enable);
        MainWindowActions.VIEW_PAN_RIGHT.setEnabled(enable);
        MainWindowActions.VIEW_PAN_DOWN.setEnabled(enable);
    }

    public void updateMainMenuState(boolean canModify) {
        mainMenu.updateCommandsMenuState(canModify);
    }

    public void toggleDockableWindowMaximized(DockableWindow dockableWindow) {
        if (dockableWindow != null) {
            DockingManager.toggleMaximized(dockableWindow);
            dockableWindow.setMaximized(!dockableWindow.isMaximized());
        }
    }

    public void closeDockableWindow(DockableWindow dockableWindow) {
        if (dockableWindow != null) {
            GraphEditorPanel editor = getGraphEditorPanel(dockableWindow);
            if (editor != null) {
                closeDockableEditorWindow(dockableWindow, editor);
            } else {
                closeDockableUtilityWindow(dockableWindow);
            }
        }
    }

    private void closeDockableEditorWindow(DockableWindow editorWindow, GraphEditorPanel editor) {
        WorkspaceEntry we = editor.getWorkspaceEntry();
        try {
            saveWorkBeforeClose(we);
            // Un-maximise the editor window
            if (DockingManager.isMaximized(editorWindow)) {
                toggleDockableWindowMaximized(editorWindow);
            }
            // Remove the window tab listeners
            for (DockableWindow dockableWindow : weWindowsMap.get(we)) {
                if (dockableWindow == editorWindow) {
                    dockableWindow.clearTabListeners();
                }
            }
            // Remove the window and close its workspace entry
            weWindowsMap.remove(we, editorWindow);
            if (weWindowsMap.get(we).isEmpty()) {
                Framework.getInstance().closeWork(we);
            }
            // Remove commands menu and update property window
            if (editorInFocus == editor) {
                mainMenu.removeCommandsMenu();
                editorInFocus = null;
                setPropertyEditorTitle(TITLE_PROPERTY_EDITOR);
            }
            // If no more editor windows left, then activate the document placeholder
            if (weWindowsMap.isEmpty()) {
                DockingManager.registerDockable(documentPlaceholder);
                DockingManager.dock(documentPlaceholder, editorWindow, DockingConstants.CENTER_REGION);
                utilityWindows.add(documentPlaceholder);
                setWorkActionsEnableness(false);
                modelToolbar.removeAll();
                controlToolbar.removeAll();
                propertyEditorWindow.clear();
                toolControlsWindow.setContent(null);
                displayDockableWindow(propertyEditorDockable);
                closeDockableWindow(toolControlsDockable);
                setPropertyEditorTitle(TITLE_PROPERTY_EDITOR);
            }
            // Unregister window from docking manager
            DockingManager.close(editorWindow);
            DockingManager.unregisterDockable(editorWindow);
            editorWindow.setClosed(true);
        } catch (OperationCancelledException e) {
        }
    }

    private void saveWorkBeforeClose(WorkspaceEntry we) throws OperationCancelledException {
        if (we.isChanged()) {
            requestFocus(we);
            String title = we.getTitle();
            int result = JOptionPane.showConfirmDialog(this,
                    "Document '" + title + "' has unsaved changes.\n" + "Save before closing?",
                    "Close work", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

            switch (result) {
            case JOptionPane.YES_OPTION:
                saveWork(we);
                break;
            case JOptionPane.NO_OPTION:
                break;
            default:
                throw new OperationCancelledException("Operation cancelled by user.");
            }
        }
    }

    private void setToolbarVisibility(JToolBar toolbar, boolean visibility) {
        mainMenu.setToolbarVisibility(toolbar, visibility);
        toolbar.setVisible(visibility);
    }

    private void closeDockableUtilityWindow(DockableWindow dockableWindow) {
        mainMenu.setWindowVisibility(dockableWindow, false);
        DockingManager.close(dockableWindow);
        dockableWindow.setClosed(true);
    }

    public void displayDockableWindow(DockableWindow window) {
        DockingManager.display(window);
        window.setClosed(false);
        mainMenu.setWindowVisibility(window, true);
    }

    public void toggleDockableWindow(DockableWindow window) {
        if (window.isClosed()) {
            displayDockableWindow(window);
        } else {
            closeDockableWindow(window);
        }
    }

    public DisplayMode getDisplayMode() {
        return getGraphicsConfiguration().getDevice().getDisplayMode();
    }

    private GraphEditorPanel getGraphEditorPanel(DockableWindow dockableWindow) {
        JComponent content = dockableWindow.getComponent().getContent();
        return (content instanceof GraphEditorPanel) ? (GraphEditorPanel) content : null;
    }

    public DockableWindow getEditorWindow(GraphEditorPanel editor) {
        for (DockableWindow dockableWindow : weWindowsMap.values()) {
            if (editor == getGraphEditorPanel(dockableWindow)) {
                return dockableWindow;
            }
        }
        return null;
    }

    private void createDockingLayout() {
        // Setup docking manger (should go before perspective manager for correctly restoring window position).
        EffectsManager.setPreview(new GhostPreview());
        DockingManager.setFloatingEnabled(true);
        DockingManager.setAutoPersist(true);
        PropertyManager.getDockingPortRoot().setTabPlacement(SwingConstants.TOP);

        // Set default perspective.
        PerspectiveManager pm = PerspectiveManager.getInstance();
        pm.add(new Perspective(FLEXDOCK_WORKSPACE, FLEXDOCK_WORKSPACE));
        pm.setCurrentPerspective(FLEXDOCK_WORKSPACE, true);

        // Configure perspective manager (should go after docking manager for correctly restoring window position).
        PerspectiveManager.setRestoreFloatingOnLoad(true);
        File file = new File(Framework.UILAYOUT_FILE_PATH);
        PersistenceHandler persister = new FilePersistenceHandler(file, XMLPersister.newDefaultInstance());
        PerspectiveManager.setPersistenceHandler(persister);

        try {
            DockingManager.loadLayoutModel();
        } catch (IOException | PersistenceException e) {
            LogUtils.logWarning("Window layout could not be loaded from '" + file.getAbsolutePath() + "'.");
        }

        float xSplit = 0.87f;
        float ySplit = 0.8f;

        documentPlaceholder = DockingUtils.createPlaceholderDockable(new DocumentPlaceholder(),
                TITLE_PLACEHOLDER, defaultDockingPort);

        DockableWindow workspaceDockable = DockingUtils.createUtilityDockable(workspaceWindow,
                TITLE_WORKSPACE, documentPlaceholder, DockingManager.EAST_REGION, xSplit);

        propertyEditorDockable = DockingUtils.createUtilityDockable(propertyEditorWindow,
                TITLE_PROPERTY_EDITOR, workspaceDockable, DockingManager.NORTH_REGION, ySplit);

        toolControlsDockable = DockingUtils.createUtilityDockable(toolControlsWindow,
                TITLE_TOOL_CONTROLS, propertyEditorDockable, DockingManager.SOUTH_REGION, 0.5f);

        outputDockable = DockingUtils.createUtilityDockable(outputWindow,
                TITLE_OUTPUT, documentPlaceholder, DockingManager.SOUTH_REGION, ySplit);

        DockableWindow errorDockable = DockingUtils.createUtilityDockable(errorWindow,
                TITLE_PROBLEMS, outputDockable);

        DockableWindow javaScriptDockable = DockingUtils.createUtilityDockable(javaScriptWindow,
                TITLE_JAVASCRIPT, outputDockable);

        DockableWindow tasksDockable = DockingUtils.createUtilityDockable(new TaskManagerWindow(),
                TITLE_TASKS, outputDockable);

        registerUtilityWindow(outputDockable);
        registerUtilityWindow(errorDockable);
        registerUtilityWindow(javaScriptDockable);
        registerUtilityWindow(tasksDockable);
        registerUtilityWindow(propertyEditorDockable);
        registerUtilityWindow(toolControlsDockable);
        registerUtilityWindow(workspaceDockable);

        // FIXME: Restoring previously saved layout does not work as expected:
        // "default" and "restored" layouts interfere with each other, which does not look nice.
        //DockingManager.restoreLayout();
    }

    public void shutdown() throws OperationCancelledException {
        closeEditorWindows();
        if (!weWindowsMap.isEmpty()) {
            throw new OperationCancelledException("Operation cancelled by user.");
        }

        Workspace workspace = Framework.getInstance().getWorkspace();
        if (workspace.isChanged() && !workspace.isTemporary()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "!Current workspace has unsaved changes.\n" + "Save before closing?",
                    "Close work", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

            switch (result) {
            case JOptionPane.YES_OPTION:
                workspaceWindow.saveWorkspace();
                break;
            case JOptionPane.NO_OPTION:
                break;
            default:
                throw new OperationCancelledException("Operation cancelled by user.");
            }
        }
        saveWindowGeometryToConfig();
        saveToolbarParametersToConfig();

        content.remove(defaultDockingPort);

        outputWindow.releaseStream();
        errorWindow.releaseStream();
        setVisible(false);
    }

    private void loadToolbarParametersFromConfig() {
        loadToolbarParametersFromConfig(globalToolbar,
                CONFIG_TOOLBAR_GLOBAL_VISIBILITY,
                CONFIG_TOOLBAR_GLOBAL_POSITION);

        loadToolbarParametersFromConfig(modelToolbar,
                CONFIG_TOOLBAR_MODEL_VISIBILITY,
                CONFIG_TOOLBAR_MODEL_POSITION);

        loadToolbarParametersFromConfig(controlToolbar,
                CONFIG_TOOLBAR_CONTROL_VISIBILITY,
                CONFIG_TOOLBAR_CONTROL_POSITION);
    }

    private void loadToolbarParametersFromConfig(JToolBar toolbar, String keyVisibility, String keyPosition) {
        final Framework framework = Framework.getInstance();

        boolean visible = true;
        String visibleVal = framework.getConfigVar(keyVisibility, false);
        if (visibleVal != null) {
            visible = Boolean.valueOf(visibleVal);
        }
        setToolbarVisibility(toolbar, visible);

        String position = framework.getConfigVar(keyPosition, false);
        if (position == null) {
            position = BorderLayout.NORTH;
        }
        if (BorderLayout.EAST.equals(position) || BorderLayout.WEST.equals(position)) {
            toolbar.setOrientation(ToolBar.VERTICAL);
        } else {
            toolbar.setOrientation(ToolBar.HORIZONTAL);
        }
        add(toolbar, position);
    }

    private void saveToolbarParametersToConfig() {
        saveToolbarParametersToConfig(globalToolbar,
                CONFIG_TOOLBAR_GLOBAL_VISIBILITY,
                CONFIG_TOOLBAR_GLOBAL_POSITION);

        saveToolbarParametersToConfig(modelToolbar,
                CONFIG_TOOLBAR_MODEL_VISIBILITY,
                CONFIG_TOOLBAR_MODEL_POSITION);

        saveToolbarParametersToConfig(controlToolbar,
                CONFIG_TOOLBAR_CONTROL_VISIBILITY,
                CONFIG_TOOLBAR_CONTROL_POSITION);
    }

    private void saveToolbarParametersToConfig(JToolBar toolbar, String keyVisibility, String keyPosition) {
        final Framework framework = Framework.getInstance();

        String visibleVal = String.valueOf(toolbar.isVisible());
        framework.setConfigVar(keyVisibility, visibleVal, false);

        Object positionVal = layout.getConstraints(toolbar);
        if (positionVal instanceof String) {
            framework.setConfigVar(keyPosition, (String) positionVal, false);
        }
    }

    public void loadWindowGeometryFromConfig() {
        final Framework framework = Framework.getInstance();
        String maximisedStr = framework.getConfigVar(CONFIG_WINDOW_MAXIMISED, false);
        String widthStr = framework.getConfigVar(CONFIG_WINDOW_WIDTH, false);
        String heightStr = framework.getConfigVar(CONFIG_WINDOW_HEIGHT, false);

        boolean maximised = (maximisedStr == null) ? true : Boolean.parseBoolean(maximisedStr);
        setExtendedState(maximised ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);

        DisplayMode mode = getDisplayMode();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int width = mode.getWidth() - insets.right - insets.left;
        int height = mode.getHeight() - insets.top - insets.bottom;
        if ((widthStr != null) && (heightStr != null)) {
            width = Integer.parseInt(widthStr);
            if (width < MIN_WIDTH) {
                width = MIN_WIDTH;
            }
            height = Integer.parseInt(heightStr);
            if (height < MIN_HEIGHT) {
                height = MIN_HEIGHT;
            }
        }
        setSize(width, height);
    }

    public void saveWindowGeometryToConfig() {
        final Framework framework = Framework.getInstance();
        boolean maximised = (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
        framework.setConfigVar(CONFIG_WINDOW_MAXIMISED, Boolean.toString(maximised), false);
        framework.setConfigVar(CONFIG_WINDOW_WIDTH, Integer.toString(getWidth()), false);
        framework.setConfigVar(CONFIG_WINDOW_HEIGHT, Integer.toString(getHeight()), false);
    }

    public void createWork() throws OperationCancelledException {
        createWork(Path.empty());
    }

    public void createWork(Path<String> directory) throws OperationCancelledException {
        CreateWorkDialog dialog = new CreateWorkDialog(this);
        if (!dialog.reveal()) {
            throw new OperationCancelledException("Create operation cancelled by user.");
        }
        ModelDescriptor md = dialog.getSelectedModel();
        if (md == null) {
            DialogUtils.showError("Math model is not defined for '" + md.getDisplayName() + "'.");
            return;
        }
        try {
            VisualModelDescriptor vmd = md.getVisualModelDescriptor();
            if (vmd == null) {
                DialogUtils.showError("Visual model is not defined for '" + md.getDisplayName() + "'.");
                return;
            }
            MathModel mathModel = md.createMathModel();
            VisualModel visualModel = vmd.create(mathModel);
            ModelEntry me = new ModelEntry(md, visualModel);
            final Framework framework = Framework.getInstance();
            WorkspaceEntry we = framework.createWork(me, directory, null);
            we.setChanged(false);
        } catch (VisualModelInstantiationException e) {
            DialogUtils.showError(e.getMessage());
        }
    }

    public void requestFocus(final WorkspaceEntry we) {
        for (DockableWindow window: weWindowsMap.get(we)) {
            Container parent = window.getComponent().getParent();
            if (parent instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) parent;
                tabbedPane.setSelectedComponent(window.getComponent());
                break;
            }
        }
    }

    public void requestFocus(final GraphEditorPanel editor) {
        if (editorInFocus != editor) {
            editorInFocus = editor;
            editorInFocus.updateToolsView();
            editorInFocus.updatePropertyView();
            updateDockableWindowVisibility();

            WorkspaceEntry we = editorInFocus.getWorkspaceEntry();
            mainMenu.setMenuForWorkspaceEntry(we);
            Framework.getInstance().updateJavaScript(we);
        }
        SwingUtilities.invokeLater(() -> {
            if (editorInFocus != null) {
                editorInFocus.requestFocus();
            }
        });
    }

    public void updateDockableWindowVisibility() {
        // To preserve the layout, first display both the property editor
        // and the tool controls. Only after that close the one of them (if empty).
        displayDockableWindow(propertyEditorDockable);
        displayDockableWindow(toolControlsDockable);
        if (toolControlsWindow.isEmpty()) {
            closeDockableWindow(toolControlsDockable);
        } else if (propertyEditorWindow.isEmpty()) {
            closeDockableWindow(propertyEditorDockable);
        }
    }

    public JFileChooser createOpenDialog(String title, boolean multiSelection, boolean allowWorkFiles, Format format) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setDialogTitle(title);
        boolean allowAllFileFilter = true;
        if (allowWorkFiles) {
            fc.setFileFilter(FileFilters.DOCUMENT_FILES);
            allowAllFileFilter = false;
        }
        if (format != null) {
            fc.addChoosableFileFilter(new FormatFileFilter(format));
            allowAllFileFilter = false;
        }
        GuiUtils.sizeFileChooserToScreen(fc, getDisplayMode());
        fc.setCurrentDirectory(Framework.getInstance().getLastDirectory());
        fc.setAcceptAllFileFilterUsed(allowAllFileFilter);
        fc.setMultiSelectionEnabled(multiSelection);
        return fc;
    }

    public JFileChooser createSaveDialog(String title, File file, Format format) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle(title);
        GuiUtils.sizeFileChooserToScreen(fc, getDisplayMode());
        // Set file name
        fc.setSelectedFile(file);
        // Set working directory
        if (file.exists()) {
            fc.setCurrentDirectory(file.getParentFile());
        } else {
            fc.setCurrentDirectory(Framework.getInstance().getLastDirectory());
        }
        // Set file filters
        fc.setAcceptAllFileFilterUsed(false);
        if (format == null) {
            fc.setFileFilter(FileFilters.DOCUMENT_FILES);
        } else {
            fc.setFileFilter(new FormatFileFilter(format));
        }
        return fc;
    }

    public void openWork() throws OperationCancelledException {
        JFileChooser fc = createOpenDialog("Open work file(s)", true, true, null);
        if (fc.showDialog(this, "Open") == JFileChooser.APPROVE_OPTION) {
            final HashSet<WorkspaceEntry> newWorkspaceEntries = new HashSet<>();
            for (File file : fc.getSelectedFiles()) {
                File workFile = file;
                String path = file.getPath();
                if (!FileFilters.isWorkPath(path)) {
                    workFile = new File(path + FileFilters.DOCUMENT_EXTENSION);
                }
                WorkspaceEntry we = openWork(workFile);
                if (we != null) {
                    newWorkspaceEntries.add(we);
                }
            }
            // FIXME: Go through the newly open works and update their zoom,
            // in case tabs appeared and changed the viewport size.
            SwingUtilities.invokeLater(() -> {
                for (WorkspaceEntry we: newWorkspaceEntries) {
                    for (DockableWindow window: weWindowsMap.get(we)) {
                        GraphEditor editor = getGraphEditorPanel(window);
                        if (editor != null) {
                            editor.zoomFit();
                        }
                    }
                }
            });
        } else {
            throw new OperationCancelledException("Open operation cancelled by user.");
        }
    }

    public WorkspaceEntry openWork(File file) {
        final Framework framework = Framework.getInstance();
        WorkspaceEntry we = null;
        if (FileUtils.checkAvailability(file, null, true)) {
            try {
                we = framework.loadWork(file);
                framework.setLastDirectory(file);
                framework.pushRecentFilePath(file);
                mainMenu.updateRecentMenu();
            } catch (DeserialisationException e) {
                DialogUtils.showError("A problem was encountered while trying to load '"
                        + file.getPath() + "'.\n" + e.getMessage());
            }
        }
        return we;
    }

    public void mergeWork() throws OperationCancelledException {
        JFileChooser fc = createOpenDialog("Merge work file(s)", true, true, null);
        if (fc.showDialog(this, "Merge") == JFileChooser.APPROVE_OPTION) {
            for (File file: fc.getSelectedFiles()) {
                File workFile = file;
                String path = file.getPath();
                if (!FileFilters.isWorkPath(path)) {
                    workFile = new File(path + FileFilters.DOCUMENT_EXTENSION);
                }
                mergeWork(workFile);
            }
        } else {
            throw new OperationCancelledException("Merge operation cancelled by user.");
        }
    }

    public void mergeWork(File file) {
        if (editorInFocus == null) {
            openWork(file);
        } else {
            try {
                final Framework framework = Framework.getInstance();
                WorkspaceEntry we = editorInFocus.getWorkspaceEntry();
                framework.mergeWork(we, file);
            } catch (DeserialisationException e) {
                DialogUtils.showError("A problem was encountered while trying to merge '" + file.getPath() + "'.\n" + e.getMessage());
            }
        }
    }

    public void saveWork() throws OperationCancelledException {
        if (editorInFocus != null) {
            saveWork(editorInFocus.getWorkspaceEntry());
        } else {
            System.out.println("No editor in focus");
        }
    }

    public void saveWorkAs() throws OperationCancelledException {
        if (editorInFocus != null) {
            saveWorkAs(editorInFocus.getWorkspaceEntry());
        } else {
            System.err.println("No editor in focus");
        }
    }

    public void saveWork(WorkspaceEntry we) throws OperationCancelledException {
        Workspace workspace = Framework.getInstance().getWorkspace();
        File file = workspace.getFile(we);
        if ((file == null) || !file.exists()) {
            saveWorkAs(we);
        } else {
            saveWork(we, file.getPath());
        }
    }

    public void saveWorkAs(WorkspaceEntry we) throws OperationCancelledException {
        Workspace workspace = Framework.getInstance().getWorkspace();
        File file = workspace.getFile(we);
        if (file == null) {
            file = new File(getFileNameForCurrentWork());
        }
        JFileChooser fc = createSaveDialog("Save work as", file, null);
        String path = ExportUtils.getValidSavePath(fc, null);
        saveWork(we, path);
    }

    private void saveWork(WorkspaceEntry we, String path) {
        if (we.getModelEntry() == null) {
            throw new RuntimeException(
                    "Cannot save workspace entry - it does not have an associated Workcraft model.");
        }
        Framework framework = Framework.getInstance();
        try {
            framework.saveWork(we, path);
        } catch (SerialisationException e) {
            DialogUtils.showError(e.getMessage());
        }
        File file = framework.getWorkspace().getFile(we);
        framework.setLastDirectory(file);
        framework.pushRecentFilePath(file);
        mainMenu.updateRecentMenu();
    }

    public void importFrom(Importer importer) {
        Format format = importer.getFormat();
        JFileChooser fc = createOpenDialog("Import model(s)", true, false, format);
        if (fc.showDialog(this, "Open") == JFileChooser.APPROVE_OPTION) {
            for (File file : fc.getSelectedFiles()) {
                importFrom(importer, file);
            }
        }
    }

    private void importFrom(Importer importer, File file) {
        if (FileUtils.checkAvailability(file, null, true)
                && FormatFileFilter.checkFileFormat(file, importer.getFormat())) {

            try {
                ModelEntry me = ImportUtils.importFromFile(importer, file);
                String title = me.getMathModel().getTitle();
                if ((title == null) || title.isEmpty()) {
                    title = FileUtils.getFileNameWithoutExtension(file);
                    me.getMathModel().setTitle(title);
                }
                final Framework framework = Framework.getInstance();
                framework.createWork(me, Path.empty(), file.getName());
                framework.setLastDirectory(file);
            } catch (IOException | DeserialisationException | OperationCancelledException e) {
                DialogUtils.showError(e.getMessage());
            }
        }
    }

    public void export(Exporter exporter) {
        Format format = exporter.getFormat();
        String title = "Export as " + format.getDescription();
        File file = new File(getFileNameForCurrentWork());
        JFileChooser fc = createSaveDialog(title, file, format);
        try {
            String path = ExportUtils.getValidSavePath(fc, format);
            VisualModel model = editorInFocus.getModel();
            ExportTask exportTask = new ExportTask(exporter, model, new File(path));
            final Framework framework = Framework.getInstance();
            final TaskManager taskManager = framework.getTaskManager();
            String description = "Exporting " + title;
            final TaskFailureNotifier monitor = new TaskFailureNotifier(description);
            taskManager.queue(exportTask, description, monitor);
            framework.setLastDirectory(fc.getCurrentDirectory());
        } catch (OperationCancelledException e) {
        }
    }

    private String getFileNameForCurrentWork() {
        String fileName = "";
        if (editorInFocus != null) {
            WorkspaceEntry we = editorInFocus.getWorkspaceEntry();
            if (we != null) {
                fileName = we.getFileName();
            }
        }
        return fileName;
    }

    public void refreshWorkspaceEntryTitle(WorkspaceEntry we, boolean updateHeaders) {
        for (DockableWindow window: weWindowsMap.get(we)) {
            String title = we.getTitleAndModel();
            window.setTitle(title);
        }
        if (updateHeaders) {
            DockingUtils.updateHeaders(defaultDockingPort);
        }
    }

    public void setPropertyEditorTitle(String title) {
        propertyEditorDockable.setTitle(title);
        DockingUtils.updateHeaders(defaultDockingPort);
    }

    public List<GraphEditorPanel> getEditors(WorkspaceEntry we) {
        ArrayList<GraphEditorPanel> result = new ArrayList<>();
        for (DockableWindow window: weWindowsMap.get(we)) {
            result.add(getGraphEditorPanel(window));
        }
        return result;
    }

    public GraphEditorPanel getEditor(final WorkspaceEntry we) {
        GraphEditorPanel result = this.getCurrentEditor();
        if ((result == null) || (result.getWorkspaceEntry() != we)) {
            final List<GraphEditorPanel> editors = getEditors(we);
            if (editors.isEmpty()) {
                result = this.createEditorWindow(we);
            } else {
                result = editors.get(0);
                this.requestFocus(result);
            }
        }
        return result;
    }

    public GraphEditorPanel getCurrentEditor() {
        return editorInFocus;
    }

    public Toolbox getToolbox(final WorkspaceEntry we) {
        GraphEditorPanel editor = getEditor(we);
        return (editor == null) ? null : editor.getToolBox();
    }

    public Toolbox getCurrentToolbox() {
        GraphEditorPanel editor = getCurrentEditor();
        return (editor == null) ? null : editor.getToolBox();
    }

    public void closeActiveEditor() {
        for (WorkspaceEntry we: weWindowsMap.keySet()) {
            for (DockableWindow window: weWindowsMap.get(we)) {
                ContentPanel contentPanel = window.getComponent();
                if ((contentPanel != null) && (contentPanel.getContent() == editorInFocus)) {
                    closeDockableWindow(window);
                    return;
                }
            }
        }
    }

    public void closeEditorWindows() {
        LinkedHashSet<DockableWindow> windowsToClose = new LinkedHashSet<>();
        for (WorkspaceEntry we: weWindowsMap.keySet()) {
            for (DockableWindow window: weWindowsMap.get(we)) {
                windowsToClose.add(window);
            }
        }
        for (DockableWindow dockableWindow: windowsToClose) {
            if (DockingManager.isMaximized(dockableWindow)) {
                toggleDockableWindowMaximized(dockableWindow);
            }
        }
        for (DockableWindow window: windowsToClose) {
            try {
                closeDockableWindow(window);
            } catch (ClassCastException e) {
                // FIXME: Flexdock may throw ClassCast exception when closing Workcraft.
            }
        }
    }

    public void closeEditors(WorkspaceEntry we) {
        for (DockableWindow window: new ArrayList<>(weWindowsMap.get(we))) {
            closeDockableWindow(window);
        }
    }

    public void undo() {
        if (editorInFocus != null) {
            editorInFocus.getWorkspaceEntry().undo();
            editorInFocus.forceRedraw();
            editorInFocus.requestFocus();
        }
    }

    public void redo() {
        if (editorInFocus != null) {
            editorInFocus.getWorkspaceEntry().redo();
            editorInFocus.forceRedraw();
            editorInFocus.requestFocus();
        }
    }

    public void cut() {
        if (editorInFocus != null) {
            editorInFocus.getWorkspaceEntry().cut();
            editorInFocus.forceRedraw();
            editorInFocus.requestFocus();
        }
    }

    public void copy() {
        if (editorInFocus != null) {
            editorInFocus.getWorkspaceEntry().copy();
            editorInFocus.forceRedraw();
            editorInFocus.requestFocus();
        }
    }

    public void paste() {
        if (editorInFocus != null) {
            editorInFocus.getWorkspaceEntry().paste();
            editorInFocus.forceRedraw();
            editorInFocus.requestFocus();
        }
    }

    public void delete() {
        if (editorInFocus != null) {
            editorInFocus.getWorkspaceEntry().delete();
            editorInFocus.forceRedraw();
            editorInFocus.requestFocus();
        }
    }

    public void selectAll() {
        if (editorInFocus != null) {
            VisualModel visualModel = editorInFocus.getWorkspaceEntry().getModelEntry().getVisualModel();
            visualModel.selectAll();
            editorInFocus.requestFocus();
        }
    }

    public void selectNone() {
        if (editorInFocus != null) {
            VisualModel visualModel = editorInFocus.getWorkspaceEntry().getModelEntry().getVisualModel();
            visualModel.selectNone();
            editorInFocus.requestFocus();
        }
    }

    public void selectInverse() {
        if (editorInFocus != null) {
            VisualModel visualModel = editorInFocus.getWorkspaceEntry().getModelEntry().getVisualModel();
            visualModel.selectInverse();
            editorInFocus.requestFocus();
        }
    }

    public void editSettings() {
        SettingsEditorDialog dialog = new SettingsEditorDialog(this);
        if (dialog.reveal()) {
            mainMenu.setMenuForWorkspaceEntry(editorInFocus.getWorkspaceEntry());
            for (WorkspaceEntry we: weWindowsMap.keySet()) {
                refreshWorkspaceEntryTitle(we, false);
            }
            DockingUtils.updateHeaders(defaultDockingPort);
            globalToolbar.refreshToggles();
        }
    }

    public void resetLayout() {
        if (DialogUtils.showConfirmWarning(
                "This will close all works and reset the GUI to the default layout.\n\n"
                        + "Are you sure you want to do this?", "Reset layout", false)) {
            try {
                final Framework framework = Framework.getInstance();
                framework.shutdownGUI();
                new File(Framework.UILAYOUT_FILE_PATH).delete();
                framework.startGUI();
            } catch (OperationCancelledException e) {
            }
        }
    }

    public PropertyEditorWindow getPropertyView() {
        return propertyEditorWindow;
    }

    public ToolControlsWindow getControlsView() {
        return toolControlsWindow;
    }

    public JToolBar getModelToolbar() {
        return modelToolbar;
    }

    public JToolBar getControlToolbar() {
        return controlToolbar;
    }

    public WorkspaceWindow getWorkspaceView() {
        return workspaceWindow;
    }

}
