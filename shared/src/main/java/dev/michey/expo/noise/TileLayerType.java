package dev.michey.expo.noise;

import java.util.HashMap;

public enum TileLayerType {

    EMPTY(0,                new int[] {-1},         new String[] {"EMPTY"},                                 false),
    SOIL(1,                 new int[] {0},          new String[] {"SOIL"},                                  false),
    SOIL_HOLE(2,            new int[] {90, 111},    new String[] {"SOIL_HOLE"},                             false),
    SAND(3,                 new int[] {23, 44},     new String[] {"SAND", "DESERT"},                        false),
    DESERT(4,               new int[] {23, 44},     new String[] {"DESERT", "SAND"},                        false),
    GRASS(5,                new int[] {1, 22},      new String[] {"GRASS", "FOREST"},                       false),
    FOREST(6,               new int[] {112, 133},   new String[] {"FOREST", "GRASS"},                       false),
    WATER(7,                new int[] {46, 67},     new String[] {"WATER", "WATER_SANDY"},                  false),
    WATER_DEEP(8,           new int[] {68, 89},     new String[] {"WATER_DEEP", "WATER", "WATER_SANDY", "WATER_OVERLAY"},    false),
    ROCK(9,                 new int[] {156, 177},   new String[] {"ROCK"},                                  true),
    SOIL_FARMLAND(10,       new int[] {178, 199},   new String[] {"SOIL_FARMLAND"},                         false),
    OAK_PLANK(11,           new int[] {200, 221},   new String[] {"OAK_PLANK"},                             false),
    DIRT(12,                new int[] {222, 243},   new String[] {"DIRT"},                                  true),
    WATER_SANDY(13,         new int[] {244, 265},   new String[] {"WATER_SANDY", "WATER"},                  false),
    OAKPLANKWALL(14,        new int[] {266, 287},   new String[] {"OAKPLANKWALL"},                          true),
    WATER_OVERLAY(15,       new int[] {288, 309},   new String[] {"WATER_OVERLAY", "WATER_DEEP"},           false),
    SAND_WATERLOGGED(16,    new int[] {310, 331},   new String[] {"SAND_WATERLOGGED"},                      false),
    SOIL_DEEP_WATERLOGGED(17,new int[] {332, 353},  new String[] {"SOIL_DEEP_WATERLOGGED", "SOIL_WATERLOGGED"},                 false),
    SOIL_WATERLOGGED(18,    new int[] {354, 375},   new String[] {"SOIL_WATERLOGGED"},false),
    ;

    public final int SERIALIZATION_ID;
    public final int[] TILE_ID_DATA;
    public final String[] TILE_CONNECTION_DATA;
    public final boolean TILE_IS_WALL;

    public static final HashMap<TileLayerType, String> ELEVATION_TEXTURE_MAP;

    static {
        ELEVATION_TEXTURE_MAP = new HashMap<>();
        ELEVATION_TEXTURE_MAP.put(ROCK, "tile_rock_elevation");
        ELEVATION_TEXTURE_MAP.put(DIRT, "tile_dirt_elevation");
        ELEVATION_TEXTURE_MAP.put(OAKPLANKWALL, "tile_oakplankwall_elevation");
    }

    TileLayerType(int SERIALIZATION_ID, int[] TILE_ID_DATA, String[] TILE_CONNECTION_DATA, boolean TILE_IS_WALL) {
        this.SERIALIZATION_ID = SERIALIZATION_ID;
        this.TILE_ID_DATA = TILE_ID_DATA;
        this.TILE_CONNECTION_DATA = TILE_CONNECTION_DATA;
        this.TILE_IS_WALL = TILE_IS_WALL;
    }

    public static TileLayerType serialIdToType(int id) {
        return switch (id) {
            case 1 -> SOIL;
            case 2 -> SOIL_HOLE;
            case 3 -> SAND;
            case 4 -> DESERT;
            case 5 -> GRASS;
            case 6 -> FOREST;
            case 7 -> WATER;
            case 8 -> WATER_DEEP;
            case 9 -> ROCK;
            case 10 -> SOIL_FARMLAND;
            case 11 -> OAK_PLANK;
            case 12 -> DIRT;
            case 13 -> WATER_SANDY;
            case 14 -> OAKPLANKWALL;
            case 15 -> WATER_OVERLAY;
            case 16 -> SAND_WATERLOGGED;
            case 17 -> SOIL_DEEP_WATERLOGGED;
            case 18 -> SOIL_WATERLOGGED;
            default -> EMPTY;
        };
    }

    public static float color255Packed(int r, int g, int b, int a) {
        int color = (a << 24) | (b << 16) | (g << 8) | r;
        return Float.intBitsToFloat(color & 0xfeffffff);
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
            case WATER, WATER_DEEP, WATER_SANDY, WATER_OVERLAY, SAND_WATERLOGGED, SOIL_DEEP_WATERLOGGED, SOIL_WATERLOGGED -> true;
            default -> false;
        };
    }

    public static boolean isWaterBiome(BiomeType type) {
        return switch (type) {
            case OCEAN, OCEAN_DEEP, PUDDLE, LAKE, RIVER -> true;
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
        return switch (type) {
            case PUDDLE, LAKE, RIVER, OCEAN -> SOIL_WATERLOGGED;
            default -> SOIL;
        };
    }

    public static TileLayerType biomeToLayer1(BiomeType type) {
        return switch (type) {
            case FOREST, DENSE_FOREST, PLAINS, WHEAT_FIELDS -> FOREST;
            case BEACH -> SAND;
            case OCEAN_DEEP -> SOIL_DEEP_WATERLOGGED;
            case PUDDLE, LAKE, RIVER, OCEAN -> SOIL_WATERLOGGED;
            default -> EMPTY;
        };
    }

    public static TileLayerType biomeToLayer2(BiomeType type) {
        return switch (type) {
            case ROCK -> ROCK;
            case DIRT -> DIRT;
            case RIVER, PUDDLE, OCEAN_DEEP, OCEAN, LAKE -> WATER_OVERLAY;
            //case OCEAN -> WATER_SANDY;
            //case LAKE, RIVER, PUDDLE -> WATER;
            //case OCEAN_DEEP -> WATER_DEEP;
            default -> EMPTY;
        };
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
