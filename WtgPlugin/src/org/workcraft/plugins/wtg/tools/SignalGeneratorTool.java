package org.workcraft.plugins.wtg.tools;

import java.awt.Color;
import java.awt.Graphics2D;

import org.workcraft.dom.visual.VisualModel;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.generators.DefaultNodeGenerator;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.graph.tools.NodeGeneratorTool;
import org.workcraft.plugins.dtd.Signal;
import org.workcraft.plugins.wtg.VisualWaveform;
import org.workcraft.util.GUI;

public class SignalGeneratorTool extends NodeGeneratorTool {

    public SignalGeneratorTool() {
        super(new DefaultNodeGenerator(Signal.class));
    }

    @Override
    public void mousePressed(GraphEditorMouseEvent e) {
        VisualModel model = e.getModel();
        if (model.getCurrentLevel() instanceof VisualWaveform) {
            super.mousePressed(e);
        }
    }

    @Override
    public void drawInScreenSpace(GraphEditor editor, Graphics2D g) {
        VisualModel model = editor.getModel();
        if (model.getCurrentLevel() instanceof VisualWaveform) {
            super.drawInScreenSpace(editor, g);
        } else {
            GUI.drawEditorMessage(editor, g, Color.RED, "Signals can only be created inside waveforms.");
        }
    }

}
