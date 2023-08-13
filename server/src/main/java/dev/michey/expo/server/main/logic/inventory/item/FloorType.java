package dev.michey.expo.server.main.logic.inventory.item;

import dev.michey.expo.noise.TileLayerType;

public enum FloorType {

    GRASS(TileLayerType.FOREST),
    SAND(TileLayerType.SAND),
    DIRT(TileLayerType.SOIL),
    OAK_PLANK(TileLayerType.OAK_PLANK),
    ;

    public final TileLayerType TILE_LAYER_TYPE;

    FloorType(TileLayerType TILE_LAYER_TYPE) {
        this.TILE_LAYER_TYPE = TILE_LAYER_TYPE;
    }

}
