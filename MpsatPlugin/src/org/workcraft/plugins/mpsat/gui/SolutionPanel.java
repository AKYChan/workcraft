package org.workcraft.plugins.mpsat.gui;

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.workcraft.Framework;
import org.workcraft.Trace;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.ToolboxPanel;
import org.workcraft.gui.graph.GraphEditorPanel;
import org.workcraft.plugins.mpsat.tasks.MpsatChainTask;
import org.workcraft.plugins.petri.tools.PetriNetSimulationTool;
import org.workcraft.workspace.WorkspaceEntry;


@SuppressWarnings("serial")
public class SolutionPanel extends JPanel {
	private JPanel buttonsPanel;
	private JTextArea traceText;

	public SolutionPanel(final MpsatChainTask task, final Trace trace, final ActionListener closeAction) {
		super (new TableLayout(new double[][]
		        { { TableLayout.FILL, TableLayout.PREFERRED },
				{TableLayout.FILL} }
		));

		traceText = new JTextArea();
		traceText.setText(trace.toString());

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(traceText);

		buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

		JButton playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final Framework framework = Framework.getInstance();
				final WorkspaceEntry we = task.getWorkspaceEntry();
				final MainWindow mainWindow = framework.getMainWindow();
				GraphEditorPanel currentEditor = mainWindow.getCurrentEditor();
				if(currentEditor == null || currentEditor.getWorkspaceEntry() != we) {
					final List<GraphEditorPanel> editors = mainWindow.getEditors(we);
					if(editors.size()>0) {
						currentEditor = editors.get(0);
						mainWindow.requestFocus(currentEditor);
					} else {
						currentEditor = mainWindow.createEditorWindow(we);
					}
				}
				final ToolboxPanel toolbox = currentEditor.getToolBox();
				final PetriNetSimulationTool tool = toolbox.getToolInstance(PetriNetSimulationTool.class);
				toolbox.selectTool(tool);
				tool.setTrace(trace, currentEditor);
				closeAction.actionPerformed(null);
			}
		});

		buttonsPanel.add(playButton);

		add(scrollPane, "0 0");
		add(buttonsPanel, "1 0");
	}

}
