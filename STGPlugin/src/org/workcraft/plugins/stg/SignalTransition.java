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

package org.workcraft.plugins.stg;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.VisualClass;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.exceptions.NotSupportedException;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.serialisation.xml.NoAutoSerialisation;

@DisplayName("Signal transition")
@VisualClass(org.workcraft.plugins.stg.VisualSignalTransition.class)
public class SignalTransition extends NamedTransition
{
	public enum Type {
		INPUT,
		OUTPUT,
		INTERNAL;

		@Override
		public String toString() {
			switch(this)
			{
			case INPUT:
				return "input";
			case OUTPUT:
				return "output";
			case INTERNAL:
				return "internal";
			default:
				throw new NotSupportedException();
			}
		}
	}

	public enum Direction {
		PLUS,
		MINUS,
		TOGGLE;

		public static Direction fromString(String s) {
			if (s.equals("+"))
				return PLUS;
			else if (s.equals("-"))
				return MINUS;
			else if (s.equals("~"))
				return TOGGLE;

			throw new ArgumentException ("Unexpected string: " + s);
		}

		@Override
		public String toString() {
			switch(this)
			{
			case PLUS:
				return "+";
			case MINUS:
				return "-";
			case TOGGLE:
				return "~";
			default:
				throw new NotSupportedException();
			}
		}
	}

	private Type type = Type.INTERNAL;
	private Direction direction = Direction.TOGGLE;
	private String signalName = null;

	public Type getSignalType() {
		return type;
	}

	public void setSignalType(Type type) {
		if (this.type != type) {
			this.type = type;
			sendNotification(new PropertyChangedEvent(this, "signalType"));
		}
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		if (this.direction != direction) {
			this.direction = direction;
			sendNotification(new PropertyChangedEvent(this, "direction"));
		}
	}

	@NoAutoSerialisation
	public String getSignalName() {
		return signalName;
	}

	@NoAutoSerialisation
	public void setSignalName(String signalName) {
		this.signalName = signalName;
		sendNotification(new PropertyChangedEvent(this, "signalName"));
	}

	@NoAutoSerialisation
	@Override
	public String getName() {
		final StringBuffer result = new StringBuffer(signalName);
		switch (direction) {
		case PLUS:
			result.append("+");
			break;
		case MINUS:
			result.append("-");
			break;
		case TOGGLE:
			result.append("~");
			break;
		}
		return result.toString();
	}

}