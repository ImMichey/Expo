package dev.michey.expo.client;

import com.esotericsoftware.kryonet.Client;
import dev.michey.expo.server.main.packet.ExpoServerRegistry;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.ExpoShared;

import java.io.IOException;

public class ExpoClient {

    /** Kryo layer */
    private Client kryoClient;
    private ExpoClientPacketReader packetReader;
    private ExpoClientListener packetListener;

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
        kryoClient = new Client();
        kryoClient.start();

        try {
            kryoClient.connect(ExpoShared.CLIENT_TIMEOUT_THRESHOLD, address, port, port);
        } catch (IOException e) {
            return e;
        }

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
