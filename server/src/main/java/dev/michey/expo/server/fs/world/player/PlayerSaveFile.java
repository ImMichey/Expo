package dev.michey.expo.server.fs.world.player;

import dev.michey.expo.io.ExpoConfig;
import dev.michey.expo.util.ExpoShared;

import java.math.BigDecimal;
import java.util.HashMap;

public class PlayerSaveFile {

    public static HashMap<String, Object> DEFAULT_PROPERTIES;
    private final ExpoConfig expoConfig;

    static {
        HashMap<String, Object> p = new HashMap<>();

        p.put("dimensionName", ExpoShared.DIMENSION_OVERWORLD);
        p.put("entityId", -1);
        p.put("username", "?");
        p.put("posX", 0.0f);
        p.put("posY", 0.0f);
        p.put("inventory", "[]");
        p.put("hunger", 100f);
        p.put("health", 100f);
        p.put("hungerCooldown", 180f);
        p.put("nextHungerTickDown", 4f);
        p.put("nextHungerDamageTick", 4f);

        DEFAULT_PROPERTIES = p;
    }

    public PlayerSaveFile(ExpoConfig expoConfig) {
        this.expoConfig = expoConfig;
    }

    public ExpoConfig getHandler() {
        return expoConfig;
    }

    public String getString(String key) {
        return (String) expoConfig.get(key);
    }

    public float getFloat(String key) {
        Object o = expoConfig.get(key);

        // hacky json object workaround otherwise it deadlocks for some reason
        if(o instanceof BigDecimal bd) {
            return bd.floatValue();
        }

        if(o instanceof Integer i) {
            return i.floatValue();
        }

        return (float) o;
    }

    public int getInt(String key) {
        return (int) expoConfig.get(key);
    }

}
