package dev.michey.expo.server.main.logic.world.gen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import dev.michey.expo.server.ServerLauncher;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class WorldGen {

    private final HashMap<String, WorldGenSettings> worldGenMap;

    private static WorldGen INSTANCE;

    public WorldGen() {
        worldGenMap = new HashMap<>();

        String data = null;

        if(Gdx.files == null) {
            try {
                data = new String(ServerLauncher.class.getResourceAsStream("/worldgen.json").readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            data = Gdx.files.internal("worldgen.json").readString();
        }

        JSONArray generatorArray = new JSONObject(data).getJSONArray("generators");

        for(int i = 0; i < generatorArray.length(); i++) {
            JSONObject generator = generatorArray.getJSONObject(i);
            String dimension = generator.getString("dimension");
            WorldGenSettings settings = new WorldGenSettings();

            if(generator.has("noise")) settings.parseNoiseSettings(generator.getJSONObject("noise"));
            if(generator.has("biomePopulators")) settings.parseBiomeSettings(generator.getJSONObject("biomePopulators"));

            worldGenMap.put(dimension, settings);
            log("Added world gen mapping [" + dimension + ", " + settings + "]");
        }

        INSTANCE = this;
    }

    public WorldGenSettings getSettings(String dimension) {
        return worldGenMap.get(dimension);
    }

    public static WorldGen get() {
        return INSTANCE;
    }

}