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

package org.workcraft.plugins.xmas.components;

import java.awt.Color;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.Hotkey;
import org.workcraft.annotations.SVGIcon;
import org.workcraft.dom.visual.Positioning;
import org.workcraft.dom.visual.Stylable;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;

@DisplayName("Queue")
@Hotkey(KeyEvent.VK_Q)
@SVGIcon("images/icons/svg/xmas-queue.svg")
public class VisualQueueComponent extends VisualXmasComponent {
	public static final String PROPERTY_FOREGROUND_COLOR = "Foreground color";

	public Color color = new Color(0, 0, 0, 255);

	public VisualQueueComponent(QueueComponent component) {
		super(component);
		addPropertyDeclarations();
		component.setCapacity(1);
		if (component.getChildren().isEmpty()) {
			this.addInput("i", Positioning.LEFT);
			this.addOutput("o", Positioning.RIGHT);
		}
	}

	private void addPropertyDeclarations() {
		addPropertyDeclaration(new PropertyDeclaration<VisualQueueComponent, Integer>(
				this, QueueComponent.PROPERTY_CAPACITY, Integer.class, true, true, true) {
			public void setter(VisualQueueComponent object, Integer value) {
				object.getReferencedQueueComponent().setCapacity(value);
			}
			public Integer getter(VisualQueueComponent object) {
				return object.getReferencedQueueComponent().getCapacity();
			}
		});
	}

	public QueueComponent getReferencedQueueComponent() {
		return (QueueComponent)getReferencedComponent();
	}

	@Override
	public Shape getShape() {
		Path2D shape = new Path2D.Double();

		shape.moveTo(-0.5 * size, -0.4 * size);
		shape.lineTo(-0.5 * size, +0.4 * size);
		shape.lineTo(+0.5 * size, +0.4 * size);
		shape.lineTo(+0.5 * size, -0.4 * size);
		shape.closePath();

		shape.moveTo(0.0, -0.4 * size);
		shape.lineTo(0.0, +0.4 * size);

		return shape;
	}

	@Override
	public void copyStyle(Stylable src) {
		super.copyStyle(src);
		if (src instanceof VisualQueueComponent) {
			QueueComponent srcComponent = ((VisualQueueComponent)src).getReferencedQueueComponent();
			getReferencedQueueComponent().setCapacity(srcComponent.getCapacity());
			getReferencedQueueComponent().setInit(srcComponent.getInit());
		}
	}

}
