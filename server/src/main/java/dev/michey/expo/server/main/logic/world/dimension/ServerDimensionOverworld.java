package dev.michey.expo.server.main.logic.world.dimension;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.main.logic.entity.misc.ServerDummy;

public class ServerDimensionOverworld extends ServerDimension {

    public ServerDimensionOverworld(String dimensionName, boolean mainDimension) {
        super(dimensionName, mainDimension);
    }

    @Override
    public void onReady() {
        float baseX = dimensionSpawnX;
        float baseY = dimensionSpawnY;

        int generate = MathUtils.random(2, 4);

        for(int i = 0; i < generate; i++) {
            ServerDummy dummy = new ServerDummy();
            dummy.posX = baseX;
            dummy.posY = baseY;
            // ServerWorld.get().registerServerEntity(dimensionName, dummy);
        }
    }

}