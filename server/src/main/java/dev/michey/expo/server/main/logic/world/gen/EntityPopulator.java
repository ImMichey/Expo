package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

import java.util.Arrays;

public class EntityPopulator {

    public ServerEntityType type;
    public float spawnChance;
    public boolean asStaticEntity;
    public double poissonDiskSamplerDistance;
    public int[] spreadBetweenAmount;
    public float spreadChance;
    public float[] spreadBetweenDistance;
    public ServerEntityType[] spreadBetweenEntities;
    public boolean spreadAsStaticEntity;
    public float[] spreadOffsets;
    public int priority;
    public boolean spreadUseNextTarget;
    public EntityBoundsEntry dimensionBounds = null;

    public EntityPopulator(JSONObject singlePopulatorObject) {
        type = ServerEntityType.valueOf(singlePopulatorObject.getString("type"));
        spawnChance = singlePopulatorObject.getFloat("chance");
        asStaticEntity = singlePopulatorObject.getBoolean("static");
        poissonDiskSamplerDistance = singlePopulatorObject.getDouble("pds");
        if(singlePopulatorObject.has("priority")) priority = singlePopulatorObject.getInt("priority");

        // optional
        if(singlePopulatorObject.has("spreadData")) {
            JSONObject spreadData = singlePopulatorObject.getJSONObject("spreadData");
            spreadChance = spreadData.getFloat("spreadChance");

            spreadBetweenAmount = new int[2];
            for(int i = 0; i < 2; i++) spreadBetweenAmount[i] = spreadData.getJSONArray("spread").getInt(i);

            spreadBetweenDistance = new float[2];
            for(int i = 0; i < 2; i++) spreadBetweenDistance[i] = spreadData.getJSONArray("spreadDis").getFloat(i);

            spreadBetweenEntities = new ServerEntityType[spreadData.getJSONArray("spreadType").length()];
            for(int i = 0; i < spreadBetweenEntities.length; i++) spreadBetweenEntities[i] = ServerEntityType.valueOf(spreadData.getJSONArray("spreadType").getString(i));

            spreadAsStaticEntity = spreadData.getBoolean("spreadStatic");

            spreadOffsets = new float[2];
            for(int i = 0; i < 2; i++) spreadOffsets[i] = spreadData.getJSONArray("spreadOffsets").getFloat(i);

            if(spreadData.has("spreadUseNextTarget")) spreadUseNextTarget = spreadData.getBoolean("spreadUseNextTarget");
        }
    }

    @Override
    public String toString() {
        return "EntityPopulator{" +
                "type=" + type +
                ", spawnChance=" + spawnChance +
                ", asStaticEntity=" + asStaticEntity +
                ", poissonDiskSamplerDistance=" + poissonDiskSamplerDistance +
                ", spreadBetweenAmount=" + Arrays.toString(spreadBetweenAmount) +
                ", spreadChance=" + spreadChance +
                ", spreadBetweenDistance=" + Arrays.toString(spreadBetweenDistance) +
                ", spreadBetweenEntities=" + Arrays.toString(spreadBetweenEntities) +
                ", spreadAsStaticEntity=" + spreadAsStaticEntity +
                ", spreadOffsets=" + Arrays.toString(spreadOffsets) +
                ", priority=" + priority +
                ", spreadUseNextTarget=" + spreadUseNextTarget +
                ", dimensionBounds=" + dimensionBounds +
                '}';
    }

}
