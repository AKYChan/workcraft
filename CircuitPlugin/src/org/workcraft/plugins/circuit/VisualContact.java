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

package org.workcraft.plugins.circuit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.gui.Coloriser;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.observation.StateEvent;
import org.workcraft.observation.StateObserver;
import org.workcraft.observation.TransformChangedEvent;
import org.workcraft.observation.TransformChangingEvent;
import org.workcraft.plugins.circuit.Contact.IOType;
import org.workcraft.plugins.circuit.renderers.ComponentRenderingResult.RenderType;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.serialisation.xml.NoAutoSerialisation;


public class VisualContact extends VisualComponent implements StateObserver {

	public enum Direction {
		WEST("West"),
		NORTH("North"),
		EAST("East"),
		SOUTH("South");

		private final String name;

		private Direction(String name) {
			this.name = name;
		}

		static public AffineTransform getDirectionTransform(Direction dir) {
			AffineTransform at = new AffineTransform();
			if (dir!=null) {
				switch (dir) {
				case WEST:
					at.quadrantRotate(2);
					break;
				case NORTH:
					at.quadrantRotate(3);
					break;
				case EAST:
					at.setToIdentity();
					break;
				case SOUTH:
					at.quadrantRotate(1);
					break;
				}
			}
			return at;
		}

		static public Map<String, Direction> getChoice() {
			LinkedHashMap<String, Direction> choice = new LinkedHashMap<String, Direction>();
			for (Direction item : Direction.values()) {
				choice.put(item.name, item);
			}
			return choice;
		}

		public Direction rotateClockwise() {
			switch (this) {
			case WEST: return NORTH;
			case NORTH: return EAST;
			case EAST: return SOUTH;
			case SOUTH: return WEST;
			default: return this;
			}
		}

		public Direction rotateCounterclockwise() {
			switch (this) {
			case WEST: return SOUTH;
			case NORTH: return WEST;
			case EAST: return NORTH;
			case SOUTH: return EAST;
			default: return this;
			}
		}

		public Direction flipHorizontal() {
			switch (this) {
			case WEST: return EAST;
			case EAST: return WEST;
			default: return this;
			}
		}

		public Direction flipVertical() {
			switch (this) {
			case NORTH: return SOUTH;
			case SOUTH: return NORTH;
			default: return this;
			}
		}

		public Direction flip() {
			switch (this) {
			case WEST: return EAST;
			case NORTH: return SOUTH;
			case EAST: return WEST;
			case SOUTH: return NORTH;
			default: return this;
			}
		}

	};

	public static final Color inputColor = Color.RED;
	public static final Color outputColor = Color.BLUE;

	private static Font nameFont = new Font("Sans-serif", Font.PLAIN, 1).deriveFont(0.5f);
	private double size = 0.3;
	private GlyphVector nameGlyph = null;
	private Direction direction = Direction.WEST;

	private HashSet<SignalTransition> referencedTransitions=new HashSet<SignalTransition>();
	private Place referencedZeroPlace=null;
	private Place referencedOnePlace=null;

	public VisualContact(Contact contact) {
		super(contact, true, false, false);
		setDirection(contact.getIOType()==IOType.INPUT ? Direction.WEST : Direction.EAST);
		contact.addObserver(this);
		addPropertyDeclarations();
	}

	private Shape getShape() {
		if (getParent() instanceof VisualCircuitComponent) {
			return new Rectangle2D.Double(
					-size / 2 + CircuitSettings.getWireWidth(),
					-size / 2 + CircuitSettings.getWireWidth(),
					size - CircuitSettings.getWireWidth() * 2,
					size - CircuitSettings.getWireWidth() * 2);
		} else {
			Path2D path = new Path2D.Double();
			path.moveTo(-size / 2, -size / 2);
			path.lineTo(0, -size / 2);
			path.lineTo(size / 2, 0);
			path.lineTo(0, size / 2);
			path.lineTo(-size / 2, size / 2);
			path.closePath();
			return path;
		}
	}

