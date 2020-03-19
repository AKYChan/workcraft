package org.workcraft.plugins.builtin.serialisation.xml;

import org.w3c.dom.Element;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.serialisation.BasicXMLSerialiser;

import java.io.File;
import java.io.IOException;

public class FileSerialiser implements BasicXMLSerialiser<File> {

    @Override
    public String getClassName() {
        return File.class.getName();
    }

    @Override
    public void serialise(Element element, File object) throws SerialisationException {
        if (object != null) {
            try {
                element.setAttribute("path", object.getCanonicalPath());
            } catch (IOException e) {
                throw new SerialisationException(e);
            }
        }
    }

}
