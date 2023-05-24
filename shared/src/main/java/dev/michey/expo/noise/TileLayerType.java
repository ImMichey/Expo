package dev.michey.expo.noise;

public enum TileLayerType {

    EMPTY(0, new int[] {-1}, new String[] {"EMPTY"}),
    SOIL(1, new int[] {0}, new String[] {"SOIL"}),
    SOIL_HOLE(2, new int[] {90, 111}, new String[] {"SOIL_HOLE"}),
    SAND(3, new int[] {23, 44}, new String[] {"SAND", "DESERT"}),
    DESERT(4, new int[] {23, 44}, new String[] {"DESERT", "SAND"}),
    GRASS(5, new int[] {1, 22}, new String[] {"GRASS", "FOREST"}),
    FOREST(6, new int[] {112, 133}, new String[] {"FOREST", "GRASS"}),
    WATER(7, new int[] {46, 67}, new String[] {"WATER"}),
    WATER_DEEP(8, new int[] {68, 89}, new String[] {"WATER_DEEP", "WATER"}),
    ROCK(9, new int[] {156, 177}, new String[] {"DEEP"}),
    ;

    public final int SERIALIZATION_ID;
    public final int[] TILE_ID_DATA;
    public final String[] TILE_CONNECTION_DATA;

    TileLayerType(int SERIALIZATION_ID, int[] TILE_ID_DATA, String[] TILE_CONNECTION_DATA) {
        this.SERIALIZATION_ID = SERIALIZATION_ID;
        this.TILE_ID_DATA = TILE_ID_DATA;
        this.TILE_CONNECTION_DATA = TILE_CONNECTION_DATA;
    }

    public static String typeToItemDrop(TileLayerType type) {
        return switch (type) {
            case SOIL -> "item_dirt";
            case SAND -> "item_floor_sand";
            case FOREST, GRASS -> "item_floor_grass";
            default -> null;
        };
    }

    public static String typeToHitSound(TileLayerType type) {
        return switch (type) {
            case SAND -> "dig_sand";
            case SOIL -> "step_water";
            case GRASS, FOREST -> "grass_hit";
            default -> null;
        };
    }

    public static boolean isWater(TileLayerType type) {
        return switch (type) {
            case WATER, WATER_DEEP -> true;
            default -> false;
        };
    }

    public static boolean isConnected(TileLayerType t0, TileLayerType t1) {
        String[] check = t0.TILE_CONNECTION_DATA;
        if(check == null) return t0 == t1;

        for(String c : check) {
            if(TileLayerType.valueOf(c) == t1) return true;
        }

        return false;
    }

    public static TileLayerType biomeToLayer0(BiomeType type) {
        return SOIL;
    }

    public static TileLayerType biomeToLayer1(BiomeType type) {
        return switch (type) {
            case FOREST, DENSE_FOREST -> FOREST;
            case PLAINS -> GRASS;
            case DESERT -> DESERT;
            case BEACH -> SAND;
            case LAKE, RIVER, OCEAN -> WATER;
            case OCEAN_DEEP -> WATER_DEEP;
            default -> EMPTY;
        };
    }

    public static TileLayerType biomeToLayer2(BiomeType type) {
        return EMPTY;
    }

    public static TileLayerType biomeToLayer(BiomeType type, int layer) {
        if(layer == 1) {
            return biomeToLayer1(type);
        } else if(layer == 0) {
            return biomeToLayer0(type);
        } else {
            return biomeToLayer2(type);
        }
    }

}
