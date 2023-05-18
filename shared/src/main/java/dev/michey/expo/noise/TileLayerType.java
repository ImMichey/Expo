package dev.michey.expo.noise;

public enum TileLayerType {

    EMPTY,
    SOIL, SOIL_HOLE,                                    // Layer 0
    SAND, DESERT, GRASS, FOREST, WATER, WATER_DEEP,     // Layer 1
    ;

    public static TileLayerType biomeToLayer0(BiomeType type) {
        return SOIL;
    }

    public static TileLayerType biomeToLayer1(BiomeType type) {
        return switch (type) {
            case FOREST, DENSE_FOREST -> TileLayerType.FOREST;
            case PLAINS -> TileLayerType.GRASS;
            case DESERT -> TileLayerType.DESERT;
            case BEACH -> TileLayerType.SAND;
            case LAKE, RIVER, OCEAN -> TileLayerType.WATER;
            case OCEAN_DEEP -> TileLayerType.WATER_DEEP;
            default -> TileLayerType.EMPTY;
        };
    }

}