	private void addPropertyDeclarations() {
		addPropertyDeclaration(new PropertyDeclaration<VisualContact, Direction>(
				this, "Direction", Direction.class, Direction.getChoice()) {
			protected void setter(VisualContact object, Direction value) {
				object.setDirection(value);
			}
			protected Direction getter(VisualContact object) {
				return object.getDirection();
			}
		});

		addPropertyDeclaration(new PropertyDeclaration<VisualContact, IOType>(
				this, "I/O type", IOType.class, IOType.getChoice()) {
			protected void setter(VisualContact object, IOType value) {
				object.setIOType(value);
			}
			protected IOType getter(VisualContact object) {
				return object.getIOType();
			}
		});

		addPropertyDeclaration(new PropertyDeclaration<VisualContact, Boolean>(
				this, "Init to one", Boolean.class) {
			protected void setter(VisualContact object, Boolean value) {
				object.setInitOne(value);
			}
			protected Boolean getter(VisualContact object) {
				return object.getInitOne();
			}
		});
	}

	@NoAutoSerialisation
	public boolean getInitOne() {
		return getReferencedContact().getInitOne();
	}

	@NoAutoSerialisation
	public void setInitOne(boolean value) {
		getReferencedContact().setInitOne(value);
	}

	@Override
	public void draw(DrawRequest r) {
		Graphics2D g = r.getGraphics();
		Decoration d = r.getDecoration();

		boolean inSimulationMode = ((d.getColorisation() != null) || (d.getBackground() != null));
		Color colorisation = d.getColorisation();
		Color fillColor = d.getBackground();
		if (fillColor == null) {
			fillColor = getFillColor();
		}

		AffineTransform oldTransform = g.getTransform();
		AffineTransform at = Direction.getDirectionTransform(getDirection());
		if (getIOType()==IOType.INPUT) {
			at.quadrantRotate(2);
		}
		g.transform(at);

		if (inSimulationMode || CircuitSettings.getShowContacts() || !(getParent() instanceof VisualCircuitComponent)) {
			Shape shape = getShape();
			g.setStroke(new BasicStroke((float)CircuitSettings.getWireWidth()));
			g.setColor(fillColor);
			g.fill(shape);
			g.setColor(Coloriser.colorise(getForegroundColor(), colorisation));
			g.draw(shape);
		} else if (r.getModel().getConnections(this).size() > 1) {
			g.setColor(Coloriser.colorise(getForegroundColor(), colorisation));
			g.fill(VisualJoint.shape);
		}
		g.setTransform(oldTransform);

		if ( !(getParent() instanceof VisualCircuitComponent) ) {
			at.setToIdentity();
			if (getDirection() == Direction.NORTH || getDirection() == Direction.SOUTH) {
				at.quadrantRotate(-1);
			}
			g.transform(at);

			GlyphVector gv = getNameGlyphs(r);
			Rectangle2D cur = gv.getVisualBounds();
			g.setColor(Coloriser.colorise((getIOType()==IOType.INPUT)?inputColor:outputColor, colorisation));

			float xx = (float)size;
			if (getDirection() == Direction.SOUTH || getDirection() == Direction.WEST) {
				xx = (float)(-cur.getWidth() - size);
			}
			g.drawGlyphVector(gv,xx, (float)size/2);
		}
	}

	@Override
	public Rectangle2D getBoundingBoxInLocalSpace() {
		Shape shape = getShape();
		if (shape != null) {
			return shape.getBounds2D();
		} else {
			return new Rectangle2D.Double(-size/2, -size/2, size, size);
		}
	}

	@Override
	public boolean hitTestInLocalSpace(Point2D pointInLocalSpace) {
		Point2D p2 = new Point2D.Double();
		p2.setLocation(pointInLocalSpace);
		if (!(getParent() instanceof VisualCircuitComponent)) {
			AffineTransform at = new AffineTransform();
			// rotate in the direction opposite to Direction.getDirectionTransform
			switch (getDirection()) {
			case WEST:
				at.quadrantRotate(2);
				break;
			case NORTH:
				at.quadrantRotate(1);
				break;
			case EAST:
				at.setToIdentity();
				break;
			case SOUTH:
				at.quadrantRotate(3);
				break;
			}
			if (getIOType()==IOType.INPUT) {
				at.quadrantRotate(2);
			}
			at.transform(pointInLocalSpace, p2);
		}
		Shape shape = getShape();
		if (shape != null) {
			return shape.contains(p2);
		} else {
			return false;
		}
	}

