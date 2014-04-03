/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.gui.graph.tools;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedHashSet;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.BoundingBoxHelper;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.Movable;
import org.workcraft.dom.visual.MovableHelper;
import org.workcraft.dom.visual.TransformHelper;
import org.workcraft.dom.visual.VisualComment;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualModelTransformer;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.dom.visual.connections.DefaultAnchorGenerator;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.gui.events.GraphEditorKeyEvent;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.layouts.WrapLayout;
import org.workcraft.util.GUI;
import org.workcraft.util.Hierarchy;

public class SelectionTool extends AbstractTool {
	private static final int DRAG_NONE = 0;
	private static final int DRAG_MOVE = 1;
	private static final int DRAG_SELECT = 2;

	private static final int SELECTION_NONE = 0;
	private static final int SELECTION_ADD = 1;
	private static final int SELECTION_REMOVE = 2;
	private static final int SELECTION_REPLACE = 3;
	protected static Color selectionColor = new Color(99, 130, 191).brighter();

	// node for the MouseOver events
	private VisualNode mouseOverNode = null;


	protected Color selectionBorderColor = new Color(200, 200, 200);
	protected Color selectionFillColor = new Color(99, 130, 191, 32);
	protected Color grayOutColor = Color.LIGHT_GRAY;

	protected JPanel interfacePanel;
	protected JPanel controlPanel;
	protected JPanel infoPanel;
	protected JPanel statusPanel;

	private int drag = DRAG_NONE;
	private boolean notClick1 = false;
	private boolean notClick3 = false;

	private Point2D snapOffset;
	private DefaultAnchorGenerator anchorGenerator = new DefaultAnchorGenerator();

	private LinkedHashSet<Node> selected = new LinkedHashSet<Node>();
	private int selectionMode;
	private Rectangle2D selectionBox = null;

	private boolean cancelInPlaceEdit = false;

	public SelectionTool() {
		super();
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
		return GUI.createIconFromSVG("images/icons/svg/select.svg");
	}

	@Override
	public JPanel getInterfacePanel() {
		return interfacePanel;
	}

