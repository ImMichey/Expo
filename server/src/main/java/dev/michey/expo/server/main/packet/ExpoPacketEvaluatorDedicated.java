package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.Pair;

public class ExpoPacketEvaluatorDedicated extends ExpoPacketEvaluator implements Listener {

    public ExpoPacketEvaluatorDedicated(ExpoServerPacketReader packetReader) {
        super(packetReader);
    }

    @Override
    public void queuePacket(Connection connection, Packet packet) {
        incomingPacketQueue.add(new Pair<>(connection, packet));
    }

    @Override
    public void handlePackets() {
        while(!incomingPacketQueue.isEmpty()) {
            var packet = incomingPacketQueue.poll();
            packetReader.handlePacket(packet.key, packet.value);
        }
    }

    @Override
    public void disconnected(Connection connection) {
        PlayerConnection pc = PlayerConnectionHandler.get().getPlayerConnection(connection);
        if(pc == null) return;

        if(pc.player != null) {
            ServerPackets.p10PlayerQuit(pc.player.username, PacketReceiver.all());
            ServerPackets.p4EntityDelete(pc.player.entityId, EntityRemovalReason.VISIBILITY, PacketReceiver.whoCanSee(pc.player));
            pc.player.getDimension().getEntityManager().removeEntitySafely(pc.player);

            if(pc.player.playerSaveFile != null) {
                pc.player.updateSaveFile();
            }

            var players = ServerWorld.get().getMainDimension().getEntityManager().getAllPlayers();

            for(var p : players) {
                if(p.entityId == pc.player.entityId) continue;
                p.entityVisibilityController.removeTrackedEntity(pc.player.entityId);
            }

            // async save player file
            // CompletableFuture.runAsync(() -> psf.getHandler().save());
        }

        PlayerConnectionHandler.get().removePlayerConnection(connection);
    }

    @Override
    public void received(Connection connection, Object object) {
        if(object instanceof FrameworkMessage) return;
        if(!(object instanceof Packet)) return;
        incomingPacketQueue.add(new Pair<>(connection, (Packet) object));
    }

}
