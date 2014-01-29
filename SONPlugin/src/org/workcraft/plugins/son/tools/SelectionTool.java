package org.workcraft.plugins.son.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.workcraft.dom.Connection;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.gui.events.GraphEditorKeyEvent;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.graph.tools.GraphEditorTool;
import org.workcraft.plugins.son.VisualONGroup;
import org.workcraft.plugins.son.VisualSON;
import org.workcraft.plugins.son.connections.VisualSONConnection;
import org.workcraft.plugins.son.elements.VisualChannelPlace;
import org.workcraft.plugins.son.elements.VisualCondition;
import org.workcraft.plugins.son.elements.VisualEvent;
import org.workcraft.util.GUI;

public class SelectionTool extends org.workcraft.gui.graph.tools.SelectionTool {

	private GraphEditorTool channelPlaceTool = null;
	private boolean asyn = true;
	private boolean sync = true;

	public SelectionTool(GraphEditorTool channelPlaceTool) {
		this.channelPlaceTool = channelPlaceTool;
	}

	@Override
	public void createInterfacePanel(final GraphEditor editor) {
		super.createInterfacePanel(editor);
		JPanel sonPanel = new JPanel();
		controlPanel.add(sonPanel);
		JButton supergroupButton = GUI.createIconButton(GUI.createIconFromSVG(
				"images/icons/svg/son-supergroup.svg"), "Merge selected nodes into supergroup (Ctrl+B)");
		supergroupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionSupergroup(editor);
			}
		});
		sonPanel.add(supergroupButton);
	}

	@Override
	public void mouseClicked(GraphEditorMouseEvent e)
	{
		VisualModel model = e.getEditor().getModel();

		if (e.getClickCount() > 1)
		{
			VisualNode node = (VisualNode) HitMan.hitTestForSelection(e.getPosition(), model);
			Collection<Node> selection = e.getModel().getSelection();

			if (model.getCurrentLevel() instanceof VisualGroup) {
				VisualGroup currentGroup = (VisualGroup)model.getCurrentLevel();
				if ( !currentGroup.getBoundingBoxInLocalSpace().contains(e.getPosition()) ) {
					setChannelPlaceToolState(e.getEditor(), true);
				}
			}

			if(selection.size() == 1)
			{
				Node selectedNode = selection.iterator().next();
				selectedNode = (VisualNode) HitMan.hitTestForSelection(e.getPosition(), model);

				if (selectedNode instanceof VisualONGroup) {
					setChannelPlaceToolState(e.getEditor(), false);
				}
				if (selectedNode instanceof VisualCondition) {
					VisualCondition vc = (VisualCondition) selectedNode;
					if (vc.hasToken() == false)
						vc.setToken(true);
					else if (vc.hasToken() == true)
						vc.setToken(false);
				}

				if (selectedNode instanceof VisualEvent) {
					VisualEvent ve = (VisualEvent) selectedNode;
					if (ve.isFaulty() == false)
						ve.setFaulty(true);
					else if (ve.isFaulty() == true)
						ve.setFaulty(false);
				}

				if(selectedNode instanceof VisualChannelPlace) {
					VisualChannelPlace cPlace = (VisualChannelPlace) node;
					for (Connection con : model.getConnections(cPlace)){
						if (((VisualSONConnection) con).getSONConnectionType() == VisualSONConnection.SONConnectionType.ASYNLINE)
							this.sync = false;
						if (((VisualSONConnection) con).getSONConnectionType() == VisualSONConnection.SONConnectionType.SYNCLINE)
							this.asyn = false;
					}
					if (sync && !asyn)
						for (Connection con : model.getConnections(cPlace)){
						((VisualSONConnection) con).setSONConnectionType(VisualSONConnection.SONConnectionType.ASYNLINE);
						((VisualSONConnection) con).setMathConnectionType("ASYNLINE");
						}

					if (!sync && asyn)
						for (Connection con : model.getConnections(cPlace)){
						((VisualSONConnection) con).setSONConnectionType(VisualSONConnection.SONConnectionType.SYNCLINE);
						((VisualSONConnection) con).setMathConnectionType("SYNCLINE");
						}
					if (!sync && !asyn)
						for (Connection con : model.getConnections(cPlace)){
						((VisualSONConnection) con).setSONConnectionType(VisualSONConnection.SONConnectionType.SYNCLINE);
						((VisualSONConnection) con).setMathConnectionType("SYNCLINE");
						}
					asyn = true;
					sync = true;
				}
			}
		}
		super.mouseClicked(e);
	}

	@Override
	public void keyPressed(GraphEditorKeyEvent e)
	{
		super.keyPressed(e);
		if (!e.isCtrlDown())
		{
			if (!e.isShiftDown()) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_PAGE_UP:
					setChannelPlaceToolState(e.getEditor(), true);
					// Note: level-up is handled in the parent
					// selectionLevelUp();
					break;
				case KeyEvent.VK_PAGE_DOWN:
					setChannelPlaceToolState(e.getEditor(), false);
					// Note: level-down is handled in the parent
					// selectionLevelDown();
					break;
				}
			}
		}

		if(e.isCtrlDown()){
			switch (e.getKeyCode()){
			case KeyEvent.VK_B:
				selectionSupergroup(e.getEditor());
				break;
			}
		}
	}

	private void selectionSupergroup(final GraphEditor editor) {
		((VisualSON)editor.getModel()).superGroupSelection();
		editor.repaint();
	}

	private void setChannelPlaceToolState(final GraphEditor editor, boolean state) {
		editor.getMainWindow().getCurrentEditor().getToolBox().setToolButtonState(channelPlaceTool, state);
	}

}
