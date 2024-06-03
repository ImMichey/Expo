package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;

public enum BorderRequirement {

    WATER, LAND;

    public boolean meetsRequirements(BorderRequirement requirement, ServerTile[] tiles) {
        for(ServerTile tile : tiles) {
            if(tile == null) continue;
            if(tile.dynamicTileParts == null) continue;
            if(tile.dynamicTileParts[2].emulatingType == null) continue;

            if(requirement == WATER && !TileLayerType.isWater(tile.dynamicTileParts[2].emulatingType)) {
                return false;
            } else if(requirement == LAND && TileLayerType.isWater(tile.dynamicTileParts[2].emulatingType)) {
                return false;
            }
        }

        return true;
    }

    public boolean meetsRequirementsAny(BorderRequirement requirement, int requirementCount, ServerTile[] tiles) {
        int found = 0;

        for(ServerTile tile : tiles) {
            if(tile == null) continue;
            if(tile.dynamicTileParts == null) continue;
            if(tile.dynamicTileParts[2].emulatingType == null) continue;

            if(requirement == WATER && TileLayerType.isWater(tile.dynamicTileParts[2].emulatingType)) {
                found++;
                if(found >= requirementCount) return true;
            }

            if(requirement == LAND && !TileLayerType.isWater(tile.dynamicTileParts[2].emulatingType)) {
                found++;
                if(found >= requirementCount) return true;
            }
        }

        return false;
    }

}