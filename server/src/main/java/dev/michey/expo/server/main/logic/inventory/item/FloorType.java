package dev.michey.expo.server.main.logic.inventory.item;

import dev.michey.expo.noise.TileLayerType;

public enum FloorType {

    GRASS(TileLayerType.FOREST),
    SAND(TileLayerType.SAND),
    DIRT(TileLayerType.SOIL),
    OAK_PLANK(TileLayerType.OAK_PLANK),
    OAK_PLANK_WALL(TileLayerType.OAKPLANKWALL),
    ROCK_WALL(TileLayerType.ROCK),
    DIRT_WALL(TileLayerType.DIRT),
    HEDGE(TileLayerType.HEDGE),
    ;

    public final TileLayerType TILE_LAYER_TYPE;

    FloorType(TileLayerType TILE_LAYER_TYPE) {
        this.TILE_LAYER_TYPE = TILE_LAYER_TYPE;
    }

}
