package dev.michey.expo.server.main.logic;

import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.world.ServerWorld;

public class ExpoServerContainer {

    /** Singleton */
    private static ExpoServerContainer INSTANCE;

    /** The game world. */
    private final ServerWorld serverWorld;
    public float globalDelta;

    /** Performance metrics. */
    public long totalTickDuration;
    public long packetTickDuration;
    public long worldTickDuration;
    private long lastUpdate;
    public long longestTickDuration;

    public ExpoServerContainer() {
        serverWorld = new ServerWorld();
        INSTANCE = this;
    }

    /** Base tick method of the game server's logic. */
    public void loop(ExpoServerBase base, float delta) {
        long s = System.nanoTime();

        base.handlePackets();

        long pp = System.nanoTime();

        globalDelta = delta;
        serverWorld.tickWorld();

        long pw = System.nanoTime();

        boolean doUpdate = pw - lastUpdate >= 1_000_000_000;

        long ttd = pw - s;
        long ptd = pp - s;
        long wtd = pw - pp;

        if(ttd > longestTickDuration) {
            longestTickDuration = ttd;
        }

        if(doUpdate) {
            totalTickDuration = ttd;
            packetTickDuration = ptd;
            worldTickDuration = wtd;
            lastUpdate = pw;
            longestTickDuration = 0;

            /*
            ExpoLogger.log("LongestTickDuration: " + longestTickDuration);
            int tickRate = base.isLocalServer() ? ExpoShared.DEFAULT_LOCAL_TICK_RATE : ExpoServerDedicated.get().getTicksPerSecond();
            ExpoLogger.log(String.format(Locale.US, "%.2f", (longestTickDuration / 1_000_000_000d * tickRate)) + "%");
            */
        }
    }

    public void cancelAll() {
        serverWorld.cancelAll();
    }

    public static ExpoServerContainer get() {
        return INSTANCE;
    }

}
