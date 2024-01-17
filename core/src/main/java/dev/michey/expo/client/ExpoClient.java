package dev.michey.expo.client;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import dev.michey.expo.client.serialization.ExpoClientSerialization;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.server.main.packet.ExpoServerRegistry;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.ExpoShared;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExpoClient {

    /** Kryo layer */
    private Client kryoClient;
    private ExpoClientPacketReader packetReader;
    public ConcurrentLinkedQueue<Packet> drainAfterInitQueue;

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
        ExpoServerRegistry.registerPackets(kryoClient.getKryo());
        kryoClient.start();

        try {
            kryoClient.connect(ExpoShared.CLIENT_TIMEOUT_THRESHOLD, address, port, port);
        } catch (IOException e) {
            return e;
        }

        lastBytesUpdate = System.currentTimeMillis();
        packetReader = new ExpoClientPacketReader();
        kryoClient.addListener(new Listener() {

            @Override
            public void received(Connection connection, Object object) {
                if(object instanceof Packet p) {
                    ExpoClientContainer ecc = ExpoClientContainer.get();

                    if(ecc == null) {
                        // This is a workaround for localhost servers as their response time is 0ms and ExpoClientContainer might not be initialized yet.
                        drainAfterInitQueue = new ConcurrentLinkedQueue<>();
                        drainAfterInitQueue.add(p);
                        return;
                    }

                    ecc.getPacketEvaluator().queuePacket(p);
                }
            }

        });

        return null;
    }

    public Client getKryoClient() {
        return kryoClient;
    }

    public ExpoClientPacketReader getPacketReader() {
        return packetReader;
    }

}
