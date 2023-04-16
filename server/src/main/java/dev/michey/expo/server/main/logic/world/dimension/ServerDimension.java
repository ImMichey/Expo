package dev.michey.expo.server.main.logic.world.dimension;

import com.badlogic.gdx.math.MathUtils;
import com.dongbat.jbump.World;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.ExpoServerContainer;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.world.chunk.EntityMasterVisibilityController;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunkGrid;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.ExpoTime;
import dev.michey.expo.util.Pair;
import dev.michey.expo.weather.Weather;

import java.util.concurrent.Callable;

public abstract class ServerDimension {

    /** Dimension properties */
    protected final String dimensionName;
    protected final boolean mainDimension;
    protected float dimensionSpawnX = MathUtils.random(128.0f, 384.0f);
    protected float dimensionSpawnY = MathUtils.random(128.0f, 384.0f);

    /** Dimension data */
    public float dimensionTime = ExpoTime.worldDurationHours(8);
    public Weather dimensionWeather = Weather.SUN;
    public float dimensionWeatherDuration = Weather.SUN.generateWeatherDuration();
    public float dimensionWeatherStrength = Weather.SUN.generateWeatherStrength();

    /** Entity handler */
    private final ServerDimensionEntityManager entityManager;
    private final EntityMasterVisibilityController visibilityController;

    /** Chunk handler */
    private final ServerChunkGrid chunkHandler;

    /** Physics handler */
    private final World<ServerEntity> physicsWorld;

    public ServerDimension(String dimensionName, boolean mainDimension) {
        this.dimensionName = dimensionName;
        this.mainDimension = mainDimension;
        entityManager = new ServerDimensionEntityManager();
        chunkHandler = new ServerChunkGrid(this);
        visibilityController = new EntityMasterVisibilityController(this);
        physicsWorld = new World<>(16f);
    }

    private void tickDimension() {
        dimensionTime += ExpoServerContainer.get().globalDelta;
        if(dimensionTime >= ExpoTime.WORLD_CYCLE_DURATION) dimensionTime %= ExpoTime.WORLD_CYCLE_DURATION;

        dimensionWeatherDuration -= ExpoServerContainer.get().globalDelta;

        if(dimensionWeatherDuration < 0) {
            generateWeather();
        }

        chunkHandler.tickChunks();
        entityManager.tickEntities(ExpoServerContainer.get().globalDelta);
        visibilityController.tick();
    }

    private void generateWeather() {
        if(dimensionWeather == Weather.SUN) {
            dimensionWeather = Weather.RAIN;
            dimensionWeatherDuration = Weather.RAIN.generateWeatherDuration();
            dimensionWeatherStrength = Weather.RAIN.generateWeatherStrength();
        } else {
            dimensionWeather = Weather.SUN;
            dimensionWeatherDuration = Weather.SUN.generateWeatherDuration();
            dimensionWeatherStrength = Weather.SUN.generateWeatherStrength();
        }

        // Update.
        ServerPackets.p14WorldUpdate(dimensionTime, dimensionWeather.WEATHER_ID, dimensionWeatherStrength, PacketReceiver.dimension(this));
    }

    /** This method gets called when the dimension is ready to tick. */
    public void onReady() {

    }

    public void setNoiseSeed(int seed) {
        chunkHandler.getTerrainNoiseHeight().setSeed(seed);
        chunkHandler.getTerrainNoiseTemperature().setSeed(seed + 1);
        chunkHandler.getTerrainNoiseMoisture().setSeed(seed + 2);
        chunkHandler.getRiverNoise().setSeed(seed);
    }

    private boolean isBiome(int tx, int ty, BiomeType... types) {
        for(BiomeType type : types) {
            if(type == chunkHandler.getBiome(tx, ty)) return true;
        }

        return false;
    }

    private boolean isBiome(float x, float y, BiomeType... types) {
        int tileX = ExpoShared.posToTile(x);
        int tileY = ExpoShared.posToTile(y);

        for(BiomeType type : types) {
            if(type == chunkHandler.getBiome(tileX, tileY)) return true;
        }

        return false;
    }

    public Pair<Boolean, float[]> findBiome(int maxAttempts, BiomeType... types) {
        // First check spawn location.
        if(isBiome(dimensionSpawnX, dimensionSpawnY, types)) {
            return new Pair<>(true, new float[] {dimensionSpawnX, dimensionSpawnY});
        }

        Pair<Boolean, float[]> pair = new Pair<>(false, null);

        boolean doAttemptCheck = maxAttempts != -1;
        int attempts = 0;

        int checkTile = 0;
        int increase = 3;

        while(true) {
            int cx = -checkTile;
            int cy = -checkTile;

            if(isBiome(cx, cy, types)) {
                pair.key = true;
                pair.value = new float[] {ExpoShared.tileToPos(cx), ExpoShared.tileToPos(cy)};
                break;
            }

            cx = -checkTile;
            cy = checkTile;

            if(isBiome(cx, cy, types)) {
                pair.key = true;
                pair.value = new float[] {ExpoShared.tileToPos(cx), ExpoShared.tileToPos(cy)};
                break;
            }

            cx = checkTile;
            cy = checkTile;

            if(isBiome(cx, cy, types)) {
                pair.key = true;
                pair.value = new float[] {ExpoShared.tileToPos(cx), ExpoShared.tileToPos(cy)};
                break;
            }

            cx = checkTile;
            cy = -checkTile;

            if(isBiome(cx, cy, types)) {
                pair.key = true;
                pair.value = new float[] {ExpoShared.tileToPos(cx), ExpoShared.tileToPos(cy)};
                break;
            }

            attempts++;

            if(doAttemptCheck && (attempts == maxAttempts)) {
                // Didn't find.
                break;
            }

            checkTile += increase;
        }

        return pair;
    }

    /** Wrapper method to enable multi-threading. Don't touch. */
    public Callable<Void> tick() {
        tickDimension();
        return () -> null;
    }

    public ServerDimensionEntityManager getEntityManager() {
        return entityManager;
    }

    public ServerChunkGrid getChunkHandler() {
        return chunkHandler;
    }

    public World<ServerEntity> getPhysicsWorld() {
        return physicsWorld;
    }

    public EntityMasterVisibilityController getVisibilityController() {
        return visibilityController;
    }

    public boolean isMainDimension() {
        return mainDimension;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    public float getDimensionSpawnX() {
        return dimensionSpawnX;
    }

    public float getDimensionSpawnY() {
        return dimensionSpawnY;
    }

    public void setDimensionSpawnX(float dimensionSpawnX) {
        this.dimensionSpawnX = dimensionSpawnX;
    }

    public void setDimensionSpawnY(float dimensionSpawnY) {
        this.dimensionSpawnY = dimensionSpawnY;
    }

}
