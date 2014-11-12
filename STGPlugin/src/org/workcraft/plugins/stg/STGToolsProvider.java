package org.workcraft.plugins.stg;

import java.util.ArrayList;

import org.workcraft.dom.Node;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.graph.tools.ConnectionTool;
import org.workcraft.gui.graph.tools.CustomToolsProvider;
import org.workcraft.gui.graph.tools.DefaultNodeGenerator;
import org.workcraft.gui.graph.tools.GraphEditorTool;
import org.workcraft.gui.graph.tools.NodeGeneratorTool;
import org.workcraft.gui.graph.tools.CommentGeneratorTool;
import org.workcraft.plugins.stg.tools.STGSelectionTool;
import org.workcraft.plugins.stg.tools.STGSignalTransitionGeneratorTool;
import org.workcraft.plugins.stg.tools.STGSimulationTool;

public class STGToolsProvider implements CustomToolsProvider {

	@Override
	public Iterable<GraphEditorTool> getTools() {
		ArrayList<GraphEditorTool> result = new ArrayList<GraphEditorTool>();

		result.add(new STGSelectionTool());
		result.add(new CommentGeneratorTool());
		result.add(new ConnectionTool(true) {
			@Override
			protected boolean isConnectable(Node node) {
				return !(node instanceof VisualConnection) || (node instanceof VisualImplicitPlaceArc);
			}
		});
		result.add(new NodeGeneratorTool(new DefaultNodeGenerator(STGPlace.class)));
		result.add(new STGSignalTransitionGeneratorTool());
		result.add(new NodeGeneratorTool(new DefaultNodeGenerator(DummyTransition.class)));
		result.add(new STGSimulationTool());
		return result;
	}

}
