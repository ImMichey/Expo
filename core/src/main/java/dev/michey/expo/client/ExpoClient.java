package dev.michey.expo.client;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import dev.michey.expo.client.serialization.ExpoClientSerialization;
import dev.michey.expo.server.main.packet.ExpoServerRegistry;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.ExpoShared;

import java.io.IOException;

public class ExpoClient {

    /** Kryo layer */
    private Client kryoClient;
    private ExpoClientPacketReader packetReader;
    private ExpoClientListener packetListener;

    /** Packet tracking */
    public int bytesInTcp;
    public int bytesInUdp;
    public int bytesOutTcp;
    public int bytesOutUdp;
    public long lastBytesUpdate;

    public void sendPacketTcp(Packet packet) {
        kryoClient.sendTCP(packet);
    }

    public void sendPacketUdp(Packet packet) {
        kryoClient.sendUDP(packet);
    }

    public void disconnect() {
        kryoClient.stop();
    }

    public Exception connect(String address, int port) {
        ExpoClientSerialization clientSerialization = new ExpoClientSerialization();
        kryoClient = new Client(ExpoShared.DEFAULT_WRITE_BUFFER_SIZE, ExpoShared.DEFAULT_OBJECT_BUFFER_SIZE, clientSerialization) {

            @Override
            public Kryo getKryo() {
                return clientSerialization.getKryo();
            }

        };
        kryoClient.start();

        try {
            kryoClient.connect(ExpoShared.CLIENT_TIMEOUT_THRESHOLD, address, port, port);
        } catch (IOException e) {
            return e;
        }

        lastBytesUpdate = System.currentTimeMillis();
        ExpoServerRegistry.registerPackets(kryoClient.getKryo());
        packetReader = new ExpoClientPacketReader();
        packetListener = new ExpoClientListener(packetReader);
        kryoClient.addListener(packetListener);

        return null;
    }

    public Client getKryoClient() {
        return kryoClient;
    }

    public ExpoClientListener getPacketListener() {
        return packetListener;
    }

    public ExpoClientPacketReader getPacketReader() {
        return packetReader;
    }

}
