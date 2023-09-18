package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;

public enum BorderRequirement {

    WATER, LAND;

    public boolean meetsRequirements(BorderRequirement requirement, ServerTile[] tiles) {
        for(ServerTile tile : tiles) {
            if(tile == null) continue;

            if(requirement == WATER && !TileLayerType.isWater(tile.dynamicTileParts[2].emulatingType)) {
                return false;
            } else if(requirement == LAND && TileLayerType.isWater(tile.dynamicTileParts[2].emulatingType)) {
                return false;
            }
        }

        return true;
    }

}