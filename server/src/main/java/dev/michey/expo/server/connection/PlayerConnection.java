package dev.michey.expo.server.connection;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

import java.util.Timer;
import java.util.TimerTask;

public class PlayerConnection {

    /** Base info */
    private Connection kryoConnection;
    private TimerTask pingerTask;

    /** Game world reference */
    public ServerPlayer player;

    /** Username, Steam data */
    public String username;
    public long steamId;

    public PlayerConnection(Connection kryoConnection, String username, long steamId) {
        this.kryoConnection = kryoConnection;
        this.username = username;
        this.steamId = steamId;
    }

    public void connectTo(ServerPlayer player) {
        this.player = player;
        player.playerConnection = this;
    }

    public boolean isSteamConnection() {
        return steamId != -1;
    }

    public void startPingerTask(Timer timer) {
        pingerTask = new TimerTask() {

            @Override
            public void run() {
                kryoConnection.updateReturnTripTime();
            }

        };

        timer.scheduleAtFixedRate(pingerTask, 100, 100);
    }

    public void stopPingerTask() {
        pingerTask.cancel();
    }

    public boolean isConnection(Connection connection) {
        return kryoConnection.getID() == connection.getID();
    }

    public Connection getKryoConnection() {
        return kryoConnection;
    }

}
