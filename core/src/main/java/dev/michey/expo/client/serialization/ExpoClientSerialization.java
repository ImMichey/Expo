package dev.michey.expo.client.serialization;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.DiscoverHost;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.FrameworkMessage.Ping;
import com.esotericsoftware.kryonet.FrameworkMessage.RegisterTCP;
import com.esotericsoftware.kryonet.FrameworkMessage.RegisterUDP;
import com.esotericsoftware.kryonet.serialization.Serialization;
import dev.michey.expo.Expo;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;

public class ExpoClientSerialization implements Serialization {

    private final Kryo kryo;
    private final ByteBufferInput input;
    private final ByteBufferOutput output;

    public ExpoClientSerialization() {
        this(new Kryo());

        this.kryo.setReferences(false);
        this.kryo.setRegistrationRequired(true);
    }

    public ExpoClientSerialization(Kryo kryo) {
        this.kryo = kryo;

        this.kryo.register(RegisterTCP.class);
        this.kryo.register(RegisterUDP.class);
        this.kryo.register(KeepAlive.class);
        this.kryo.register(DiscoverHost.class);
        this.kryo.register(Ping.class);

        this.input = new ByteBufferInput();
        this.output = new ByteBufferOutput();
    }

    public Kryo getKryo() {
        return kryo;
    }

    @Override
    public synchronized void write(Connection connection, ByteBuffer buffer, Object object) {
        int startPos = buffer.position();
        output.setBuffer(buffer);
        kryo.getContext().put("connection", connection);
        kryo.writeClassAndObject(output, object);
        int totalBytes = buffer.position() - startPos;
        if(ExpoClientContainer.get() != null) ExpoClientContainer.get().getClient().bytesOutTcp += totalBytes;
        output.flush();
    }

    @Override
    public synchronized Object read(Connection connection, ByteBuffer buffer) {
        int startPos = buffer.position();
        input.setBuffer(buffer);
        kryo.getContext().put("connection", connection);
        Object o = kryo.readClassAndObject(input);
        int totalBytes = buffer.position() - startPos;
        if(ExpoClientContainer.get() != null) ExpoClientContainer.get().getClient().bytesInTcp += totalBytes;
        return o;
    }

    @Override
    public void writeLength(ByteBuffer buffer, int length) {
        buffer.putInt(length);
    }

    @Override
    public int readLength(ByteBuffer buffer) {
        return buffer.getInt();
    }

    @Override
    public int getLengthLength() {
        return 4;
    }
}