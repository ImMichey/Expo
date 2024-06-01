package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class EntityPopulator {

    public ServerEntityType type;
    public float spawnChance;
    public boolean asStaticEntity;
    public double poissonDiskSamplerDistance;
    public int priority;
    public EntityBoundsEntry dimensionBounds = null;
    public SpreadData[] spreadData;

    public EntityPopulator(JSONObject singlePopulatorObject) {
        type = ServerEntityType.valueOf(singlePopulatorObject.getString("type"));
        spawnChance = singlePopulatorObject.getFloat("chance");
        asStaticEntity = singlePopulatorObject.getBoolean("static");
        poissonDiskSamplerDistance = singlePopulatorObject.getDouble("pds");
        if(singlePopulatorObject.has("priority")) priority = singlePopulatorObject.getInt("priority");

        // optional
        if(singlePopulatorObject.has("spreadData")) {
            var object = singlePopulatorObject.get("spreadData");

            if(object instanceof JSONArray ja) {
                SpreadData[] all = new SpreadData[ja.length()];

                for(int i = 0; i < ja.length(); i++) {
                    JSONObject iterator = ja.getJSONObject(i);
                    all[i] = new SpreadData(iterator);
                }

                spreadData = all;
            } else {
                JSONObject entry = singlePopulatorObject.getJSONObject("spreadData");
                spreadData = new SpreadData[] {new SpreadData(entry)};
            }
        }
    }

    public SpreadData pickSpreadData(GenerationRandom gr) {
        return spreadData[gr.random(0, spreadData.length - 1)];
    }

    @Override
    public String toString() {
        return "EntityPopulator{" +
                "type=" + type +
                ", spawnChance=" + spawnChance +
                ", asStaticEntity=" + asStaticEntity +
                ", poissonDiskSamplerDistance=" + poissonDiskSamplerDistance +
                ", priority=" + priority +
                ", spreadData=" + Arrays.toString(spreadData) +
                '}';
    }

}
