package dev.michey.expo.noise;

public enum BiomeType {

    OCEAN(0, "Ocean", new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}),
    PLAINS(1, "Plains", new float[] {108f/255f, 173f/255f, 76f/255f, 1.0f}),
    BEACH(2, "Beach", new float[] {245f/255f, 241f/255f, 219f/255f, 1.0f}),
    RIVER(3, "River", new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}),
    LAKE(4, "Lake", new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}),
    VOID(5, "Void", new float[] {0f, 0f, 0f, 1.0f}),
    OCEAN_DEEP(6, "Deep Ocean", new float[] {73f/255f, 135f/255f, 211f/255f, 1.0f}),
    FOREST(7, "Forest", new float[] {87f/255f, 143f/255f, 65f/255f, 1.0f}),
    DESERT(8, "Desert", new float[] {243f/255f, 232f/255f, 212f/255f, 1.0f}),
    DENSE_FOREST(9, "Dense Forest", new float[] {57f/255f, 113f/255f, 35f/255f, 1.0f}),
    ROCK(10, "Rock", new float[] {71f/255f, 71f/255f, 76f/255f, 1.0f}),
    WHEAT_FIELDS(11, "WheatFields", new float[] {140f/255f, 205f/255f, 76f/255f, 1.0f}),
    PUDDLE(12, "Puddle", new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}),
    DIRT(13, "Dirt", new float[] {78f/255f, 67f/255f, 55f/255f, 1.0f}),
    ;

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
            case 1 -> PLAINS;
            case 2 -> BEACH;
            case 3 -> RIVER;
            case 4 -> LAKE;
            case 5 -> VOID;
            case 6 -> OCEAN_DEEP;
            case 7 -> FOREST;
            case 8 -> DESERT;
            case 9 -> DENSE_FOREST;
            case 10 -> ROCK;
            case 11 -> WHEAT_FIELDS;
            case 12 -> PUDDLE;
            case 13 -> DIRT;
            default -> OCEAN;
        };
    }

    public static boolean isWater(BiomeType biomeType) {
        return switch (biomeType) {
            case OCEAN, RIVER, LAKE, OCEAN_DEEP, PUDDLE -> true;
            default -> false;
        };
    }

}
