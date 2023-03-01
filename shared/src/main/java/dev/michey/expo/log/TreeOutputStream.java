package dev.michey.expo.log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class TreeOutputStream extends OutputStream {

    private final List<OutputStream> streams;

    public TreeOutputStream() {
        streams = new LinkedList<>();
    }

    public void addStream(OutputStream stream) {
        streams.add(stream);
    }

    public void removeStream(OutputStream stream) {
        streams.remove(stream);
    }

    @Override
    public void write(int b) {
        streams.forEach(stream -> {
            try {
                stream.write(b);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}