package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

import java.util.Arrays;

public class SpreadData {

    public boolean spreadUseNextTarget;
    public int[] spreadBetweenAmount;
    public double spreadChance;
    public float[] spreadBetweenDistance;
    public ServerEntityType[] spreadBetweenEntities;
    public boolean spreadAsStaticEntity;
    public float[] spreadOffsets;
    public boolean spreadIgnoreOriginBounds;

    public SpreadData(JSONObject spreadData) {
        spreadChance = spreadData.getDouble("spreadChance");

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
        if(spreadData.has("spreadIgnoreOriginBounds")) spreadIgnoreOriginBounds = spreadData.getBoolean("spreadIgnoreOriginBounds");
    }

    @Override
    public String toString() {
        return "SpreadData{" +
                "spreadUseNextTarget=" + spreadUseNextTarget +
                ", spreadBetweenAmount=" + Arrays.toString(spreadBetweenAmount) +
                ", spreadChance=" + spreadChance +
                ", spreadBetweenDistance=" + Arrays.toString(spreadBetweenDistance) +
                ", spreadBetweenEntities=" + Arrays.toString(spreadBetweenEntities) +
                ", spreadAsStaticEntity=" + spreadAsStaticEntity +
                ", spreadOffsets=" + Arrays.toString(spreadOffsets) +
                ", spreadIgnoreOriginBounds=" + spreadIgnoreOriginBounds +
                '}';
    }

}
