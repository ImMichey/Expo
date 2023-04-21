package dev.michey.expo.server.main.arch;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.command.CommandResolver;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.command.*;
import dev.michey.expo.server.config.ExpoServerConfiguration;
import dev.michey.expo.server.fs.world.WorldSaveFile;
import dev.michey.expo.server.fs.world.player.PlayerSaveFile;
import dev.michey.expo.server.main.logic.ExpoServerContainer;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.main.packet.ExpoServerPacketReader;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.ExpoShared;

import java.io.File;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.SPAWN_AREA_CHUNK_RANGE;

public abstract class ExpoServerBase {

    /** Singleton */
    private static ExpoServerBase INSTANCE;

    /** Server properties */
    private final boolean localServer;

    /** Lifecycle */
    protected final ExpoServerPacketReader packetReader;
    protected final ExpoServerContainer serverContainer;
    protected boolean running = true;
    protected int tps;

    /** I/O handlers */
    private final WorldSaveFile worldSaveFile;

    /** Command resolver */
    private final ServerCommandResolver commandResolver;

    public ExpoServerBase(boolean localServer, String worldName) {
        this.localServer = localServer;
        packetReader = new ExpoServerPacketReader();
        serverContainer = new ExpoServerContainer();
        worldSaveFile = new WorldSaveFile(worldName);
        if(!worldSaveFile.load()) {
            log("Failed to load WorldSaveFile, aborting application.");
            System.exit(0);
        }
        commandResolver = new ServerCommandResolver();
        commandResolver.addCommand(new ServerCommandTime());
        commandResolver.addCommand(new ServerCommandChunkDump());
        commandResolver.addCommand(new ServerCommandEntityDump());
        commandResolver.addCommand(new ServerCommandHelp());
        commandResolver.addCommand(new ServerCommandItems());
        commandResolver.addCommand(new ServerCommandStop());
        commandResolver.addCommand(new ServerCommandWhitelist());
        commandResolver.addCommand(new ServerCommandTiles());
        commandResolver.addCommand(new ServerCommandRain());
        commandResolver.addCommand(new ServerCommandSun());
        commandResolver.addCommand(new ServerCommandHunger());
        commandResolver.addCommand(new ServerCommandDamage());
        INSTANCE = this;
        applyFileProperties();
    }

    private void applyFileProperties() {
        ServerWorld world = ServerWorld.get();

        // update entity id + world seed
        world.setCurrentEntityId(worldSaveFile.getCurrentEntityId());
        world.setWorldSeed(worldSaveFile.getWorldSeed());

        // update dimensions
        for(ServerDimension dimension : world.getDimensions()) {
            if(!localServer) {
                dimension.getChunkHandler().setUnloadAfterMillis(ExpoServerConfiguration.get().getUnloadChunksAfter());
                dimension.getChunkHandler().setSaveAfterMillis(ExpoServerConfiguration.get().getSaveChunksAfter());
            }

            File[] files = new File(worldSaveFile.getPathDimensionSpecificFolder(dimension.getDimensionName())).listFiles(); // <execDir>/saves/<world>/dimensions/<dimension>/[LIST]
            dimension.getChunkHandler().initializeKnownChunks(files);

            if(dimension.isMainDimension()) {
                // find proper spawn
                if(worldSaveFile.getCreationTimestamp() == worldSaveFile.getLastSaveTimestamp()) {
                    log("Finding spawn area for '" + dimension.getDimensionName() + "'...");
                    var found = world.getMainDimension().findBiome(-1, BiomeType.PLAINS);

                    if(found.key) {
                        log("Found spawn area at " + found.value[0] + ", " + found.value[1] + " - using as dimension spawn coordinates.");
                        dimension.setDimensionSpawnX(found.value[0] + 3);
                        dimension.setDimensionSpawnY(found.value[1] + 8);
                    } else {
                        log("Failed to find spawn area, re-using dimension spawn coordinates.");
                    }
                }

                PlayerSaveFile.DEFAULT_PROPERTIES.put("posX", dimension.getDimensionSpawnX());
                PlayerSaveFile.DEFAULT_PROPERTIES.put("posY", dimension.getDimensionSpawnY());
                PlayerSaveFile.DEFAULT_PROPERTIES.put("dimensionName", dimension.getDimensionName());

                int spawnChunkX = ExpoShared.posToChunk(dimension.getDimensionSpawnX());
                int spawnChunkY = ExpoShared.posToChunk(dimension.getDimensionSpawnY());
                int startChunkX = spawnChunkX - (SPAWN_AREA_CHUNK_RANGE - 1) / 2;
                int startChunkY = spawnChunkY - (SPAWN_AREA_CHUNK_RANGE - 1) / 2;

                dimension.getChunkHandler().initializeSpawnChunks(startChunkX, startChunkY);
                dimension.onReady();
            }
        }
    }

    /** Called when the game server should be started, returns whether it was successful or not. */
    public abstract boolean startServer();

    /** Called when the game server should be stopped. */
    public abstract void stopServer();

    /** Implemented by its extensions. */
    public abstract void broadcastPacketTCP(Packet packet);
    public abstract void broadcastPacketUDP(Packet packet);
    public abstract void broadcastPacketTCPExcept(Packet packet, Connection connection);
    public abstract void broadcastPacketUDPExcept(Packet packet, Connection connection);

    public boolean isLocalServer() {
        return localServer;
    }

    public int getTicksPerSecond() {
        return tps;
    }

    public ServerCommandResolver getCommandResolver() {
        return commandResolver;
    }

    public WorldSaveFile getWorldSaveHandler() {
        return worldSaveFile;
    }

    public ExpoServerPacketReader getPacketReader() {
        return packetReader;
    }

    public void resetInstance() {
        INSTANCE = null;
    }

    public static ExpoServerBase get() {
        return INSTANCE;
    }

}
