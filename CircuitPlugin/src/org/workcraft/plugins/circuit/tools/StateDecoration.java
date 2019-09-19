package org.workcraft.plugins.circuit.tools;

import java.awt.Color;

import org.workcraft.gui.tools.Decoration;

public interface StateDecoration extends Decoration {

    boolean showForcedInit();

    class Empty implements StateDecoration {

        public static final StateDecoration.Empty INSTANCE = new StateDecoration.Empty();

        @Override
        public Color getColorisation() {
            return Decoration.Empty.INSTANCE.getColorisation();
        }

        @Override
        public Color getBackground() {
            return Decoration.Empty.INSTANCE.getBackground();
        }

        @Override
        public boolean showForcedInit() {
            return true;
        }

    }

}
