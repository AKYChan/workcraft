package org.workcraft.plugins.son.elements;

import java.awt.Color;

import org.workcraft.annotations.VisualClass;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.shared.CommonVisualSettings;


@VisualClass (org.workcraft.plugins.son.elements.VisualCondition.class)
public class Condition extends NamedElement {

	private Color foregroundColor=CommonVisualSettings.getForegroundColor();
	private Color fillColor = CommonVisualSettings.getFillColor();
	private String label="";
	protected boolean token=false;

	public boolean hasToken() {
		return token;
	}

	public void setToken(boolean token) {
		this.token=token;
		sendNotification( new PropertyChangedEvent(this, "token") );
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
