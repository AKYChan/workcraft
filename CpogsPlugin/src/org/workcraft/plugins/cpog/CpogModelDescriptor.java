package org.workcraft.plugins.cpog;

import org.workcraft.dom.ModelDescriptor;
import org.workcraft.dom.VisualModelDescriptor;
import org.workcraft.dom.math.MathModel;

public class CpogModelDescriptor implements ModelDescriptor {
	@Override
	public String getDisplayName() {
		return "Conditional Partial Order Graph";
	}

	@Override
	public MathModel createMathModel() {
		return new CPOG();
	}

	@Override
	public VisualModelDescriptor getVisualModelDescriptor() {
		return new VisualCpogModelDescriptor();
	}

}
