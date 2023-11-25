package dev.michey.expo.client;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ExpoClientListener implements Listener {

    /** Packet queue */
    private final ConcurrentLinkedQueue<Object> incomingPacketQueue;
    private final ExpoClientPacketReader packetReader;

    public ExpoClientListener(ExpoClientPacketReader packetReader) {
        this.packetReader = packetReader;
        incomingPacketQueue = new ConcurrentLinkedQueue<>();
    }

    /** At the beginning of every client tick, the queued packets are being evaluated. */
    public void evaluatePackets() {
        while(!incomingPacketQueue.isEmpty()) {
            Object packet = incomingPacketQueue.poll();
            packetReader.handlePacketDedicated(packet);
        }
    }

    /** Returns the amount of queued packets. */
    public int getQueuedPacketAmount() {
        return incomingPacketQueue.size();
    }

    @Override
    public void received(Connection connection, Object object) {
        if(object instanceof FrameworkMessage) return;
        incomingPacketQueue.add(object);
    }

}