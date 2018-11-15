package org.workcraft.gui.graph.tools;

import org.workcraft.Framework;
import org.workcraft.NodeTransformer;
import org.workcraft.commands.Command;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.*;
import org.workcraft.dom.visual.connections.DefaultAnchorGenerator;
import org.workcraft.gui.DesktopApi;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.actions.ActionMenuItem;
import org.workcraft.gui.actions.PopupToolAction;
import org.workcraft.gui.events.GraphEditorKeyEvent;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.Viewport;
import org.workcraft.gui.graph.editors.AbstractInplaceEditor;
import org.workcraft.gui.graph.editors.LabelInplaceEditor;
import org.workcraft.plugins.shared.CommonDecorationSettings;
import org.workcraft.util.Commands;
import org.workcraft.util.GUI;
import org.workcraft.util.Hierarchy;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class SelectionTool extends AbstractGraphEditorTool {

    private enum DrugState { NONE, MOVE, SELECT };
    private enum SelectionMode { NONE, ADD, REMOVE, REPLACE };

    private DrugState dragState = DrugState.NONE;
    private boolean ignoreMouseButton1 = false;
    private boolean ignoreMouseButton3 = false;

    private Point2D offset;
    private Set<Point2D> snaps = new HashSet<>();
    private final DefaultAnchorGenerator anchorGenerator = new DefaultAnchorGenerator();

    private final LinkedHashSet<VisualNode> selected = new LinkedHashSet<>();
    private SelectionMode selectionMode = SelectionMode.NONE;
    private Rectangle2D selectionBox = null;

    private Point2D currentMousePosition = null;
    private VisualNode currentNode = null;
    private Collection<VisualNode> currentNodes = null;

    private boolean enableGroupping = true;
    private boolean enablePaging = true;
    private boolean enableFlipping = true;
    private boolean enableRotating = true;

    public SelectionTool() {
        this(true, true, true, true);
    }

    public SelectionTool(boolean enableGroupping, boolean enablePaging, boolean enableFlipping, boolean enableRotating) {
        super();
        this.enableGroupping = enableGroupping;
        this.enablePaging = enablePaging;
        this.enableFlipping = enableFlipping;
        this.enableRotating = enableRotating;
    }

    @Override
    public String getLabel() {
        return "Select";
    }

    @Override
    public int getHotKeyCode() {
        return KeyEvent.VK_S;
    }

    @Override
    public Icon getIcon() {
        return GUI.createIconFromSVG("images/tool-selection.svg");
    }

    @Override
    public boolean requiresPropertyEditor() {
        return true;
    }

    @Override
    public void updateControlsToolbar(JToolBar toolbar, final GraphEditor editor) {
        super.updateControlsToolbar(toolbar, editor);

        if (enableGroupping) {
            JButton groupButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-group.svg"),
                    "Group selection (" + DesktopApi.getMenuKeyMaskName() + "-G)");
            groupButton.addActionListener(event -> {
                groupSelection(editor);
                editor.requestFocus();
            });
            toolbar.add(groupButton);
        }

        if (enablePaging) {
            JButton groupPageButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-page.svg"),
                    "Combine selection into a page (Alt-G)");
            groupPageButton.addActionListener(event -> {
                pageSelection(editor);
                editor.requestFocus();
            });
            toolbar.add(groupPageButton);
        }

        if (enableGroupping || enablePaging) {
            JButton ungroupButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-ungroup.svg"),
                    "Ungroup selection (" + DesktopApi.getMenuKeyMaskName() + "+Shift-G)");
            ungroupButton.addActionListener(event -> {
                ungroupSelection(editor);
                editor.requestFocus();
            });
            toolbar.add(ungroupButton);

            JButton levelUpButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-level_up.svg"),
                    "Level up (PageUp)");
            levelUpButton.addActionListener(event -> {
                changeLevelUp(editor);
                editor.requestFocus();
            });
            toolbar.add(levelUpButton);

            JButton levelDownButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-level_down.svg"),
                    "Level down (PageDown)");
            levelDownButton.addActionListener(event -> {
                changeLevelDown(editor);
                editor.requestFocus();
            });
            toolbar.add(levelDownButton);
        }
        if (toolbar.getComponentCount() > 0) {
            toolbar.addSeparator();
        }
        if (enableFlipping) {
            JButton flipHorizontalButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-flip_horizontal.svg"),
                    "Flip horizontal");
            flipHorizontalButton.addActionListener(event -> {
                flipSelectionHorizontal(editor);
                editor.requestFocus();
            });
            toolbar.add(flipHorizontalButton);

            JButton flipVerticalButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-flip_vertical.svg"),
                    "Flip vertical");
            flipVerticalButton.addActionListener(event -> {
                flipSelectionVertical(editor);
                editor.requestFocus();
            });
            toolbar.add(flipVerticalButton);
        }

        if (enableRotating) {
            JButton rotateClockwiseButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-rotate_clockwise.svg"),
                    "Rotate clockwise");
            rotateClockwiseButton.addActionListener(event -> {
                rotateSelectionClockwise(editor);
                editor.requestFocus();
            });
            toolbar.add(rotateClockwiseButton);

            JButton rotateCounterclockwiseButton = GUI.createIconButton(
                    GUI.createIconFromSVG("images/selection-rotate_counterclockwise.svg"),
                    "Rotate counterclockwise");
            rotateCounterclockwiseButton.addActionListener(event -> {
                rotateSelectionCounterclockwise(editor);
                editor.requestFocus();
            });
            toolbar.add(rotateCounterclockwiseButton);
        }
        if (toolbar.getComponentCount() > 0) {
            toolbar.addSeparator();
        }
    }

    @Override
    public void activated(final GraphEditor editor) {
        super.activated(editor);
        currentMousePosition = null;
        currentNode = null;
        currentNodes = null;
    }

    @Override
    public void deactivated(GraphEditor editor) {
        super.deactivated(editor);
        editor.getModel().selectNone();
        currentMousePosition = null;
        currentNode = null;
        currentNodes = null;
    }

    @Override
    public void setPermissions(final GraphEditor editor) {
        WorkspaceEntry we = editor.getWorkspaceEntry();
        we.setCanModify(true);
        we.setCanSelect(true);
        we.setCanCopy(true);
    }

    @Override
    public boolean isDragging() {
        return dragState != DrugState.NONE;
    }

    @Override
    public void mouseClicked(GraphEditorMouseEvent e) {
        if (ignoreMouseButton1 && (e.getButton() == MouseEvent.BUTTON1)) {
            return;
        }
        if (ignoreMouseButton3 && (e.getButton() == MouseEvent.BUTTON3)) {
            return;
        }

        VisualModel model = e.getModel();
        GraphEditor editor = e.getEditor();
        Point2D position = e.getPosition();
        if (e.getButton() == MouseEvent.BUTTON1) {
            VisualNode node = HitMan.hitFirstInCurrentLevel(position, model);
            if (node == null) {
                if (e.getClickCount() > 1) {
                    if (model.getCurrentLevel() instanceof VisualGroup) {
                        VisualGroup currentGroup = (VisualGroup) model.getCurrentLevel();
                        Rectangle2D bbInLocalSpace = currentGroup.getBoundingBoxInLocalSpace();
                        Point2D posInRootSpace = currentGroup.getRootSpacePosition();
                        Rectangle2D bbInRootSpace = BoundingBoxHelper.move(bbInLocalSpace, posInRootSpace);
                        if (!bbInRootSpace.contains(position)) {
                            changeLevelUp(editor);
                            return;
                        }
                    }
                    if (model.getCurrentLevel() instanceof VisualPage) {
                        VisualPage currentPage = (VisualPage) model.getCurrentLevel();
                        Rectangle2D bbInLocalSpace = currentPage.getBoundingBoxInLocalSpace();
                        Point2D posInRootSpace = currentPage.getRootSpacePosition();
                        Rectangle2D bbInRootSpace = BoundingBoxHelper.move(bbInLocalSpace, posInRootSpace);
                        if (!bbInRootSpace.contains(position)) {
                            changeLevelUp(editor);
                            return;
                        }
                    }

                } else {
                    if (e.getKeyModifiers() == 0) {
                        model.selectNone();
                    }
                }
            } else {
                if (e.getClickCount() > 1) {
                    if (node instanceof VisualGroup || node instanceof VisualPage) {
                        changeLevelDown(editor);
                        return;

                    } else if (node instanceof VisualComment) {
                        final VisualComment comment = (VisualComment) node;
                        AbstractInplaceEditor textEditor = new LabelInplaceEditor(editor, comment);
                        textEditor.edit(comment.getLabel(), comment.getLabelFont(),
                                comment.getLabelOffset(), comment.getLabelAlignment(), true);
                        editor.forceRedraw();
                        return;
                    }
                } else {
                    Collection<VisualNode> nodes = e.isExtendKeyDown()
                            ? getNodeWithAdjacentConnections(model, node)
                            : Arrays.asList(new VisualNode[] {node});
                    if (e.isShiftKeyDown()) {
                        model.addToSelection(nodes);
                    } else if (e.isMenuKeyDown()) {
                        model.removeFromSelection(nodes);
                    } else {
                        model.select(nodes);
                    }
                }
            }
            anchorGenerator.mouseClicked(e);
        }
    }

    public Collection<VisualNode> getNodeWithAdjacentConnections(VisualModel model, VisualNode node) {
        ArrayList<VisualNode> result = new ArrayList<>();
        result.add(node);
        result.addAll(model.getConnections(node));
        return result;
    }

    public VisualNode hitTestPopup(VisualModel model, Point2D position) {
        return HitMan.hitFirstInCurrentLevel(position, model);
    }

    public JPopupMenu createPopupMenu(VisualNode node, final GraphEditor editor) {
        JPopupMenu popup = null;
        WorkspaceEntry we = editor.getWorkspaceEntry();
        List<Command> applicableTools = new ArrayList<>();
        HashSet<Command> enabledTools = new HashSet<>();
        for (Command command: Commands.getApplicableVisibleCommands(we)) {
            if (command instanceof NodeTransformer) {
                NodeTransformer nodeTransformer = (NodeTransformer) command;
                if (nodeTransformer.isApplicableTo(node)) {
                    applicableTools.add(command);
                    ModelEntry me = we.getModelEntry();
                    if (nodeTransformer.isEnabled(me, node)) {
                        enabledTools.add(command);
                    }
                }
            }
        }
        if (!applicableTools.isEmpty()) {
            popup = new JPopupMenu();
            popup.setFocusable(false);
            final Framework framework = Framework.getInstance();
            final MainWindow mainWindow = framework.getMainWindow();
            for (Command tool: applicableTools) {
                PopupToolAction toolAction = new PopupToolAction(tool);
                ActionMenuItem miTool = new ActionMenuItem(toolAction);
                miTool.addScriptedActionListener(mainWindow.getDefaultActionListener());
                miTool.setEnabled(enabledTools.contains(tool));
                popup.add(miTool);
            }
        }
        return popup;
    }

    @Override
    public void mouseMoved(GraphEditorMouseEvent e) {
        GraphEditor editor = e.getEditor();
        VisualModel model = editor.getModel();
        if (dragState == DrugState.MOVE) {
            Point2D prevPos = e.getPrevPosition();
            Point2D.Double pos1 = new Point2D.Double(prevPos.getX() + offset.getX(), prevPos.getY() + offset.getY());
            Point2D snapPos1 = editor.snap(pos1, snaps);
            Point2D.Double pos2 = new Point2D.Double(e.getX() + offset.getX(), e.getY() + offset.getY());
            Point2D snapPos2 = editor.snap(pos2, snaps);
            // Intermediate move of the selection - no need for beforeSelectionModification or afterSelectionModification
            VisualModelTransformer.translateSelection(model, snapPos2.getX() - snapPos1.getX(), snapPos2.getY() - snapPos1.getY());
        } else if (dragState == DrugState.SELECT) {
            selected.clear();
            selected.addAll(model.hitBox(e.getStartPosition(), e.getPosition()));
            selectionBox = getSelectionRect(e.getStartPosition(), e.getPosition());
            editor.repaint();
        } else {
            VisualNode node = HitMan.hitFirstInCurrentLevel(e.getPosition(), model);
            if (currentNode != node) {
                currentNode = node;
                currentNodes = e.isExtendKeyDown()
                        ? getNodeWithAdjacentConnections(model, node)
                        : Arrays.asList(new VisualNode[] {node});
                editor.repaint();
            }
        }
        currentMousePosition = e.getPosition();
    }

    @Override
    public void mousePressed(GraphEditorMouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            ignoreMouseButton1 = false;
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            ignoreMouseButton3 = false;
            if (isDragging()) {
                cancelDrag(e.getEditor());
            } else {
                VisualModel model = e.getModel();
                GraphEditor editor = e.getEditor();
                Point2D position = e.getPosition();
                VisualNode node = hitTestPopup(model, position);
                JPopupMenu popup = createPopupMenu(node, editor);
                if (popup != null) {
                    if (node == null) {
                        model.selectNone();
                    } else {
                        model.select(node);
                    }
                    MouseEvent systemEvent = e.getSystemEvent();
                    popup.show(systemEvent.getComponent(), systemEvent.getX(), systemEvent.getY());
                }
            }
        }
    }

    @Override
    public void startDrag(GraphEditorMouseEvent e) {
        GraphEditor editor = e.getEditor();
        VisualModel model = editor.getModel();
        if (e.getButtonModifiers() == MouseEvent.BUTTON1_DOWN_MASK) {
            Point2D startPos = e.getStartPosition();
            Node hitNode = HitMan.hitFirstInCurrentLevel(startPos, model);

            if (hitNode == null) {
                // If hit nothing then start select-drag
                if (e.isShiftKeyDown()) {
                    selectionMode = SelectionMode.ADD;
                } else if (e.isMenuKeyDown()) {
                    selectionMode = SelectionMode.REMOVE;
                } else {
                    selectionMode = SelectionMode.REPLACE;
                }
                // Selection will not actually be changed until drag completes
                dragState = DrugState.SELECT;
                selected.clear();
                if (selectionMode == SelectionMode.REPLACE) {
                    model.selectNone();
                } else {
                    selected.addAll(model.getSelection());
                }
            } else if ((e.getKeyModifiers() == 0) && (hitNode instanceof VisualNode)) {
                // If mouse down without modifiers and hit something then begin move-drag
                dragState = DrugState.MOVE;
                VisualNode node = (VisualNode) hitNode;
                if ((node != null) && !model.getSelection().contains(node)) {
                    model.select(node);
                }
                AffineTransform localToRootTransform = TransformHelper.getTransformToRoot(node);
                Point2D pos = TransformHelper.transform(node, localToRootTransform).getCenter();
                snaps = editor.getSnaps(node);
                Point2D snapPos = editor.snap(pos, snaps);
                offset = new Point2D.Double(snapPos.getX() - pos.getX(), snapPos.getY() - pos.getY());
                // Initial move of the selection - beforeSelectionModification is needed
                beforeSelectionModification(editor);
                VisualModelTransformer.translateSelection(model, offset.getX(), offset.getY());
            } else {
                // Do nothing if pressed on a node with modifiers
            }
        }
    }

    @Override
    public void finishDrag(GraphEditorMouseEvent e) {
        GraphEditor editor = e.getEditor();
        if (dragState == DrugState.MOVE) {
            // Final move of the selection - afterSelectionModification is needed
            afterSelectionModification(editor);
        } else if (dragState == DrugState.SELECT) {
            VisualModel model = e.getModel();
            if (selectionMode == SelectionMode.REPLACE) {
                model.select(selected);
            } else if (selectionMode == SelectionMode.ADD) {
                model.addToSelection(selected);
            } else if (selectionMode == SelectionMode.REMOVE) {
                model.removeFromSelection(selected);
            }
            selectionBox = null;
        }
        dragState = DrugState.NONE;
        selected.clear();
        editor.repaint();
    }

    private void cancelDrag(GraphEditor editor) {
        if (dragState == DrugState.MOVE) {
            editor.getWorkspaceEntry().cancelMemento();
        } else if (dragState == DrugState.SELECT) {
            selected.clear();
            selectionBox = null;
        }
        dragState = DrugState.NONE;
        ignoreMouseButton1 = true;
        ignoreMouseButton3 = true;
        editor.repaint();
    }

    @Override
    public boolean keyPressed(GraphEditorKeyEvent e) {
        GraphEditor editor = e.getEditor();
        switch (e.getKeyCode()) {
        case KeyEvent.VK_ESCAPE:
            if (isDragging()) {
                cancelDrag(editor);
            } else {
                cancelSelection(editor);
            }
            return true;
        case KeyEvent.VK_PAGE_UP:
            changeLevelUp(editor);
            return true;
        case KeyEvent.VK_PAGE_DOWN:
            changeLevelDown(editor);
            return true;
        }

        if (!e.isMenuKeyDown() && !e.isShiftKeyDown()) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                offsetSelection(editor, -1, 0);
                return true;
            case KeyEvent.VK_RIGHT:
                offsetSelection(editor, 1, 0);
                return true;
            case KeyEvent.VK_UP:
                offsetSelection(editor, 0, -1);
                return true;
            case KeyEvent.VK_DOWN:
                offsetSelection(editor, 0, 1);
                return true;
            }
        }

        if (enablePaging && e.isAltKeyDown() && !e.isMenuKeyDown()) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_G:
                if (e.isShiftKeyDown()) {
                    ungroupSelection(editor);
                } else {
                    pageSelection(editor);
                }
                return true;
            }
        }

        if (enableGroupping && e.isMenuKeyDown() && !e.isAltKeyDown()) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_G:
                if (e.isShiftKeyDown()) {
                    ungroupSelection(editor);
                } else {
                    groupSelection(editor);
                }
                return true;
            }
        }

        if (e.isMenuKeyDown() && !e.isAltKeyDown()) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_V:
                Point2D pastePosition = TransformHelper.snapP5(currentMousePosition);
                editor.getWorkspaceEntry().setPastePosition(pastePosition);
                return true;
            }
        }

        if (e.isExtendKeyDown()) {
            currentNodes = getNodeWithAdjacentConnections(e.getModel(), currentNode);
            editor.repaint();
            return true;
        }

        return super.keyPressed(e);
    }

    @Override
    public boolean keyReleased(GraphEditorKeyEvent e) {
        if (!e.isExtendKeyDown()) {
            currentNodes = Arrays.asList(new VisualNode[] {currentNode});
            e.getEditor().repaint();
        }
        return super.keyReleased(e);
    }

    @Override
    public String getHintText(final GraphEditor editor) {
        return "Modifiers: Shift - add to selection; " + DesktopApi.getMenuKeyMaskName()
            + " - remove from selection; Alt/AltGr - extend selection to adjacent nodes.";
    }

    @Override
    public void drawInUserSpace(GraphEditor editor, Graphics2D g) {
        if ((dragState == DrugState.SELECT) && (selectionBox != null)) {
            Viewport viewport = editor.getViewport();
            g.setStroke(new BasicStroke((float) viewport.pixelSizeInUserSpace().getX()));
            Color borderColor = CommonDecorationSettings.getSelectedComponentColor();
            Color fillColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 35);
            g.setColor(fillColor);
            g.fill(selectionBox);
            g.setColor(borderColor);
            g.draw(selectionBox);
        }
    }

    @Override
    public Decorator getDecorator(final GraphEditor editor) {
        return new Decorator() {

            @Override
            public Decoration getDecoration(Node node) {
                VisualModel model = editor.getModel();
                if ((currentNodes != null) && currentNodes.contains(node)) {
                    return Decoration.Highlighted.INSTANCE;
                }

                if (node == model.getCurrentLevel()) {
                    return Decoration.Empty.INSTANCE;
                }

                if (node == model.getRoot()) {
                    return Decoration.Shaded.INSTANCE;
                }
                /*
                 * r & !c & s | !r & (c | s) <=> (!r & c) | (!c & s)
                 * where
                 *   r = (selectionMode == SelectionState.REMOVE)
                 *   c = selected.contains(node)
                 *   s = model.getSelection().contains(node)
                 */
                if (((selectionMode != SelectionMode.REMOVE) && selected.contains(node))
                        || (!selected.contains(node) && model.getSelection().contains(node))) {
                    return Decoration.Selected.INSTANCE;
                }

                //if (node == mouseOverNode) return selectedDecoration;

                return null;
            }
        };
    }

    protected void changeLevelDown(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        Collection<VisualNode> selection = model.getSelection();
        if (selection.size() == 1) {
            Node node = selection.iterator().next();
            if (node instanceof Container) {
                editor.getWorkspaceEntry().saveMemento();
                model.setCurrentLevel((Container) node);
                currentNode = null;
                currentNodes = null;
                editor.repaint();
            }
        }
    }

    protected void changeLevelUp(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        Container level = model.getCurrentLevel();
        Container parent = Hierarchy.getNearestAncestor(level.getParent(), Container.class);
        if ((parent != null) && (level instanceof VisualNode)) {
            editor.getWorkspaceEntry().saveMemento();
            model.setCurrentLevel(parent);
            model.addToSelection((VisualNode) level);
            currentNode = null;
            currentNodes = null;
            editor.repaint();
        }
    }

    private Rectangle2D getSelectionRect(Point2D startPosition, Point2D currentPosition) {
        return new Rectangle2D.Double(
                Math.min(startPosition.getX(), currentPosition.getX()),
                Math.min(startPosition.getY(), currentPosition.getY()),
                Math.abs(startPosition.getX() - currentPosition.getX()),
                Math.abs(startPosition.getY() - currentPosition.getY())
        );
    }

    protected void cancelSelection(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            final Framework framework = Framework.getInstance();
            final MainWindow mainWindow = framework.getMainWindow();
            mainWindow.selectNone();
            editor.forceRedraw();
        }
        editor.requestFocus();
    }

    private void offsetSelection(final GraphEditor editor, double dx, double dy) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            beforeSelectionModification(editor);
            VisualModelTransformer.translateSelection(model, dx, dy);
            afterSelectionModification(editor);
        }
    }

    protected void groupSelection(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            beforeSelectionModification(editor);
            model.groupSelection();
            afterSelectionModification(editor);
        }
    }

    protected void ungroupSelection(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            beforeSelectionModification(editor);
            model.ungroupSelection();
            afterSelectionModification(editor);
        }
    }

    protected void pageSelection(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            beforeSelectionModification(editor);
            model.groupPageSelection();
            afterSelectionModification(editor);
        }
    }

    protected void rotateSelectionClockwise(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            beforeSelectionModification(editor);
            VisualModelTransformer.rotateSelection(model, Math.PI / 2);
            for (Node node : model.getSelection()) {
                if (node instanceof Rotatable) {
                    ((Rotatable) node).rotateClockwise();
                }
            }
            afterSelectionModification(editor);
        }
    }

    protected void rotateSelectionCounterclockwise(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            beforeSelectionModification(editor);
            VisualModelTransformer.rotateSelection(model, -Math.PI / 2);
            for (Node node : model.getSelection()) {
                if (node instanceof Rotatable) {
                    ((Rotatable) node).rotateCounterclockwise();
                }
            }
            afterSelectionModification(editor);
        }
    }

    protected void flipSelectionHorizontal(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            beforeSelectionModification(editor);
            VisualModelTransformer.scaleSelection(model, -1.0, 1.0);
            for (Node node : model.getSelection()) {
                if (node instanceof Flippable) {
                    ((Flippable) node).flipHorizontal();
                }
            }
            afterSelectionModification(editor);
        }
    }

    protected void flipSelectionVertical(final GraphEditor editor) {
        VisualModel model = editor.getModel();
        if (!model.getSelection().isEmpty()) {
            beforeSelectionModification(editor);
            VisualModelTransformer.scaleSelection(model, 1.0, -1.0);
            for (Node node : model.getSelection()) {
                if (node instanceof Flippable) {
                    ((Flippable) node).flipVertical();
                }
            }
            afterSelectionModification(editor);
        }
    }

    public void beforeSelectionModification(final GraphEditor editor) {
        // Capture model memento for use in afterSelectionModification
        editor.getWorkspaceEntry().captureMemento();
    }

    public void afterSelectionModification(final GraphEditor editor) {
        // Save memento that was captured in beforeSelectionModification
        editor.getWorkspaceEntry().saveMemento();
        // Redraw the editor window to recalculate all the bounding boxes
        editor.forceRedraw();
    }

}
