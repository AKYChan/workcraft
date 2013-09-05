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

package org.workcraft.plugins.sdfs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.SVGIcon;
import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.gui.Coloriser;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.plugins.sdfs.decorations.BinaryRegisterDecoration;

@DisplayName ("Push")
@SVGIcon("images/icons/svg/sdfs-push_register.svg")
public class VisualPushRegister extends VisualComponent {

	public VisualPushRegister(PushRegister register) {
		super(register);
		addPropertyDeclarations();
	}

	public PushRegister getReferencedPushRegister() {
		return (PushRegister)getReferencedComponent();
	}

	private void addPropertyDeclarations() {
		LinkedHashMap<String, Object> markingChoice = new LinkedHashMap<String, Object>();
		for (ControlRegister.Marking marking : ControlRegister.Marking.values())
			markingChoice.put(marking.name, marking);

		addPropertyDeclaration(new PropertyDeclaration(this, "Marking", "getMarking", "setMarking",
				ControlRegister.Marking.class, markingChoice));
	}

	@Override
	public void draw(DrawRequest r) {
		Graphics2D g = r.getGraphics();
		Decoration d = r.getDecoration();
		double w = size - strokeWidth;
		double h = size - strokeWidth;
		double w2 = w/2;
		double h2 = h/2;
		double dx = size / 5;
		double dy = strokeWidth / 4;
		double dt = (size - strokeWidth) / 8;
		float strokeWidth1 = (float)strokeWidth;
		float strokeWidth2 = strokeWidth1 / 2;

		Shape shape = new Rectangle2D.Double(-w2, -h2, w, h);

		Path2D trueInnerShape = new Path2D.Double();
		trueInnerShape.moveTo(-w2 + dx, +h2 - dy);
		trueInnerShape.lineTo(-w2 + dx, -h2 + dy);
		trueInnerShape.moveTo(+w2 - dx, +h2 - dy);
		trueInnerShape.lineTo(+w2 - dx, -h2 + dy);

		Path2D falseInnerShape = new Path2D.Double();
		falseInnerShape.moveTo(+w2 - dx, +h2 - dy);
		falseInnerShape.lineTo(+w2 - dx, +h2 - 2 * dt);
		falseInnerShape.lineTo(       0, +h2 - dy);
		falseInnerShape.lineTo(-w2 + dx, +h2 - 2 * dt);
		falseInnerShape.lineTo(-w2 + dx, +h2 - dy);

		Shape tokenShape = new Ellipse2D.Double(-dt , -dt, 2 * dt, 2 * dt);

		boolean trueMarked = isTrueMarked();
		boolean trueExcited = false;
		boolean falseMarked = isFalseMarked();
		boolean falseExcited = false;
		Color defaultColor = Coloriser.colorise(getForegroundColor(), d.getColorisation());
		if (d instanceof BinaryRegisterDecoration) {
			trueMarked = ((BinaryRegisterDecoration)d).isTrueMarked();
			trueExcited = ((BinaryRegisterDecoration)d).isTrueExcited();
			falseMarked = ((BinaryRegisterDecoration)d).isFalseMarked();
			falseExcited = ((BinaryRegisterDecoration)d).isFalseExcited();
			defaultColor = getForegroundColor();
		}

		g.setColor(Coloriser.colorise(getFillColor(), d.getBackground()));
		g.fill(shape);

		g.setStroke(new BasicStroke(strokeWidth2));
		if (falseExcited) {
			g.setColor(Coloriser.colorise(getForegroundColor(), d.getColorisation()));
		} else {
			g.setColor(defaultColor);
		}
		g.draw(falseInnerShape);
		if (trueExcited) {
			g.setColor(Coloriser.colorise(getForegroundColor(), d.getColorisation()));
		} else {
			g.setColor(defaultColor);
		}
		g.draw(trueInnerShape);

		if (trueExcited || falseExcited) {
			g.setColor(Coloriser.colorise(getForegroundColor(), d.getColorisation()));
		} else {
			g.setColor(defaultColor);
		}
		g.setStroke(new BasicStroke(strokeWidth1));
		g.draw(shape);

		g.setStroke(new BasicStroke(strokeWidth2));
		if (trueMarked) {
			g.fill(tokenShape);
		}
		if (falseMarked) {
			g.draw(tokenShape);
		}

		drawLabelInLocalSpace(r);
	}

	public PushRegister.Marking getMarking() {
		return getReferencedPushRegister().getMarking();
	}

	public void setMarking(PushRegister.Marking value) {
		getReferencedPushRegister().setMarking(value);
	}

	public boolean isFalseMarked() {
		return getReferencedPushRegister().isFalseMarked();
	}

	public boolean isTrueMarked() {
		return getReferencedPushRegister().isTrusMarked();
	}

}
