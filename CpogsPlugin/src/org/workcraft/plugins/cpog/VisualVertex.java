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

package org.workcraft.plugins.cpog;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.Hotkey;
import org.workcraft.annotations.SVGIcon;
import org.workcraft.dom.visual.BoundingBoxHelper;
import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.gui.Coloriser;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.cpog.expressions.CpogFormulaVariable;
import org.workcraft.plugins.cpog.expressions.CpogVisitor;
import org.workcraft.plugins.cpog.optimisation.BooleanFormula;
import org.workcraft.plugins.cpog.optimisation.booleanvisitors.PrettifyBooleanReplacer;
import org.workcraft.plugins.cpog.optimisation.expressions.One;
import org.workcraft.plugins.cpog.optimisation.expressions.Zero;

@Hotkey(KeyEvent.VK_V)
@DisplayName("Vertex")
@SVGIcon("images/icons/svg/vertex.svg")
public class VisualVertex extends VisualComponent implements CpogFormulaVariable {
	public static final String PROPERTY_CONDITION = "Condition";
	public static Font conditionFont;
	private RenderedFormula conditionRenderedFormula = new RenderedFormula("", One.instance(), conditionFont, getLabelPositioning(), getLabelOffset());

	static {
		try {
			Font font = Font.createFont(Font.TYPE1_FONT, ClassLoader.getSystemResourceAsStream("fonts/default.pfb"));
			conditionFont = font.deriveFont(0.5f);
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public VisualVertex(Vertex vertex) {
		super(vertex);
		removePropertyDeclarationByName("Name positioning");
		removePropertyDeclarationByName("Name color");
	}

	@Override
	public void draw(DrawRequest r) {
		Graphics2D g = r.getGraphics();
		Color colorisation = r.getDecoration().getColorisation();
		Color background = r.getDecoration().getBackground();
		Shape shape = new Ellipse2D.Double(
				-size / 2 + strokeWidth / 2, -size / 2 + strokeWidth / 2,
				size - strokeWidth, size - strokeWidth);

		BooleanFormula value = evaluate();
		g.setColor(Coloriser.colorise(getFillColor(), background));
		g.fill(shape);
		g.setColor(Coloriser.colorise(getForegroundColor(), colorisation));
		if (value == Zero.instance()) {
			g.setStroke(new BasicStroke((float)strokeWidth, BasicStroke.CAP_BUTT,
			        BasicStroke.JOIN_MITER, 1.0f, new float[] {0.18f, 0.18f}, 0.00f));
		} else {
			g.setStroke(new BasicStroke((float)strokeWidth));
			if (value != One.instance())
				g.setColor(Coloriser.colorise(Color.LIGHT_GRAY, colorisation));
		}
		g.draw(shape);
		drawConditionInLocalSpace(r);
	}

	protected void cacheConditionRenderedFormula(DrawRequest r) {
		String text = getLabel();
		if (getCondition() != One.instance()) {
			text += ": ";
		}
		if (conditionRenderedFormula.isDifferent(text, getCondition(), conditionFont, getLabelPositioning(), getLabelOffset())) {
			conditionRenderedFormula = new RenderedFormula(text, getCondition(), conditionFont, getLabelPositioning(), getLabelOffset());
		}
	}

	protected void drawConditionInLocalSpace(DrawRequest r) {
		if (getLabelVisibility()) {
			Graphics2D g = r.getGraphics();
			Decoration d = r.getDecoration();
			cacheConditionRenderedFormula(r);
			if ((conditionRenderedFormula != null) && !conditionRenderedFormula.isEmpty()) {
				g.setColor(Coloriser.colorise(getLabelColor(), d.getColorisation()));
				conditionRenderedFormula.draw(g);
			}
		}
	}

	public Vertex getMathVertex() {
		return (Vertex) getReferencedComponent();
	}

	public BooleanFormula getCondition() {
		return getMathVertex().getCondition();
	}

	public void setCondition(BooleanFormula condition) {
		getMathVertex().setCondition(condition);
		sendNotification(new PropertyChangedEvent(this, PROPERTY_CONDITION));
	}

	public BooleanFormula evaluate() {
		return getCondition().accept(new PrettifyBooleanReplacer());
	}

	@Override
	public <T> T accept(CpogVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Rectangle2D getBoundingBoxInLocalSpace() {
		Rectangle2D bb = super.getBoundingBoxInLocalSpace();
		if (getLabelVisibility() && (conditionRenderedFormula != null) && !conditionRenderedFormula.isEmpty()) {
			bb = BoundingBoxHelper.union(bb, conditionRenderedFormula.getBoundingBox());
		}
		return bb;
	}

	@Override
	public boolean hitTestInLocalSpace(Point2D pointInLocalSpace) {
		return pointInLocalSpace.distanceSq(0, 0) < size * size / 4;
	}

}
