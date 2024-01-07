package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.Pair;

public class ExpoPacketEvaluatorLocal extends ExpoPacketEvaluator {

    public ExpoPacketEvaluatorLocal(ExpoServerPacketReader packetReader) {
        super(packetReader);
    }

    @Override
    public void queuePacket(Connection connection, Packet packet) {
        incomingPacketQueue.add(new Pair<>(null, packet));
    }

    @Override
    public void handlePackets() {
        while(!incomingPacketQueue.isEmpty()) {
            var packet = incomingPacketQueue.poll();
            packetReader.handlePacket(packet.value);
        }
    }

}
