package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.fs.world.player.PlayerSaveFile;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.Pair;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExpoServerListener implements Listener {

    /** Singleton */
    private static ExpoServerListener INSTANCE;

    /** Packet queue */
    private ConcurrentLinkedQueue<Pair<Connection, Object>> incomingPacketQueue;
    private ExpoServerPacketReader packetReader;

    public ExpoServerListener(ExpoServerPacketReader packetReader) {
        this.packetReader = packetReader;
        incomingPacketQueue = new ConcurrentLinkedQueue<>();
        INSTANCE = this;
    }

    /** At the beginning of every server tick, the queued packets are being evaluated. */
    public void evaluatePackets() {
        while(!incomingPacketQueue.isEmpty()) {
            var packet = incomingPacketQueue.poll();
            packetReader.handlePacket(packet.key, packet.value);
        }
    }

    @Override
    public void connected(Connection connection) {

    }

    @Override
    public void disconnected(Connection connection) {
        PlayerConnection pc = PlayerConnectionHandler.get().getPlayerConnection(connection);
        if(pc == null) return;

        if(pc.player != null) {
            ServerPackets.p10PlayerQuit(pc.player.username, PacketReceiver.all());
            ServerPackets.p4EntityDelete(pc.player.entityId, EntityRemovalReason.VISIBILITY, PacketReceiver.whoCanSee(pc.player));
            pc.player.getDimension().getEntityManager().removeEntitySafely(pc.player);
            pc.player.updateSaveFile();

            var players = ServerWorld.get().getMainDimension().getEntityManager().getAllPlayers();

            for(var p : players) {
                if(p.entityId == pc.player.entityId) continue;
                p.entityVisibilityController.removeTrackedPlayer(pc.player.entityId);
            }

            // async save player file
            // CompletableFuture.runAsync(() -> psf.getHandler().save());
        }

        PlayerConnectionHandler.get().removePlayerConnection(connection);
    }

    @Override
    public void received(Connection connection, Object object) {
        if(object instanceof FrameworkMessage) return;
        incomingPacketQueue.add(new Pair<>(connection, object));
    }

    public static ExpoServerListener get() {
        return INSTANCE;
    }

}