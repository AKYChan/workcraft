package org.workcraft.plugins.cpog;

import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.observation.PropertyChangedEvent;

public class VisualScenarioPage extends VisualPage {

	public VisualScenarioPage(MathNode refNode) {
		super(refNode);
		// TODO Auto-generated constructor stub

		addPropertyDeclaration(new PropertyDeclaration<VisualScenarioPage, Encoding>(
				this, "Encoding", Encoding.class) {
			public void setter(VisualScenarioPage object, Encoding value) {
				object.setEncoding(value);
			}
			public Encoding getter(VisualScenarioPage object) {
				return object.getEncoding();
			}
		});
	}

	private Encoding encoding = new Encoding();

	public void setEncoding(Encoding encoding) {
		this.encoding = encoding;
		sendNotification(new PropertyChangedEvent(this, "encoding"));
	}

	public Encoding getEncoding() {
		return encoding;
	}



}
