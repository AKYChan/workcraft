package org.workcraft.plugins.son;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import org.workcraft.dom.visual.BoundingBoxHelper;
import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.VisualComment;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.gui.Coloriser;
import org.workcraft.plugins.son.connections.VisualSONConnection;
import org.workcraft.plugins.son.elements.VisualBlock;
import org.workcraft.plugins.son.elements.VisualCondition;
import org.workcraft.plugins.son.elements.VisualEvent;
import org.workcraft.util.Hierarchy;

public class VisualONGroup extends VisualPage {
    private static final float strokeWidth = 0.03f;
//    private Positioning labelPositioning = Positioning.TOP_RIGHT;
//    private RenderedText groupLabelRenderedText = new RenderedText("", labelFont, labelPositioning, getGroupLabelOffset());

    private ONGroup mathGroup = null;

    public VisualONGroup(ONGroup mathGroup)    {
        super(mathGroup);
        this.mathGroup = mathGroup;
        removePropertyDeclarationByName("Fill color");
        removePropertyDeclarationByName("Label positioning");
        removePropertyDeclarationByName("Is collapsed");
    }

    @Override
    public Point2D getLabelOffset() {
        Rectangle2D bb = getInternalBoundingBoxInLocalSpace();
        double xOffset = bb.getMaxX();
        double yOffset = bb.getMinY();
        Rectangle2D labelBB = getLabelBoundingBox();
        if (labelBB != null) {
            xOffset -= (labelBB.getWidth() - getExpansion().getX()) / 2;
        }
        return new Point2D.Double(xOffset, yOffset);
    }

    @Override
    public Rectangle2D getLabelBoundingBox() {
        return BoundingBoxHelper.expand(super.getLabelBoundingBox(), 0.4, 0.2);
    }

    @Override
    public void draw(DrawRequest r) {
        for (VisualComponent component: Hierarchy.getChildrenOfType(this, VisualComponent.class)) {
            component.cacheRenderedText(r);
        }
        cacheRenderedText(r);

        Graphics2D g = r.getGraphics();
        Color colorisation = r.getDecoration().getColorisation();
        Rectangle2D groupBB = getInternalBoundingBoxInLocalSpace();
        if ((groupBB != null) && (getParent() != null)) {
            //draw label
            Rectangle2D labelBB = getLabelBoundingBox();
            if (labelBB != null) {
                g.setColor(Coloriser.colorise(Color.WHITE, colorisation));
                g.fill(labelBB);
                g.setStroke(new BasicStroke(strokeWidth - 0.005f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND, 3.0f, new float[]{0.1f,  0.05f}, 0f));
                g.setColor(Coloriser.colorise(getLabelColor(), colorisation));
                g.draw(labelBB);
                drawLabelInLocalSpace(r);
            }
            //draw group
            g.setColor(Coloriser.colorise(this.getForegroundColor(), colorisation));
            g.setStroke(new BasicStroke(strokeWidth));
            groupBB = BoundingBoxHelper.expand(groupBB, getExpansion().getX(), getExpansion().getY());
            g.draw(groupBB);
            drawNameInLocalSpace(r);
        }
    }

    @Override
    public void setLabel(String label) {
        super.setLabel(label);
        this.getMathGroup().setLabel(label);
    }

    @Override
    public String getLabel() {
        super.getLabel();
        return this.getMathGroup().getLabel();
    }

    @Override
    public void setForegroundColor(Color color) {
        this.getMathGroup().setForegroundColor(color);
    }

    @Override
    public Color getForegroundColor() {
        return this.getMathGroup().getForegroundColor();
    }

    public ONGroup getMathGroup() {
        return mathGroup;
    }

    public void setMathGroup(ONGroup mathGroup) {
        this.mathGroup = mathGroup;
    }

    public Collection<VisualCondition> getVisualConditions() {
        return Hierarchy.getDescendantsOfType(this, VisualCondition.class);
    }

    public Collection<VisualEvent> getVisualEvents() {
        return Hierarchy.getDescendantsOfType(this, VisualEvent.class);
    }

    public Collection<VisualSONConnection> getVisualSONConnections() {
        return Hierarchy.getDescendantsOfType(this, VisualSONConnection.class);
    }

    public Collection<VisualPage> getVisualPages() {
        return Hierarchy.getDescendantsOfType(this, VisualPage.class);
    }

    public Collection<VisualBlock> getVisualBlocks() {
        return Hierarchy.getDescendantsOfType(this, VisualBlock.class);
    }

    public Collection<VisualComment> getVisualComment() {
        return Hierarchy.getDescendantsOfType(this, VisualComment.class);
    }

    public Collection<VisualComponent> getVisualComponents() {
        return Hierarchy.getDescendantsOfType(this, VisualComponent.class);
    }

}
