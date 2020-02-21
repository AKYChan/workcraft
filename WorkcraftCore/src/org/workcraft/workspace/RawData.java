package org.workcraft.workspace;

import org.workcraft.shared.DataAccumulator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class RawData {

    private final byte[] data;

    public RawData(InputStream is) throws IOException {
        this.data = DataAccumulator.loadStream(is);
    }

    public RawData(ByteArrayOutputStream os) {
        this.data = os.toByteArray();
    }

    public InputStream toStream() {
        return new ByteArrayInputStream(data);
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(data, data.length);
    }

}
