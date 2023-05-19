package dev.michey.expo.noise;

import java.util.HashMap;

public enum TileLayerType {

    EMPTY(0),
    SOIL(1), SOIL_HOLE(2),                                                  // Layer 0
    SAND(3), DESERT(4), GRASS(5), FOREST(6), WATER(7), WATER_DEEP(8),       // Layer 1
    ;

    public static final HashMap<TileLayerType, int[]> TEXTURE_MAP;
    public static final HashMap<TileLayerType, TileLayerType[]> CONNECTION_MAP;

    public final int SERIALIZATION_ID;

    TileLayerType(int SERIALIZATION_ID) {
        this.SERIALIZATION_ID = SERIALIZATION_ID;
    }

    static {
        TEXTURE_MAP = new HashMap<>();
        TEXTURE_MAP.put(TileLayerType.SOIL, new int[] {0});
        TEXTURE_MAP.put(TileLayerType.SOIL_HOLE, new int[] {90, 111});
        TEXTURE_MAP.put(TileLayerType.GRASS, new int[] {1, 22});
        TEXTURE_MAP.put(TileLayerType.SAND, new int[] {23, 44});
        TEXTURE_MAP.put(TileLayerType.WATER, new int[] {46, 67});
        TEXTURE_MAP.put(TileLayerType.WATER_DEEP, new int[] {68, 89});
        TEXTURE_MAP.put(TileLayerType.DESERT, new int[] {134, 155});
        TEXTURE_MAP.put(TileLayerType.FOREST, new int[] {112, 133});

        CONNECTION_MAP = new HashMap<>();
        CONNECTION_MAP.put(WATER_DEEP, new TileLayerType[] {WATER, WATER_DEEP});
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
        TileLayerType[] check = CONNECTION_MAP.get(t0);
        if(check == null) return t0 == t1;

        for(TileLayerType c : check) {
            if(c == t0) return true;
        }

        return false;
    }

    public static TileLayerType biomeToLayer0(BiomeType type) {
        return switch (type) {
            case OCEAN_DEEP -> WATER;
            default -> SOIL;
        };
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

    public static int[] typeToTextures(TileLayerType type) {
        return TEXTURE_MAP.get(type);
    }

}
