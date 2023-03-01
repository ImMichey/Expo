package dev.michey.expo.server.connection;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerConnectionHandler {

    /** Singleton */
    private static PlayerConnectionHandler INSTANCE;

    /** Connections */
    public HashMap<Connection, PlayerConnection> connectionList;

    /** Ping task */
    private final Timer pingUpdateScheduler;
    private final Object connectionLock = new Object();

    public PlayerConnectionHandler() {
        connectionList = new HashMap<>();
        pingUpdateScheduler = new Timer();
        INSTANCE = this;
    }

    public void startPingUpdateScheduler() {
        pingUpdateScheduler.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                synchronized(connectionLock) {
                    ServerPackets.p15PingList(PacketReceiver.all());
                }
            }

        }, 500, 500);
    }

    public void stopPingUpdateScheduler() {
        pingUpdateScheduler.cancel();

        for(PlayerConnection con : connectionList.values()) {
            con.stopPingerTask();
        }
    }

    public void broadcastTcpExcept(Packet packet, Connection connection) {
        for(Connection c : connectionList.keySet()) if(c.getID() != connection.getID()) c.sendTCP(packet);
    }

    public void broadcastUdpExcept(Packet packet, Connection connection) {
        for(Connection c : connectionList.keySet()) if(c.getID() != connection.getID()) c.sendUDP(packet);
    }

    public PlayerConnection getPlayerConnection(Connection connection) {
        synchronized(connectionLock) {
            return connectionList.get(connection);
        }
    }

    public PlayerConnection addPlayerConnection(Connection connection) {
        synchronized(connectionLock) {
            PlayerConnection pc = new PlayerConnection(connection);
            connectionList.put(connection, pc);
            pc.startPingerTask(pingUpdateScheduler);
            return pc;
        }
    }

    public void removePlayerConnection(Connection connection) {
        synchronized(connectionLock) {
            connectionList.get(connection).stopPingerTask();
            connectionList.remove(connection);
        }
    }

    public Collection<PlayerConnection> connections() {
        return connectionList.values();
    }

    public static PlayerConnectionHandler get() {
        return INSTANCE;
    }

}
