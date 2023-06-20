package dev.michey.expo.server.util;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.ServerLauncher;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonConverter {

    public static JSONObject fileToJson(String path) {
        String data = null;

        if(Gdx.files == null) {
            try {
                data = new String(ServerLauncher.class.getResourceAsStream("/" + path).readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            data = Gdx.files.internal(path).readString();
        }

        return new JSONObject(data);
    }

    public static String[] pullStrings(JSONArray array) {
        String[] strings = new String[array.length()];
        for(int i = 0; i < array.length(); i++) strings[i] = array.getString(i);
        return strings;
    }

    public static int[] pullInts(JSONArray array) {
        int[] ints = new int[array.length()];
        for(int i = 0; i < array.length(); i++) ints[i] = array.getInt(i);
        return ints;
    }

    public static float[] pullFloats(JSONArray array) {
        float[] floats = new float[array.length()];
        for(int i = 0; i < array.length(); i++) floats[i] = array.getFloat(i);
        return floats;
    }

    public static BiomeType[] pullBiomes(JSONArray array) {
        BiomeType[] biomes = new BiomeType[array.length()];
        for(int i = 0; i < array.length(); i++) biomes[i] = BiomeType.valueOf(array.getString(i));
        return biomes;
    }

}
