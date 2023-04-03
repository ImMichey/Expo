package dev.michey.expo.server.util;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
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

    public static PacketReceiver whoCanSee(ServerEntity entity) {
        if(ExpoServerBase.get().isLocalServer()) {
            ServerPlayer player = ServerPlayer.getLocalPlayer();

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

            if(list.size() > 0) {
                return new PacketReceiver(false, null, null, list);
            } else {
                return null;
            }
        }
    }

}
