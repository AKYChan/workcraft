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

package org.workcraft.dom.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.workcraft.dom.Container;
import org.workcraft.dom.DefaultGroupImpl;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.Coloriser;
import org.workcraft.gui.graph.tools.ContainerDecoration;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.observation.HierarchyObserver;
import org.workcraft.observation.ObservableHierarchy;
import org.workcraft.observation.TransformChangedEvent;
import org.workcraft.observation.TransformChangingEvent;
import org.workcraft.plugins.shared.CommonVisualSettings;
import org.workcraft.util.Hierarchy;

public class VisualGroup extends VisualTransformableNode implements Drawable, Collapsible, Container, ObservableHierarchy {
    public static final String PROPERTY_IS_COLLAPSED = "Is collapsed";

    protected double size = CommonVisualSettings.getNodeSize();
    protected static final double margin = 0.20;

    private boolean isCurrentLevelInside = false;
    private boolean isCollapsed = false;
    private boolean isExcited = false;
    DefaultGroupImpl groupImpl = new DefaultGroupImpl(this);

    public VisualGroup() {
        super();
        addPropertyDeclarations();
    }

    private void addPropertyDeclarations() {
        addPropertyDeclaration(new PropertyDeclaration<VisualGroup, Boolean>(
                this, PROPERTY_IS_COLLAPSED, Boolean.class, true, true, true) {

            @Override
            protected void setter(VisualGroup object, Boolean value) {
                object.setIsCollapsed(value);
            }
            @Override
            protected Boolean getter(VisualGroup object) {
                return object.getIsCollapsed();
            }
        });
    }

    @Override
    public void setIsCurrentLevelInside(boolean value) {
        if (isCurrentLevelInside != value) {
            sendNotification(new TransformChangingEvent(this));
            this.isCurrentLevelInside = value;
            sendNotification(new TransformChangedEvent(this));
        }
    }

    public boolean isCurrentLevelInside() {
        return isCurrentLevelInside;
    }

    @Override
    public void setIsCollapsed(boolean value) {
        if (isCollapsed != value) {
            sendNotification(new TransformChangingEvent(this));
            isCollapsed = value;
            sendNotification(new TransformChangedEvent(this));
        }
    }

    @Override
    public boolean getIsCollapsed() {
        return isCollapsed && !isExcited;
    }

    @Override
    public void setIsExcited(boolean value) {
        if (isExcited != value) {
            sendNotification(new TransformChangingEvent(this));
            isExcited = value;
            sendNotification(new TransformChangedEvent(this));
        }
    }

    @Override
    public void draw(DrawRequest r) {

        Decoration dec = r.getDecoration();
        if (dec instanceof ContainerDecoration) {
            setIsExcited(((ContainerDecoration) dec).isContainerExcited());
        }

        // This is to update the rendered text for names (and labels) of group children,
        // which is necessary to calculate the bounding box before children have been drawn
        for (VisualComponent component: Hierarchy.getChildrenOfType(this, VisualComponent.class)) {
            component.cacheRenderedText(r);
        }

        Rectangle2D bb = getBoundingBoxInLocalSpace();
        if ((bb != null) && (getParent() != null)) {
            Graphics2D g = r.getGraphics();
            Decoration d = r.getDecoration();
            g.setColor(Coloriser.colorise(Color.GRAY, d.getColorisation()));
            float[] pattern = {0.2f, 0.2f};
            g.setStroke(new BasicStroke(0.05f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1.0f, pattern, 0.0f));
            g.draw(bb);

            if (d.getColorisation() != null) {
                float s2 = (float) CommonVisualSettings.getPivotSize() / 2;
                Path2D p = new Path2D.Double();
                p.moveTo(-s2, 0);
                p.lineTo(s2, 0);
                p.moveTo(0, -s2);
                p.lineTo(0, s2);
                g.setStroke(new BasicStroke((float) CommonVisualSettings.getPivotWidth()));
                g.draw(p);
            }
        }
    }

    @Override
    public Rectangle2D getBoundingBoxInLocalSpace() {
        Rectangle2D bb = null;
        if (!getIsCollapsed() || isCurrentLevelInside()) {
            Collection<Touchable> children = Hierarchy.getChildrenOfType(this, Touchable.class);
            bb = BoundingBoxHelper.mergeBoundingBoxes(children);
        }
        if (bb == null) {
            bb = new Rectangle2D.Double(-size / 2, -size / 2, size, size);
        }
        return BoundingBoxHelper.expand(bb, margin, margin);
    }

    public final Collection<VisualComponent> getComponents() {
        return Hierarchy.getChildrenOfType(this, VisualComponent.class);
    }

    public final Collection<VisualConnection> getnections() {
        return Hierarchy.getChildrenOfType(this, VisualConnection.class);
    }

    public List<Node> unGroup() {
        ArrayList<Node> nodesToReparent = new ArrayList<>(groupImpl.getChildren());
        Container newParent = Hierarchy.getNearestAncestor(getParent(), Container.class);
        groupImpl.reparent(nodesToReparent, newParent);
        double tx = localToParentTransform.getTranslateX();
        double ty = localToParentTransform.getTranslateY();
        VisualModelTransformer.translateNodes(nodesToReparent, -tx, -ty);
        return nodesToReparent;
    }

    @Override
    public boolean hitTestInLocalSpace(Point2D pointInLocalSpace) {
        Rectangle2D bb = getBoundingBoxInLocalSpace();
        if ((bb != null) && (getParent() != null)) {
            return bb.contains(pointInLocalSpace);
        }
        return false;
    }

    @Override
    public void add(Node node) {
        groupImpl.add(node);
    }

    @Override
    public void addObserver(HierarchyObserver obs) {
        groupImpl.addObserver(obs);
    }

    @Override
    public Collection<Node> getChildren() {
        return groupImpl.getChildren();
    }

    @Override
    public Node getParent() {
        return groupImpl.getParent();
    }

    @Override
    public void remove(Node node) {
        groupImpl.remove(node);
    }

    public void removeWithoutNotify(Node node) {
        groupImpl.removeWithoutNotify(node);
    }

    @Override
    public void removeObserver(HierarchyObserver obs) {
        groupImpl.removeObserver(obs);
    }

    @Override
    public void removeAllObservers() {
        groupImpl.removeAllObservers();
    }

    @Override
    public void setParent(Node parent) {
        groupImpl.setParent(parent);
    }

    @Override
    public void add(Collection<Node> nodes) {
        groupImpl.add(nodes);
    }

    @Override
    public void remove(Collection<Node> nodes) {
        groupImpl.remove(nodes);
    }

    @Override
    public void reparent(Collection<Node> nodes, Container newParent) {
        groupImpl.reparent(nodes, newParent);
    }

    @Override
    public void reparent(Collection<Node> nodes) {
        groupImpl.reparent(nodes);
    }

    @Override
    public Point2D getCenterInLocalSpace() {
        return new Point2D.Double(0, 0);
    }

    @Override
    public void copyStyle(Stylable src) {
        super.copyStyle(src);
        if (src instanceof VisualGroup) {
            VisualGroup srcGroup = (VisualGroup) src;
            setIsCollapsed(srcGroup.getIsCollapsed());
        }
    }

}
