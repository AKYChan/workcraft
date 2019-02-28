package org.workcraft.shared;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;

public class DataAccumulator extends OutputStream {

    private final LinkedList<byte[]> dataChunks = new LinkedList<>();

    public static byte[] loadStream(InputStream is) throws IOException {
        DataAccumulator accum = new DataAccumulator();

        while (true) {
            int available = is.available();
            if (available == 0) {
                available = 1024;
            }
            byte[] chunk = new byte[available];
            int read = is.read(chunk, 0, available);
            if (read == -1) {
                break;
            }
            if (read == 0) {
                continue;
            }
            if (read != available) {
                chunk = Arrays.copyOfRange(chunk, 0, read);
            }
            accum.write(chunk);
        }

        return accum.getData();
    }

    private void addDataChunk(byte[] data) {
        dataChunks.add(data);
    }

    public byte[] getData() {
        int len = 0;
        for (byte[] dataChunk : dataChunks) {
            len += dataChunk.length;
        }

        byte[] result = new byte[len];

        int cur = 0;
        for (byte[] dataChunk : dataChunks) {
            for (int i = 0; i < dataChunk.length; i++) {
                result[cur++] = dataChunk[i];
            }
        }

        return result;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(getData());
    }

    @Override
    public void write(int b) throws IOException {
        addDataChunk(new byte[] {(byte) b});
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] chunk = new byte[len];
        System.arraycopy(b, off, chunk, 0, len);
        addDataChunk(chunk);
    }

    @Override
    public void write(byte[] b) throws IOException {
        addDataChunk(b.clone());
    }

}
