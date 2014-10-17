package org.workcraft.plugins.son.elements;

import java.awt.Color;

import org.workcraft.dom.math.MathNode;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.shared.CommonVisualSettings;

public class PlaceNode extends MathNode {

	private Color foregroundColor=CommonVisualSettings.getBorderColor();
	private Color fillColor = CommonVisualSettings.getFillColor();
	private String label = "";
	private int errors = 0;
	private boolean marked = false;

	public void setMarked(boolean token) {
		this.marked=token;
		sendNotification( new PropertyChangedEvent(this, "marked") );
	}

	public boolean isMarked() {
		return marked;
	}

	public void setErrors(int errors){
		this.errors = errors;
		sendNotification( new PropertyChangedEvent(this, "errors") );
	}

	public int getErrors(){
		return errors;
	}

	public Color getForegroundColor() {
		return foregroundColor;
	}

	public void setForegroundColor(Color foregroundColor) {
		this.foregroundColor = foregroundColor;
		sendNotification(new PropertyChangedEvent(this, "foregroundColor"));
	}

	public void setFillColor (Color fillColor){
		this.fillColor = fillColor;
		sendNotification(new PropertyChangedEvent(this, "fillColor"));
	}

	public Color getFillColor() {
		return fillColor;
	}

	public void setLabel(String label){
		this.label=label;
		sendNotification(new PropertyChangedEvent(this, "label"));
	}

	public String getLabel(){
		return label;
	}
}