	public void invalidateNameGlyph() {
		nameGlyph = null;
	}

	public GlyphVector getNameGlyphs(DrawRequest r) {
		if (nameGlyph == null) {
			final FontRenderContext context = new FontRenderContext(AffineTransform.getScaleInstance(1000.0, 1000.0), true, true);
			Circuit circuit = (Circuit)r.getModel().getMathModel();
			String name = circuit.getName(this.getReferencedContact());
			nameGlyph = nameFont.createGlyphVector(context, name);
		}
		return nameGlyph;
	}

	public void setDirection(Direction dir) {
		if (dir != direction) {
			sendNotification(new TransformChangingEvent(this));
			invalidateNameGlyph();
			this.direction = dir;
			sendNotification(new PropertyChangedEvent(this, "direction"));
			sendNotification(new TransformChangedEvent(this));
		}
	}

	public Direction getDirection() {
		return direction;
	}

	@NoAutoSerialisation
	public void setIOType(IOType type) {
		getReferencedContact().setIOType(type);
	}

	@NoAutoSerialisation
	public IOType getIOType() {
		return getReferencedContact().getIOType();
	}

	@NoAutoSerialisation
	public String getName() {
		return getReferencedContact().getName();
	}

	@NoAutoSerialisation
	public void setName(String name) {
		getReferencedContact().setName(name);
	}

	public Contact getReferencedContact() {
		return (Contact)getReferencedComponent();
	}

	public boolean isInput() {
		return getReferencedContact().isInput();
	}

	public boolean isOutput() {
		return getReferencedContact().isOutput();
	}

	public boolean isPort() {
		return getReferencedContact().isPort();
	}

	public boolean isDriver() {
		return getReferencedContact().isDriver();
	}

	public boolean isDriven() {
		return getReferencedContact().isDriven();
	}

	public HashSet<SignalTransition> getReferencedTransitions() {
		return referencedTransitions;
	}

	@Override
	public void notify(StateEvent e) {
		if (e instanceof PropertyChangedEvent) {
			PropertyChangedEvent pc = (PropertyChangedEvent)e;
			if (pc.getPropertyName().equals("name")) {
				invalidateNameGlyph();
			}
		}
	}

	public void setReferencedOnePlace(Place referencedOnePlace) {
		this.referencedOnePlace = referencedOnePlace;
	}

	public Place getReferencedOnePlace() {
		return referencedOnePlace;
	}

	public void setReferencedZeroPlace(Place referencedZeroPlace) {
		this.referencedZeroPlace = referencedZeroPlace;
	}

	public Place getReferencedZeroPlace() {
		return referencedZeroPlace;
	}

	@Override
	public void rotateClockwise() {
		super.rotateClockwise();
		setDirection(getDirection().rotateClockwise());
	}

	@Override
	public void rotateCounterclockwise() {
		super.rotateCounterclockwise();
		setDirection(getDirection().rotateCounterclockwise());
	}

	@Override
	public void flipHorizontal() {
		super.flipHorizontal();
		setDirection(getDirection().flipHorizontal());
		if (getParent() instanceof VisualCircuitComponent) {
			VisualCircuitComponent component = (VisualCircuitComponent)getParent();
			if (component.getRenderType() == RenderType.BOX) {
				setX(-getX());
			}
		}
	}

	@Override
	public void flipVertical() {
		super.flipVertical();
		setDirection(getDirection().flipVertical());
		if (getParent() instanceof VisualCircuitComponent) {
			VisualCircuitComponent component = (VisualCircuitComponent)getParent();
			if (component.getRenderType() == RenderType.BOX) {
				setY(-getY());
			}
		}
	}

}
