package dev.michey.expo.server.main.logic.world.dimension;

import dev.michey.expo.server.main.logic.entity.ServerDummy;
import dev.michey.expo.server.main.logic.world.ServerWorld;

public class ServerDimensionOverworld extends ServerDimension {

    public ServerDimensionOverworld(String dimensionName, boolean mainDimension) {
        super(dimensionName, mainDimension);
    }

    @Override
    public void onReady() {
        float baseX = dimensionSpawnX;
        float baseY = dimensionSpawnY;

        int generate = 3;//MathUtils.random(32, 64);

        for(int i = 0; i < generate; i++) {
            ServerDummy dummy = new ServerDummy();
            dummy.posX = baseX;
            dummy.posY = baseY;
            ServerWorld.get().registerServerEntity(dimensionName, dummy);
            //dummy.generateDst();
        }
    }

}
