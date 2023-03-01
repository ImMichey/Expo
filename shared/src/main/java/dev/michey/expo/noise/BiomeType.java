package dev.michey.expo.noise;

public enum BiomeType {

    OCEAN(0, "Ocean", new float[] {65f/255f, 105f/255f, 225f/255f, 1.0f}),
    GRASS(1, "Grass", new float[] {34f/255f, 140f/255f, 34f/255f, 1.0f}),
    BEACH(2, "Beach", new float[] {238f/255f, 214f/255f, 175f/255f, 1.0f}),
    RIVER(3, "River", new float[] {65f/255f, 105f/255f, 225f/255f, 1.0f}),
    LAKE(4, "Lake", new float[] {65f/255f, 105f/255f, 225f/255f, 1.0f});

    public final int BIOME_ID;
    public final String BIOME_NAME;
    public final float[] BIOME_COLOR;

    BiomeType(int BIOME_ID, String BIOME_NAME, float[] BIOME_COLOR) {
        this.BIOME_ID = BIOME_ID;
        this.BIOME_NAME = BIOME_NAME;
        this.BIOME_COLOR = BIOME_COLOR;
    }

    public static BiomeType idToBiome(int id) {
        return switch (id) {
            case 1 -> GRASS;
            case 2 -> BEACH;
            case 3 -> RIVER;
            case 4 -> LAKE;
            default -> OCEAN;
        };
    }

    public static boolean isWater(BiomeType biomeType) {
        return switch (biomeType) {
            case OCEAN, RIVER, LAKE -> true;
            default -> false;
        };
    }

    /** Elevation values */
    private final static float ELEVATION_WATER = 0.515f; // 0.50 0.52 0.98 0.08
    private final static float ELEVATION_BEACH = 0.552f;
    private final static float ELEVATION_RIVER = 0.985f;
    private final static float ELEVATION_LAKE = 0.05f;

    public static BiomeType convertNoise(float noise, float riverNoise) {
        if(noise <= ELEVATION_WATER) return OCEAN;
        if(riverNoise >= ELEVATION_RIVER) return RIVER;
        if(riverNoise <= ELEVATION_LAKE) return LAKE;
        if(noise <= ELEVATION_BEACH) return BEACH;
        return GRASS;
    }

}
