package dev.michey.expo.client;

import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.server.packet.Packet;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ExpoClientPacketEvaluator {

    private final ExpoClientPacketReader packetReader;
    private final ConcurrentLinkedQueue<Packet> incomingPacketQueue;

    public ExpoClientPacketEvaluator(ExpoClientPacketReader packetReader) {
        this.packetReader = packetReader;
        incomingPacketQueue = new ConcurrentLinkedQueue<>();
    }

    public void queuePacket(Packet packet) {
        incomingPacketQueue.add(packet);
    }

    public void handlePackets() {
        while(!incomingPacketQueue.isEmpty()) {
            var packet = incomingPacketQueue.poll();
            packetReader.handlePacket(packet, ExpoServerLocal.get() != null);
        }
    }

    public int getQueuedPacketAmount() {
        return incomingPacketQueue.size();
    }

}
