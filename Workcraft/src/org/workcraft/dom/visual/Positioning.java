package org.workcraft.dom.visual;

import java.util.LinkedHashMap;
import java.util.Map;

public enum Positioning {
	TOP("Top", 0, -1),
	BOTTOM("Bottom", 0, 1),
	LEFT("Left", -1, 0),
	RIGHT("Right", 1, 0),
	CENTER("Center", 0, 0),
	TOP_LEFT("Top-Left", -1, -1),
	TOP_RIGHTLEFT("Top-Right", 1, -1),
	BOTTOM_LEFT("Bottom-Left", -1, 1),
	BOTTOM_RIGHT("Bottom-Right", 1, 1);

	public final String name;
	public final int dx, dy;

	private Positioning(String name, int dx, int dy) {
		this.name = name;
		this.dx = dx;
		this.dy = dy;
	}

	static public Map<String, Positioning> getChoice() {
		LinkedHashMap<String, Positioning> choice = new LinkedHashMap<String, Positioning>();
		for (Positioning item : Positioning.values()) {
			choice.put(item.name, item);
		}
		return choice;
	}
}
