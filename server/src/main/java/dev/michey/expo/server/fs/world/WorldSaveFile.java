package dev.michey.expo.server.fs.world;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.io.ExpoFile;
import dev.michey.expo.io.ExpoFileCreator;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.fs.world.player.PlayerSaveHandler;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.weather.Weather;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static dev.michey.expo.log.ExpoLogger.log;

public class WorldSaveFile {

    /** File names */
    private static final String SAVE_FOLDER = "saves";
    private final String PLAYER_FOLDER = "players";
    private final String WORLD_BASE_FILE = "world.json";
    private final String DIMENSIONS_FOLDER = "dimensions";

    /** world.json properties */
    private final String worldName;
    private String worldSpawnDimension = ExpoShared.DIMENSION_OVERWORLD;
    private int currentEntityId = 0;
    private int worldSeed = MathUtils.random.nextInt();
    private long creationTimestamp;
    private long lastSaveTimestamp;

    /** Player save handling */
    private PlayerSaveHandler playerSaveHandler;

    public WorldSaveFile(String worldName) {
        this.worldName = worldName;
        long now = System.currentTimeMillis();
        creationTimestamp = now;
        lastSaveTimestamp = now;
    }

    public int getCurrentEntityId() {
        return currentEntityId;
    }

    public int getWorldSeed() {
        return worldSeed;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public long getLastSaveTimestamp() {
        return lastSaveTimestamp;
    }

    public boolean load() {
        log("Loading world save file for " + worldName);

        var result = ExpoFileCreator.createFileStructure("",
                new ExpoFile(ExpoFile.FileType.FOLDER, getPathSaveFolder()), // saves
                new ExpoFile(ExpoFile.FileType.FOLDER, getPathWorldFolder()), // saves/<world>
                new ExpoFile(ExpoFile.FileType.CONFIG, getPathWorldFile(), worldConfigAsJson()), // saves/<world>/world.json
                new ExpoFile(ExpoFile.FileType.FOLDER, getPathPlayersFolder()), // saves/<world>/players
                new ExpoFile(ExpoFile.FileType.FOLDER, getPathDimensionsFolder()) // saves/<world>/dimensions
        );

        if(!result.key) {
            log("Failed to create WorldSaveFile structure");
            System.exit(0);
        } else {
            if(result.value[2] != null) {
                applyFromFile((JSONObject) result.value[2]);
            }
        }

        // Create dimensions structure
        for(ServerDimension dimension : ServerWorld.get().getDimensions()) {
            result = ExpoFileCreator.createFileStructure("", new ExpoFile(ExpoFile.FileType.FOLDER, getPathDimensionSpecificFolder(dimension.getDimensionName())));

            if(!result.key) {
                log("Failed to create WorldSaveFile structure (dimensions)");
                System.exit(0);
            }
        }

        // Create player save handler
        playerSaveHandler = new PlayerSaveHandler(getPathPlayersFolder());

        return true;
    }

    // <execDir>/saves
    public static String getPathSaveFolder() {
        return ExpoLogger.getLocalPath() + File.separator + SAVE_FOLDER;
    }

    // <execDir>/saves/<world>
    public String getPathWorldFolder() {
        return getPathSaveFolder() + File.separator + worldName;
    }

    // <execDir>/saves/<world>/world.json
    public String getPathWorldFile() {
        return getPathWorldFolder() + File.separator + WORLD_BASE_FILE;
    }

    // <execDir>/saves/<world>/players
    public String getPathPlayersFolder() {
        return getPathWorldFolder() + File.separator + PLAYER_FOLDER;
    }

    // <execDir>/saves/<world>/dimensions
    public String getPathDimensionsFolder() {
        return getPathWorldFolder() + File.separator + DIMENSIONS_FOLDER;
    }

    // <execDir>/saves/<world>/dimensions/<dimension>
    public String getPathDimensionSpecificFolder(String dimensionName) {
        return getPathDimensionsFolder() + File.separator + dimensionName;
    }

    public void updateAndSave(ServerWorld world) {
        log("Saving world data for " + worldName);
        currentEntityId = world.getCurrentEntityId();
        worldSeed = world.getWorldSeed();
        lastSaveTimestamp = System.currentTimeMillis();

        ServerDimension mainDimension = world.getMainDimension();
        worldSpawnDimension = mainDimension.getDimensionName();

        File f = new File(getPathWorldFile());

        try {
            Files.writeString(f.toPath(), worldConfigAsJson().toString(4), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(ServerDimension dimension : world.getDimensions()) {
            dimension.getChunkHandler().saveAllChunks();
        }
    }

    private void applyFromFile(JSONObject json) {
        worldSpawnDimension = json.getString("worldSpawnDimension");
        currentEntityId = json.getInt("currentEntityId");
        worldSeed = json.getInt("worldSeed");
        creationTimestamp = json.getLong("creationTimestamp");

        JSONArray dimensionArray = json.getJSONArray("dimensions");

        for(int i = 0; i < dimensionArray.length(); i++) {
            JSONObject dimensionObject = dimensionArray.getJSONObject(i);
            ServerDimension dimension = ServerWorld.get().getDimension(dimensionObject.getString("name"));
            dimension.setDimensionSpawnX(dimensionObject.getFloat("spawnX"));
            dimension.setDimensionSpawnY(dimensionObject.getFloat("spawnY"));
            dimension.dimensionTime = dimensionObject.getFloat("time");
            dimension.dimensionWeather = Weather.idToWeather(dimensionObject.getInt("weather"));
            dimension.dimensionWeatherDuration = dimensionObject.getFloat("weatherDuration");
        }
    }

    private JSONObject worldConfigAsJson() {
        JSONObject full = new JSONObject();
        full.put("currentEntityId", currentEntityId);
        full.put("worldSpawnDimension", worldSpawnDimension);
        full.put("worldSeed", worldSeed);
        full.put("creationTimestamp", creationTimestamp);
        full.put("lastSaveTimestamp", lastSaveTimestamp);

        JSONArray dimensions = new JSONArray();
        for(ServerDimension dimension : ServerWorld.get().getDimensions()) {
            JSONObject dimensionObject = new JSONObject();
            dimensionObject.put("name", dimension.getDimensionName());
            dimensionObject.put("spawnX", dimension.getDimensionSpawnX());
            dimensionObject.put("spawnY", dimension.getDimensionSpawnY());
            dimensionObject.put("time", dimension.dimensionTime);
            dimensionObject.put("weather", dimension.dimensionWeather.WEATHER_ID);
            dimensionObject.put("weatherDuration", dimension.dimensionWeatherDuration);
            dimensions.put(dimensionObject);
        }
        full.put("dimensions", dimensions);

        return full;
    }

    public PlayerSaveHandler getPlayerSaveHandler() {
        return playerSaveHandler;
    }

}
