package dev.michey.expo.server.main.logic.inventory.item;

import dev.michey.expo.noise.TileLayerType;

public enum FloorType {

    GRASS(TileLayerType.GRASS),
    SAND(TileLayerType.SAND),
    DIRT(TileLayerType.SOIL),
    ;

    public final TileLayerType TILE_LAYER_TYPE;

    FloorType(TileLayerType TILE_LAYER_TYPE) {
        this.TILE_LAYER_TYPE = TILE_LAYER_TYPE;
    }

}
