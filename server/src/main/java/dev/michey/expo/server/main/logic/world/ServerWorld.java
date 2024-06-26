package dev.michey.expo.server.main.logic.world;

import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimensionCave;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimensionOverworld;
import dev.michey.expo.util.ExpoShared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.*;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerWorld {

    /** Singleton */
    private static ServerWorld INSTANCE;

    /** All world dimensions */
    private final HashMap<String, ServerDimension> serverDimensionMap;
    private ServerDimension mainDimension;

    /** Multithreaded ticking for every dimension */
    private final ExecutorService executorService;
    private final Collection<Callable<Void>> dimensionTickCollection;
    public final ExecutorService mtServiceTick;
    public final ExecutorService mtVirtualService;

    /** Entity id tracking */
    private int currentEntityId;

    /** World seed */
    private int worldSeed;

    public ServerWorld() {
        serverDimensionMap = new HashMap<>();
        executorService = Executors.newFixedThreadPool(2);
        dimensionTickCollection = new ArrayList<>();

        ThreadFactory tf = new ThreadFactory() {
            private int nr = -1;
            @Override
            public Thread newThread(Runnable r) {
                nr++;
                return new Thread(r, "expo-mtServiceTick-" + nr);
            }
        };
        mtServiceTick = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), tf);
        mtVirtualService = Executors.newVirtualThreadPerTaskExecutor();

        addDimension(ExpoShared.DIMENSION_OVERWORLD, new ServerDimensionOverworld(ExpoShared.DIMENSION_OVERWORLD, true));
        addDimension(ExpoShared.DIMENSION_CAVE, new ServerDimensionCave(ExpoShared.DIMENSION_CAVE, false));

        INSTANCE = this;
    }

    /** Ticking the game world. */
    public void tickWorld() {
        if(!executorService.isShutdown()) {
            dimensionTickCollection.clear();

            for(ServerDimension dimension : serverDimensionMap.values()) {
                dimensionTickCollection.add(dimension.tick());
            }

            try {
                executorService.invokeAll(dimensionTickCollection);
            } catch (InterruptedException e) {
                log("ServerWorld ExecutorService crashed");
                ExpoShared.DUMP_THREADS = true;
                e.printStackTrace();
                executorService.shutdown();
            } catch (RejectedExecutionException ignored) { }
        }
    }

    /** Creates an instance of a ServerPlayer entity. */
    public ServerPlayer createPlayerEntity(PlayerConnection pc, String username) {
        ServerPlayer player = new ServerPlayer();
        player.forceChunkChange = true;
        player.username = username;
        player.localServerPlayer = pc == null;
        if(player.localServerPlayer) {
            ServerPlayer.setLocalPlayer(player);
        } else {
            pc.connectTo(player);
        }
        return player;
    }

    /** Adds an entity to the desired dimension and gives it a unique entity id. */
    public int registerServerEntity(String dimensionName, ServerEntity entity) {
        entity.entityId = generateEntityId();
        entity.entityDimension = dimensionName;
        serverDimensionMap.get(dimensionName).getEntityManager().addEntitySafely(entity);
        return entity.entityId;
    }

    /** Adds an entity to the desired dimension and uses a pre-given unique entity id. */
    public void registerServerEntity(String dimensionName, ServerEntity entity, int entityId) {
        entity.entityId = entityId;
        entity.entityDimension = dimensionName;
        serverDimensionMap.get(dimensionName).getEntityManager().addEntitySafely(entity);
    }

    /** Deletes an entity from the desired dimension. */
    public void deleteServerEntity(String dimensionName, int entityId) {
        serverDimensionMap.get(dimensionName).getEntityManager().removeEntitySafely(entityId);
    }

    /** Deletes an entity from the desired dimension. */
    public void deleteServerEntity(String dimensionName, ServerEntity entity) {
        serverDimensionMap.get(dimensionName).getEntityManager().removeEntitySafely(entity);
    }

    public void setCurrentEntityId(int currentEntityId) {
        this.currentEntityId = currentEntityId;
    }

    public int getCurrentEntityId() {
        return currentEntityId;
    }

    public synchronized int generateEntityId() {
        int nextId = currentEntityId;
        currentEntityId++;
        return nextId;
    }

    public int getWorldSeed() {
        return worldSeed;
    }

    public void setWorldSeed(int worldSeed) {
        this.worldSeed = worldSeed;

        for(ServerDimension dimension : getDimensions()) {
            dimension.setNoiseSeed(worldSeed);
        }
    }

    public void cancelAll() {
        mtServiceTick.shutdown();
        mtVirtualService.shutdown();
        executorService.shutdown();
    }

    public void addDimension(String name, ServerDimension dimension) {
        log("ServerDimension entry " + name + " (isMainDimension: " + dimension.isMainDimension() + ")");
        serverDimensionMap.put(name, dimension);
        if(dimension.isMainDimension()) mainDimension = dimension;
    }

    public ServerDimension getMainDimension() {
        return mainDimension;
    }

    public Collection<ServerDimension> getDimensions() {
        return serverDimensionMap.values();
    }

    public static ServerWorld get() {
        return INSTANCE;
    }

    public ServerDimension getDimension(String name) {
        return serverDimensionMap.get(name);
    }

}
