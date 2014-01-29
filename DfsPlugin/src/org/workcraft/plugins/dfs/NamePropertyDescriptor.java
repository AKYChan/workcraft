package org.workcraft.plugins.dfs;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.workcraft.dom.Node;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;

public class NamePropertyDescriptor implements PropertyDescriptor {
	private final Dfs model;
	private final Node node;

	public NamePropertyDescriptor(Dfs model, Node node) {
		this.model = model;
		this.node = node;
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	@Override
	public Object getValue() throws InvocationTargetException {
		return model.getName(node);
	}

	@Override
	public void setValue(Object value) throws InvocationTargetException {
		model.setName(node, (String)value);
	}

	@Override
	public Map<Object, String> getChoice() {
		return null;
	}

	@Override
	public String getName() {
		return "Name";
	}

	@Override
	public Class<?> getType() {
		return String.class;
	}

	@Override
	public boolean isCombinable() {
		return false;
	}

}
