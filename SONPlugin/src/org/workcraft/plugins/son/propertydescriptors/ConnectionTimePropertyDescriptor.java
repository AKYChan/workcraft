package org.workcraft.plugins.son.propertydescriptors;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.plugins.son.algorithm.TimeAlg;
import org.workcraft.plugins.son.connections.SONConnection;

public class ConnectionTimePropertyDescriptor implements PropertyDescriptor{
	private final SONConnection con;

	public ConnectionTimePropertyDescriptor(SONConnection con) {
		this.con = con;
	}

	@Override
	public String getName() {
		return "Time interval";
	}

	@Override
	public Class<?> getType() {
		return String.class;
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public boolean isCombinable() {
		return false;
	}

	@Override
	public boolean isTemplatable() {
		return false;
	}

	@Override
	public Object getValue() throws InvocationTargetException {
		return con.getTime();
	}

	@Override
	public void setValue(Object value) throws InvocationTargetException {
		String input = (String)value;

		final Framework framework = Framework.getInstance();
		MainWindow mainWindow = framework.getMainWindow();

		if(!TimeAlg.isValidInput(input)){
			input = con.getTime();
			JOptionPane.showMessageDialog(mainWindow,
					"Input value is not a valid time interval.\n"
							+ "The format must be xxxx-xxxx, where x is positive integer.",
					"Cannot change property", JOptionPane.WARNING_MESSAGE);
		}
		con.setTime(input);
	}

	@Override
	public Map<? extends Object, String> getChoice() {
		return null;
	}

}
