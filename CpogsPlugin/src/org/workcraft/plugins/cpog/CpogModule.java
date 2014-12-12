package org.workcraft.plugins.cpog;

import org.workcraft.Framework;
import org.workcraft.Initialiser;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Tool;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.gui.propertyeditor.PropertyClassProvider;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.plugins.cpog.serialisation.ArcDeserialiser;
import org.workcraft.plugins.cpog.serialisation.ArcSerialiser;
import org.workcraft.plugins.cpog.serialisation.RhoClauseDeserialiser;
import org.workcraft.plugins.cpog.serialisation.RhoClauseSerialiser;
import org.workcraft.plugins.cpog.serialisation.VertexDeserialiser;
import org.workcraft.plugins.cpog.serialisation.VertexSerialiser;
import org.workcraft.plugins.cpog.serialisation.VisualCPOGGroupDeserialiser;
import org.workcraft.plugins.cpog.serialisation.VisualCPOGGroupSerialiser;
import org.workcraft.plugins.cpog.tools.EncoderPreferencesTool;
import org.workcraft.plugins.cpog.tools.GraphStatisticsTool;
import org.workcraft.serialisation.xml.XMLDeserialiser;
import org.workcraft.serialisation.xml.XMLSerialiser;

public class CpogModule implements Module {
	@Override
	public void init() {
		final Framework framework = Framework.getInstance();
		final PluginManager pm = framework.getPluginManager();

		pm.registerClass(ModelDescriptor.class, CpogModelDescriptor.class);

		pm.registerClass(PropertyClassProvider.class, EncodingPropertyProvider.class);

		pm.registerClass(XMLSerialiser.class, VisualCPOGGroupSerialiser.class);
		pm.registerClass(XMLSerialiser.class, VertexSerialiser.class);
		pm.registerClass(XMLSerialiser.class, RhoClauseSerialiser.class);
		pm.registerClass(XMLSerialiser.class, ArcSerialiser.class);

		pm.registerClass(XMLDeserialiser.class, VisualCPOGGroupDeserialiser.class);
		pm.registerClass(XMLDeserialiser.class, VertexDeserialiser.class);
		pm.registerClass(XMLDeserialiser.class, RhoClauseDeserialiser.class);
		pm.registerClass(XMLDeserialiser.class, ArcDeserialiser.class);
		pm.registerClass(Settings.class, CpogSettings.class);

		//p.registerClass(Tool.class, CpogEncoder.class);

		pm.registerClass(Tool.class, EncoderPreferencesTool.class);

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new GraphStatisticsTool();
			}
		});
	}

	@Override
	public String getDescription() {
		return "Conditional Partial Order Graphs";
	}
}
