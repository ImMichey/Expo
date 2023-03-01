package dev.michey.expo.server.main.logic;

import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.packet.ExpoServerListener;

public class ExpoServerContainer {

    /** Singleton */
    private static ExpoServerContainer INSTANCE;

    /** The game world. */
    private final ServerWorld serverWorld;
    public float globalDelta;

    public ExpoServerContainer() {
        serverWorld = new ServerWorld();
        INSTANCE = this;
    }

    /** Base tick method of the game server's logic. */
    public void loop(ExpoServerBase base, float delta) {
        if(!base.isLocalServer()) {
            ExpoServerListener.get().evaluatePackets();
        }

        globalDelta = delta;
        serverWorld.tickWorld();
    }

    public void cancelAll() {
        serverWorld.cancelAll();
    }

    public static ExpoServerContainer get() {
        return INSTANCE;
    }

}
