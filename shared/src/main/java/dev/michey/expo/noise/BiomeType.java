package dev.michey.expo.noise;

public enum BiomeType {

    OCEAN(0, "Ocean", new int[] {0, 46, 67}, new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}, new String[] {"OCEAN", "RIVER", "LAKE", "OCEAN_DEEP"}),
    GRASS(1, "Grass", new int[] {0, 1, 22}, new float[] {108f/255f, 173f/255f, 76f/255f, 1.0f}, new String[] {"GRASS"}),
    BEACH(2, "Beach", new int[] {0, 23, 44}, new float[] {245f/255f, 241f/255f, 219f/255f, 1.0f}, new String[] {"BEACH"}),
    RIVER(3, "River", new int[] {0, 46, 67}, new float[] {65f/255f, 105f/255f, 225f/255f, 1.0f}, new String[] {"OCEAN", "RIVER", "LAKE", "OCEAN_DEEP"}),
    LAKE(4, "Lake", new int[] {0, 46, 67}, new float[] {65f/255f, 105f/255f, 225f/255f, 1.0f}, new String[] {"OCEAN", "RIVER", "LAKE", "OCEAN_DEEP"}),
    VOID(5, "Void", null, new float[] {0f, 0f, 0f, 1.0f}, new String[] {"VOID"}),
    OCEAN_DEEP(6, "Deep Ocean", new int[] {46, 68, 89}, new float[] {73f/255f, 135f/255f, 211f/255f, 1.0f}, new String[] {"OCEAN_DEEP"}),
    ;

    public final int BIOME_ID;
    public final String BIOME_NAME;
    public final int[] BIOME_LAYER_TEXTURES;
    public final float[] BIOME_COLOR;
    public final String[] BIOME_NEIGHBOURS;

    BiomeType(int BIOME_ID, String BIOME_NAME, int[] BIOME_LAYER_TEXTURES, float[] BIOME_COLOR, String[] BIOME_NEIGHBOURS) {
        this.BIOME_ID = BIOME_ID;
        this.BIOME_NAME = BIOME_NAME;
        this.BIOME_LAYER_TEXTURES = BIOME_LAYER_TEXTURES;
        this.BIOME_COLOR = BIOME_COLOR;
        this.BIOME_NEIGHBOURS = BIOME_NEIGHBOURS;
    }

    public static BiomeType idToBiome(int id) {
        return switch (id) {
            case 1 -> GRASS;
            case 2 -> BEACH;
            case 3 -> RIVER;
            case 4 -> LAKE;
            case 5 -> VOID;
            case 6 -> OCEAN_DEEP;
            default -> OCEAN;
        };
    }

    public static boolean isWater(BiomeType biomeType) {
        return switch (biomeType) {
            case OCEAN, RIVER, LAKE, OCEAN_DEEP -> true;
            default -> false;
        };
    }

    /** Elevation values */
    private final static float ELEVATION_WATER_DEEP = 0.465f; // 0.50 0.52 0.98 0.08
    private final static float ELEVATION_WATER = 0.515f;
    private final static float ELEVATION_BEACH = 0.552f;
    private final static float ELEVATION_RIVER = 0.985f;
    private final static float ELEVATION_LAKE = 0.05f;

    public static BiomeType convertNoise(float noise, float riverNoise) {
        if(noise <= ELEVATION_WATER_DEEP) return OCEAN_DEEP;
        if(noise <= ELEVATION_WATER) return OCEAN;
        //if(riverNoise >= ELEVATION_RIVER) return RIVER;
        //if(riverNoise <= ELEVATION_LAKE) return LAKE;
        if(noise <= ELEVATION_BEACH) return BEACH;
        return GRASS;
    }

}
