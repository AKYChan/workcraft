package org.workcraft.utils;

import org.workcraft.Framework;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.plugins.PluginManager;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginUtils {

    public static Stream<ModelDescriptor> streamModelDescriptors() {
        PluginManager pm = Framework.getInstance().getPluginManager();
        return pm.getModelDescriptorPlugins().stream()
                .map(plugin -> plugin.getSingleton());
    }

    public static Collection<ModelDescriptor> getModelDescriptors() {
        return streamModelDescriptors().collect(Collectors.toList());
    }

    public static Collection<String> getSortedModelDisplayNames() {
        return getModelDescriptors().stream()
                .map(descriptor -> descriptor.getDisplayName())
                .filter(s -> (s != null) && !s.isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

}