	@Override
	public void createInterfacePanel(final GraphEditor editor) {
		super.createInterfacePanel(editor);

		interfacePanel = new JPanel(new BorderLayout());

		controlPanel = new JPanel(new WrapLayout(WrapLayout.CENTER, 0, 0));
		interfacePanel.add(controlPanel, BorderLayout.PAGE_START);

		JPanel groupPanel = new JPanel(new FlowLayout());
		controlPanel.add(groupPanel);


		JButton groupButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/selection-group.svg"), "Group selection (Ctrl+G)");
		groupButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionGroup(editor);
			}
		});
		groupPanel.add(groupButton);

		JButton groupPageButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/page.svg"), "Group selection into a page (Alt+G)");
		groupPageButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionPageGroup(editor);
			}
		});

		groupPanel.add(groupPageButton);

		JButton ungroupButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/selection-ungroup.svg"), "Ungroup selection (Ctrl+Shift+G)");
		ungroupButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionUngroup(editor);
			}
		});




		groupPanel.add(ungroupButton);

		JPanel levelPanel = new JPanel(new FlowLayout());
		controlPanel.add(levelPanel);
		JButton levelUpButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/selection-level_up.svg"), "Level up (PageUp)");
		levelUpButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				changeLevelUp(editor);
			}
		});
		levelPanel.add(levelUpButton);
		JButton levelDownButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/selection-level_down.svg"), "Level down (PageDown)");
		levelDownButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				changeLevelDown(editor);
			}
		});
		levelPanel.add(levelDownButton);

		JPanel flipPanel = new JPanel(new FlowLayout());
		controlPanel.add(flipPanel);
		JButton flipHorizontalButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/selection-flip_horizontal.svg"), "Flip horizontal (Ctrl+F)");
		flipHorizontalButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionFlipHorizontal(editor);
			}
		});
		flipPanel.add(flipHorizontalButton);
		JButton flipVerticalButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/selection-flip_vertical.svg"), "Flip vertical (Ctrl+Shift+F)");
		flipVerticalButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionFlipVertical(editor);
			}
		});
		flipPanel.add(flipVerticalButton);

		JPanel rotatePanel = new JPanel(new FlowLayout());
		controlPanel.add(rotatePanel);
		JButton rotateClockwiseButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/selection-rotate_clockwise.svg"), "Rotate clockwise (Ctrl+R)");
		rotateClockwiseButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionRotateClockwise(editor);
			}
		});
		rotatePanel.add(rotateClockwiseButton);
		JButton rotateCounterclockwiseButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/selection-rotate_counterclockwise.svg"), "Rotate counterclockwise (Ctrl+Shift+R)");
		rotateCounterclockwiseButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionRotateCounterclockwise(editor);
			}
		});
		rotatePanel.add(rotateCounterclockwiseButton);
	}


	private void resetState(GraphEditor editor) {
		mouseOverNode = null;
	}

	private void updateState(GraphEditorMouseEvent e) {
		Point2D mousePosition = e.getPosition();

		VisualNode node = (VisualNode) HitMan.hitTestForSelection(mousePosition, e.getModel());
		mouseOverNode = node;

	}

	@Override
	public void activated(final GraphEditor editor) {
		super.activated(editor);
		editor.getWorkspaceEntry().setCanModify(true);
		resetState(editor);
	}

	@Override
	public void deactivated(GraphEditor editor) {
		editor.getModel().selectNone();
		resetState(editor);
	}

	@Override
	public boolean isDragging() {
		return drag != DRAG_NONE;
	}

	@Override
	public void mouseClicked(GraphEditorMouseEvent e) {
		if(notClick1 && e.getButton() == MouseEvent.BUTTON1)
			return;
		if(notClick3 && e.getButton() == MouseEvent.BUTTON3)
			return;

		if (e.getButton() == MouseEvent.BUTTON1) {
			VisualModel model = e.getEditor().getModel();
			Node node = HitMan.hitTestForSelection(e.getPosition(), model);
			if (node == null) {
				if (e.getClickCount() > 1) {
					if (model.getCurrentLevel() instanceof VisualGroup) {
						VisualGroup currentGroup = (VisualGroup)model.getCurrentLevel();
						if ( !currentGroup.getBoundingBoxInLocalSpace().contains(e.getPosition()) ) {
							changeLevelUp(e.getEditor());
						}
					}

					if ( model.getCurrentLevel() instanceof VisualPage) {
						VisualPage currentPage = (VisualPage)model.getCurrentLevel();
						if ( !currentPage.getBoundingBoxInLocalSpace().contains(e.getPosition()) ) {
							changeLevelUp(e.getEditor());
						}
					}

				} else {
					if (e.getKeyModifiers() == 0) {
						e.getModel().selectNone();
					}
				}
			} else {
				if (e.getClickCount() > 1) {
					if (node instanceof VisualGroup || node instanceof VisualPage) {
						changeLevelDown(e.getEditor());
					} else if (node instanceof VisualComment) {
						VisualComment comment = (VisualComment) node;
						editLabelInPlace(e.getEditor(), comment, comment.getLabel());
					}
				} else {
					switch (e.getKeyModifiers()) {
					case 0:
						e.getModel().select(node);
						break;
					case MouseEvent.SHIFT_DOWN_MASK:
						e.getModel().addToSelection(node);
						break;
					case MouseEvent.CTRL_DOWN_MASK:
						e.getModel().removeFromSelection(node);
						break;
					}
				}
			}
		}
		anchorGenerator.mouseClicked(e);
	}

	@Override
	public void mouseMoved(GraphEditorMouseEvent e) {
		VisualModel model = e.getEditor().getModel();
		if(drag==DRAG_MOVE) {
			Point2D p1 = e.getEditor().snap(new Point2D.Double(e.getPrevPosition().getX()+snapOffset.getX(), e.getPrevPosition().getY()+snapOffset.getY()));
			Point2D p2 = e.getEditor().snap(new Point2D.Double(e.getX()+snapOffset.getX(), e.getY()+snapOffset.getY()));
			selectionOffset(e.getEditor(), p2.getX()-p1.getX(), p2.getY()-p1.getY());
		} else if(drag==DRAG_SELECT) {
			selected.clear();
			selected.addAll(model.boxHitTest(e.getStartPosition(), e.getPosition()));
			selectionBox = selectionRect(e.getStartPosition(), e.getPosition());
			e.getEditor().repaint();
		} else {
			// "mouse over" events

		}

		updateState(e);

	}

	@Override
	public void startDrag(GraphEditorMouseEvent e) {
		VisualModel model = e.getEditor().getModel();
		if (e.getButtonModifiers() == MouseEvent.BUTTON1_DOWN_MASK) {
			Node hitNode = HitMan.hitTestForSelection(e.getStartPosition(), model);

			if (hitNode == null) {
				// hit nothing, so start select-drag
				switch (e.getKeyModifiers()) {
				case 0:
					selectionMode = SELECTION_REPLACE;
					break;
				case MouseEvent.CTRL_DOWN_MASK:
					selectionMode = SELECTION_REMOVE;
					break;
				case MouseEvent.SHIFT_DOWN_MASK:
					selectionMode = SELECTION_ADD;
					break;
				default:
					selectionMode = SELECTION_NONE;
				}

				if (selectionMode != SELECTION_NONE) {
					// selection will not actually be changed until drag completes
					drag = DRAG_SELECT;
					selected.clear();
					if (selectionMode == SELECTION_REPLACE) {
						model.selectNone();
					} else {
						selected.addAll(model.getSelection());
					}
				}
			} else {
				// hit something
				if (e.getKeyModifiers() == 0 && hitNode instanceof Movable) {
					// mouse down without modifiers, begin move-drag
					drag = DRAG_MOVE;
					e.getEditor().getWorkspaceEntry().captureMemento();
					if (hitNode != null && !model.getSelection().contains(hitNode)) {
						e.getModel().select(hitNode);
					}
					Movable node = (Movable) hitNode;
					Point2D pos = new Point2D.Double(node.getTransform().getTranslateX(), node.getTransform().getTranslateY());
					Point2D pSnap = e.getEditor().snap(pos);
					selectionOffset(e.getEditor(), pSnap.getX()-pos.getX(), pSnap.getY()-pos.getY());
					snapOffset = new Point2D.Double(pSnap.getX()-e.getStartPosition().getX(), pSnap.getY()-e.getStartPosition().getY());
				} else {
					// do nothing if pressed on a node with modifiers
				}
			}
		}
	}

	@Override
	public void mousePressed(GraphEditorMouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1)
			notClick1 = false;

		if (e.getButton() == MouseEvent.BUTTON3) {

			if (isDragging()) {
				cancelDrag(e.getEditor());
				e.getEditor().repaint();
				notClick1 = true;
				notClick3 = true;
			} else {
				notClick3 = false;
			}
		}
	}

	@Override
	public void finishDrag(GraphEditorMouseEvent e) {
		if (drag == DRAG_MOVE) {
			e.getEditor().getWorkspaceEntry().saveMemento();
		} else if (drag == DRAG_SELECT) {
			if (selectionMode == SELECTION_REPLACE)
				e.getModel().select(selected);
			else if (selectionMode == SELECTION_ADD)
				e.getModel().addToSelection(selected);
			else if (selectionMode == SELECTION_REMOVE)
				e.getModel().removeFromSelection(selected);
			selectionBox = null;
		}
		drag = DRAG_NONE;
		selected.clear();
		e.getEditor().repaint();
	}


	private void cancelDrag(GraphEditor editor) {
		if(drag==DRAG_MOVE) {
			editor.getWorkspaceEntry().cancelMemento();
		} else if(drag == DRAG_SELECT) {
			selected.clear();
			selectionBox = null;
		}
		drag = DRAG_NONE;
	}

	@Override
	public void mouseReleased(GraphEditorMouseEvent e) {
		// do nothing
	}

	@Override
	public void keyPressed(GraphEditorKeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
			if (isDragging()) {
				cancelDrag(e.getEditor());
				notClick1 = true;
			} else {
				selectionCancel(e.getEditor());
			}
			break;
		case KeyEvent.VK_PAGE_UP:
			changeLevelUp(e.getEditor());
			break;
		case KeyEvent.VK_PAGE_DOWN:
			changeLevelDown(e.getEditor());
			break;
		}

		if (!e.isCtrlDown() && !e.isShiftDown()) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				selectionOffset(e.getEditor(), -1, 0);
				break;
			case KeyEvent.VK_RIGHT:
				selectionOffset(e.getEditor(), 1, 0);
				break;
			case KeyEvent.VK_UP:
				selectionOffset(e.getEditor(), 0, -1);
				break;
			case KeyEvent.VK_DOWN:
				selectionOffset(e.getEditor(), 0, 1);
				break;
			}
		}

		if (e.isCtrlDown()) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_G:
				if (e.isShiftDown()) {
					selectionUngroup(e.getEditor());
				} else {
					if (e.isAltDown())
						selectionPageGroup(e.getEditor());
					else
						selectionGroup(e.getEditor());
				}
				break;
			case KeyEvent.VK_F:
				if (e.isShiftDown()) {
					selectionFlipVertical(e.getEditor());
				} else {
					selectionFlipHorizontal(e.getEditor());
				}
				break;
			case KeyEvent.VK_R:
				if (e.isShiftDown()) {
					selectionRotateCounterclockwise(e.getEditor());
				} else {
					selectionRotateClockwise(e.getEditor());
				}
				break;
			}
		}
	}

	@Override
	public void drawInUserSpace(GraphEditor editor, Graphics2D g) {
		if(drag==DRAG_SELECT && selectionBox!=null) {
			g.setStroke(new BasicStroke((float) editor.getViewport().pixelSizeInUserSpace().getX()));

			g.setColor(selectionFillColor);
			g.fill(selectionBox);
			g.setColor(selectionBorderColor);
			g.draw(selectionBox);
		}
	}

	@Override
	public Decorator getDecorator(final GraphEditor editor) {
		return new Decorator(){

			@Override
			public Decoration getDecoration(Node node) {
				if(node == editor.getModel().getCurrentLevel()) {
					return Decoration.Empty.INSTANCE;
				}
				if(node == editor.getModel().getRoot()) {
					return new Decoration(){
						@Override
						public Color getColorisation() {
							return grayOutColor;
						}
						@Override
						public Color getBackground() {
							return null;
						}
					};
				}

				Decoration selectedDecoration = new Decoration() {
					@Override
					public Color getColorisation() {
						return selectionColor;
					}
					@Override
					public Color getBackground() {
						return null;
					}
				};

				if(selected.contains(node)) {
					if (selectionMode == SELECTION_REMOVE) {
						return null;
					} else {
						return selectedDecoration;
					}
				}


				if (node==mouseOverNode) return selectedDecoration;

				if(editor.getModel().getSelection().contains(node)) {
					return selectedDecoration;
				} else {
					return null;
				}
			}
		};
	}

	private void editLabelInPlace (final GraphEditor editor, final VisualComponent component, String initialText) {
		final JTextField text = new JTextField(initialText);
		AffineTransform localToRootTransform = TransformHelper.getTransformToRoot(component);
		Rectangle2D bbRoot = TransformHelper.transform(component, localToRootTransform).getBoundingBox();
		Rectangle bbScreen = editor.getViewport().userToScreen(BoundingBoxHelper.expand(bbRoot, bbRoot.getWidth(), 0.3));
		float fontSize = VisualComponent.labelFont.getSize2D() * (float)editor.getViewport().getTransform().getScaleY();
		text.setFont(VisualComponent.labelFont.deriveFont(fontSize));
		text.setBounds(bbScreen.x, bbScreen.y, bbScreen.width, bbScreen.height);
		text.setHorizontalAlignment(JTextField.CENTER);
		text.selectAll();
		editor.getOverlay().add(text);
		text.requestFocusInWindow();

		text.addKeyListener( new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
					cancelInPlaceEdit = false;
					text.getParent().remove(text);
					editor.requestFocus();
				}
				else if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
					cancelInPlaceEdit = true;
					text.getParent().remove(text);
					editor.requestFocus();
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyTyped(KeyEvent arg0) {
			}
		});

		text.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				editor.getWorkspaceEntry().setCanModify(false);
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				if (text.getParent() != null)
					text.getParent().remove(text);
				final String newName = text.getText();
				if (!cancelInPlaceEdit) {
					editor.getWorkspaceEntry().captureMemento();
					try {
						component.setLabel(newName);
						editor.getWorkspaceEntry().saveMemento();
					} catch (ArgumentException e) {
						JOptionPane.showMessageDialog(null, e.getMessage());
						editLabelInPlace(editor, component, newName);
						editor.getWorkspaceEntry().cancelMemento();
					}
				}
				editor.getWorkspaceEntry().setCanModify(true);
				editor.repaint();
			}
		});
	}

	protected void changeLevelDown(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		Collection<Node> selection = model.getSelection();
		if (selection.size() == 1) {
			Node node = selection.iterator().next();
			if(node instanceof Container) {
				model.setCurrentLevel((Container)node);
				editor.repaint();
			}
		}
	}

	protected void changeLevelUp(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		Container level = model.getCurrentLevel();
		Container parent = Hierarchy.getNearestAncestor(level.getParent(), Container.class);
		if (parent != null) {
			model.setCurrentLevel(parent);
			model.addToSelection(level);
			editor.repaint();
		}
	}

	private Rectangle2D selectionRect(Point2D startPosition, Point2D currentPosition) {
		return new Rectangle2D.Double(
				Math.min(startPosition.getX(), currentPosition.getX()),
				Math.min(startPosition.getY(), currentPosition.getY()),
				Math.abs(startPosition.getX()-currentPosition.getX()),
				Math.abs(startPosition.getY()-currentPosition.getY())
		);
	}

	private void selectionOffset(final GraphEditor editor, double dx, double dy) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			// Note that no memento should be saved until the drug action is complete
			for(Node node : model.getSelection()){
				if(node instanceof Movable) {
					Movable mv = (Movable) node;
					MovableHelper.translate(mv, dx, dy);
				}
			}
		}
	}

	protected void selectionCancel(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getMainWindow().selectNone();
			editor.repaint();
		}
	}

	protected void selectionGroup(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getWorkspaceEntry().saveMemento();
			model.groupSelection();
			editor.repaint();
		}
	}

	protected void selectionUngroup(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getWorkspaceEntry().saveMemento();
			model.ungroupSelection();
			editor.repaint();
		}
	}

	protected void selectionPageGroup(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getWorkspaceEntry().saveMemento();
			model.groupPageSelection();
			editor.repaint();
		}
	}

	protected void selectionPageUngroup(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getWorkspaceEntry().saveMemento();
			model.ungroupPageSelection();
			editor.repaint();
		}
	}

	protected void selectionRotateClockwise(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getWorkspaceEntry().saveMemento();
			VisualModelTransformer.rotateSelection(model, Math.PI/2);
			editor.repaint();
		}
	}

	protected void selectionRotateCounterclockwise(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getWorkspaceEntry().saveMemento();
			VisualModelTransformer.rotateSelection(model, -Math.PI/2);
			editor.repaint();
		}
	}

	protected void selectionFlipHorizontal(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getWorkspaceEntry().saveMemento();
			VisualModelTransformer.scaleSelection(model, -1, 1);
			editor.repaint();
		}
	}

	protected void selectionFlipVertical(final GraphEditor editor) {
		VisualModel model = editor.getModel();
		if (model.getSelection().size() > 0) {
			editor.getWorkspaceEntry().saveMemento();
			VisualModelTransformer.scaleSelection(model, 1, -1);
			editor.repaint();
		}
	}

}
