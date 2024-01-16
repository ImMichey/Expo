package dev.michey.expo.server.util;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;

import java.util.LinkedList;
import java.util.List;

public class PacketReceiver {

    public boolean all;
    public Connection receiverSingle;
    public Connection receiverAllExcept;
    public List<Connection> receiverList;

    public PacketReceiver(boolean all, Connection receiverSingle, Connection receiverAllExcept, List<Connection> receiverList) {
        this.all = all;
        this.receiverSingle = receiverSingle;
        this.receiverAllExcept = receiverAllExcept;
        this.receiverList = receiverList;
    }

    public static PacketReceiver dimension(ServerDimension dimension) {
        if(ExpoServerBase.get().isLocalServer()) {
            if(ServerPlayer.getLocalPlayer().getDimension().equals(dimension)) {
                return local();
            }
            return null;
        }

        List<Connection> list = new LinkedList<>();
        List<ServerPlayer> playerList = dimension.getEntityManager().getAllPlayers();

        for(ServerPlayer player : playerList) {
            list.add(player.playerConnection.getKryoConnection());
        }

        return new PacketReceiver(false, null, null, list);
    }

    public static PacketReceiver all() {
        return new PacketReceiver(true, null, null, null);
    }

    public static PacketReceiver allExcept(Connection except) {
        return new PacketReceiver(false, null, except, null);
    }

    public static PacketReceiver local() {
        return new PacketReceiver(true, null, null, null);
    }

    public static PacketReceiver player(ServerPlayer player) {
        if(player.localServerPlayer) return all();
        return new PacketReceiver(false, player.playerConnection.getKryoConnection(), null, null);
    }

    public static PacketReceiver connection(Connection receiver) {
        return new PacketReceiver(false, receiver, null, null);
    }

    public static PacketReceiver whoCanSee(ServerTile tile) {
        return whoCanSee(tile.chunk.getDimension(), tile.chunk.chunkX, tile.chunk.chunkY);
    }

    public static PacketReceiver whoCanSee(ServerDimension dimension, int chunkX, int chunkY) {
        if(ExpoServerBase.get().isLocalServer()) {
            return local();
        } else {
            List<Connection> list = new LinkedList<>();
            List<ServerEntity> playerList = dimension.getEntityManager().getEntitiesOf(ServerEntityType.PLAYER);

            nextPlayer: for(ServerEntity se : playerList) {
                ServerPlayer player = (ServerPlayer) se;

                if(player.currentlyVisibleChunks != null) {
                    for(ServerChunk chunk : player.currentlyVisibleChunks) {
                        if(chunk == null) continue;
                        if(chunkX == chunk.chunkX && chunkY == chunk.chunkY) {
                            list.add(player.playerConnection.getKryoConnection());
                            continue nextPlayer;
                        }
                    }
                }
            }

            if(!list.isEmpty()) {
                return new PacketReceiver(false, null, null, list);
            } else {
                return null;
            }
        }
    }

    public static PacketReceiver whoCanSee(ServerEntity entity) {
        if(ExpoServerBase.get().isLocalServer()) {
            ServerPlayer player = ServerPlayer.getLocalPlayer();
            if(player == null) return null;

            if(entity.entityId == player.entityId || player.entityVisibilityController.isAlreadyVisible(entity.entityId)) {
                return local();
            } else {
                return null;
            }
        } else {
            List<Connection> list = new LinkedList<>();
            List<ServerEntity> playerList = entity.getDimension().getEntityManager().getEntitiesOf(ServerEntityType.PLAYER);

            for(ServerEntity se : playerList) {
                ServerPlayer player = (ServerPlayer) se;

                if(entity.entityId == player.entityId || player.entityVisibilityController.isAlreadyVisible(entity.entityId)) {
                    list.add(player.playerConnection.getKryoConnection());
                }
            }

            if(!list.isEmpty()) {
                return new PacketReceiver(false, null, null, list);
            } else {
                return null;
            }
        }
    }

    public static PacketReceiver whoCanSeeExcept(ServerEntity entity, ServerPlayer except) {
        if(ExpoServerBase.get().isLocalServer()) {
            return null;
        } else {
            List<Connection> list = new LinkedList<>();
            List<ServerEntity> playerList = entity.getDimension().getEntityManager().getEntitiesOf(ServerEntityType.PLAYER);

            for(ServerEntity se : playerList) {
                ServerPlayer player = (ServerPlayer) se;
                if(player.entityId == except.entityId) continue;

                if(entity.entityId == player.entityId || player.entityVisibilityController.isAlreadyVisible(entity.entityId)) {
                    list.add(player.playerConnection.getKryoConnection());
                }
            }

            if(!list.isEmpty()) {
                return new PacketReceiver(false, null, null, list);
            } else {
                return null;
            }
        }
    }

}
