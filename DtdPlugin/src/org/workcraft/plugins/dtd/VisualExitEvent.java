package org.workcraft.plugins.dtd;

import org.workcraft.plugins.shared.CommonVisualSettings;

import java.awt.*;
import java.awt.geom.Path2D;

public class VisualExitEvent extends VisualEvent {

    public VisualExitEvent(ExitEvent exit) {
        super(exit);
    }

    @Override
    public Shape getShape() {
        double size = CommonVisualSettings.getNodeSize();
        double w2 = 0.04 * size;
        double s2 = 0.25 * size;
        Path2D shape = new Path2D.Double();
        shape.moveTo(0.0 + w2, +s2);
        shape.lineTo(0.0 - w2, +s2);
        shape.lineTo(0.0 - w2, -s2);
        shape.lineTo(0.0 + w2, -s2);
        return shape;
    }

    @Override
    public BasicStroke getStroke() {
        return new BasicStroke((float) CommonVisualSettings.getStrokeWidth() / 10.0f);
    }

}
