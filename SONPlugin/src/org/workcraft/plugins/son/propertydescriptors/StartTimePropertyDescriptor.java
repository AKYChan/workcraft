package org.workcraft.plugins.son.propertydescriptors;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.plugins.son.Time;

public class StartTimePropertyDescriptor implements PropertyDescriptor{
	private final Time t;

	public StartTimePropertyDescriptor(Time t) {
		this.t = t;
	}

	@Override
	public String getName() {
		return "Start time";
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
		return t.getStartTime();
	}

	@Override
	public void setValue(Object value) throws InvocationTargetException {
		t.setStartTime((String)value);
	}

	@Override
	public Map<? extends Object, String> getChoice() {
		return null;
	}

}
