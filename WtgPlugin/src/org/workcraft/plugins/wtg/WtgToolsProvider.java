package org.workcraft.plugins.wtg;

import org.workcraft.gui.graph.generators.DefaultNodeGenerator;
import org.workcraft.gui.graph.tools.CommentGeneratorTool;
import org.workcraft.gui.graph.tools.CustomToolsProvider;
import org.workcraft.gui.graph.tools.GraphEditorTool;
import org.workcraft.gui.graph.tools.NodeGeneratorTool;
import org.workcraft.plugins.wtg.tools.WtgConnectionTool;
import org.workcraft.plugins.wtg.tools.WtgSelectionTool;
import org.workcraft.plugins.wtg.tools.WtgSignalGeneratorTool;
import org.workcraft.plugins.wtg.tools.WtgSimulationTool;

import java.util.ArrayList;

public class WtgToolsProvider implements CustomToolsProvider {

    @Override
    public Iterable<GraphEditorTool> getTools() {
        ArrayList<GraphEditorTool> result = new ArrayList<>();

        result.add(new WtgSelectionTool());
        result.add(new CommentGeneratorTool());
        result.add(new WtgConnectionTool());
        result.add(new NodeGeneratorTool(new DefaultNodeGenerator(State.class), true));
        result.add(new NodeGeneratorTool(new DefaultNodeGenerator(Waveform.class), true));
        result.add(new WtgSignalGeneratorTool());
        result.add(new WtgSimulationTool());
        return result;
    }

}
