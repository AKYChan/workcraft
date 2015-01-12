package org.workcraft.plugins.dfs;

import org.workcraft.observation.PropertyChangedEvent;

public class BinaryRegister extends MathDelayNode {

	public enum Marking {
		EMPTY("empty"),
		FALSE_TOKEN("false"),
		TRUE_TOKEN("true");

		private final String name;

		private Marking(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private Marking marking = Marking.EMPTY;

	public Marking getMarking() {
		return marking;
	}

	public void setMarking(Marking value) {
		this.marking = value;
		sendNotification(new PropertyChangedEvent(this, "marking"));
	}

	public boolean isFalseMarked() {
		return (this.marking == Marking.FALSE_TOKEN);
	}

	public boolean isTrueMarked() {
		return (this.marking == Marking.TRUE_TOKEN);
	}

}
