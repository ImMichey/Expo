package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

import java.util.Arrays;

public class EntityPopulator {

    public ServerEntityType type;
    public float spawnChance;
    public boolean asStaticEntity;
    public double poissonDiskSamplerDistance;
    public int priority;
    public EntityBoundsEntry dimensionBounds = null;
    public SpreadData spreadData;

    public EntityPopulator(JSONObject singlePopulatorObject) {
        type = ServerEntityType.valueOf(singlePopulatorObject.getString("type"));
        spawnChance = singlePopulatorObject.getFloat("chance");
        asStaticEntity = singlePopulatorObject.getBoolean("static");
        poissonDiskSamplerDistance = singlePopulatorObject.getDouble("pds");
        if(singlePopulatorObject.has("priority")) priority = singlePopulatorObject.getInt("priority");

        // optional
        if(singlePopulatorObject.has("spreadData")) {
            JSONObject entry = singlePopulatorObject.getJSONObject("spreadData");
            spreadData = new SpreadData(entry);
        }
    }

    @Override
    public String toString() {
        return "EntityPopulator{" +
                "type=" + type +
                ", spawnChance=" + spawnChance +
                ", asStaticEntity=" + asStaticEntity +
                ", poissonDiskSamplerDistance=" + poissonDiskSamplerDistance +
                ", priority=" + priority +
                ", spreadData=" + spreadData +
                '}';
    }

}
