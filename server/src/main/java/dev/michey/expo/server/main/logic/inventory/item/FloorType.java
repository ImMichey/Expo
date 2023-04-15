package dev.michey.expo.server.main.logic.inventory.item;

public enum FloorType {

    GRASS(1),
    SAND(23),
    DIRT(0),
    ;

    public final int TILE_TEXTURE_ID;

    FloorType(int TILE_TEXTURE_ID) {
        this.TILE_TEXTURE_ID = TILE_TEXTURE_ID;
    }

}
