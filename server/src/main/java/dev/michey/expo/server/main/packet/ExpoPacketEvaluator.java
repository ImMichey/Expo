package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.Pair;

import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class ExpoPacketEvaluator {

    public final ExpoServerPacketReader packetReader;
    public final ConcurrentLinkedQueue<Pair<Connection, Packet>> incomingPacketQueue;

    public ExpoPacketEvaluator(ExpoServerPacketReader packetReader) {
        this.packetReader = packetReader;
        incomingPacketQueue = new ConcurrentLinkedQueue<>();
    }

    public abstract void queuePacket(Connection connection, Packet packet);
    public abstract void handlePackets();

}
