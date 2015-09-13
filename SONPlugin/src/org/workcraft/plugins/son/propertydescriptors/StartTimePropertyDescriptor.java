package org.workcraft.plugins.son.propertydescriptors;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.plugins.son.Interval;
import org.workcraft.plugins.son.elements.Time;

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
		Interval value = t.getStartTime();
		return value.toString();
	}

	@Override
	public void setValue(Object value) throws InvocationTargetException {
		String input = (String)value;
		Interval result = new Interval(Interval.getMin(input), Interval.getMax(input));
		t.setStartTime(result);
	}

	@Override
	public Map<? extends Object, String> getChoice() {
		return null;
	}

}
