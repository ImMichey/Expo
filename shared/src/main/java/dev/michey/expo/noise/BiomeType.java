package dev.michey.expo.noise;

import static dev.michey.expo.util.ExpoShared.fconvS;

public enum BiomeType {

    OCEAN(			0, 	"Ocean", 				new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    PLAINS(			1, 	"Plains", 				new float[] {108f/255f, 173f/255f, 76f/255f, 1.0f}, 		fconvS(255, 255, 255),  true),
    BEACH(			2, 	"Beach", 				new float[] {245f/255f, 241f/255f, 219f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    RIVER(			3, 	"River", 				new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    LAKE(			4, 	"Lake", 				new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    VOID(			5, 	"Void", 				new float[] {0.0f, 0.0f, 0.0f, 1.0f}, 						fconvS(255, 255, 255),	false),
    OCEAN_DEEP(		6, 	"Deep Ocean", 			new float[] {73f/255f, 135f/255f, 211f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    FOREST(			7, 	"Forest", 				new float[] {87f/255f, 143f/255f, 65f/255f, 1.0f}, 			fconvS(255, 255, 255),	false),
    DESERT(			8, 	"Desert", 				new float[] {243f/255f, 232f/255f, 212f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    DENSE_FOREST(	9, 	"Dense Forest", 		new float[] {57f/255f, 113f/255f, 35f/255f, 1.0f}, 			fconvS(255, 255, 255),	true),
    ROCK(			10, 	"Rock", 				new float[] {71f/255f, 71f/255f, 76f/255f, 1.0f}, 			fconvS(255, 255, 255),	false),
    WHEAT_FIELDS(	11, 	"Wheat Fields", 		new float[] {140f/255f, 205f/255f, 76f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    PUDDLE(			12, 	"Puddle", 				new float[] {88f/255f, 158f/255f, 220f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    DIRT(			13, 	"Dirt", 				new float[] {78f/255f, 67f/255f, 55f/255f, 1.0f}, 			fconvS(255, 255, 255),	false),
    BARREN(			14, 	"Barren", 				new float[] {0.0f, 0.0f, 0.0f, 1.0f}, 						fconvS(255, 255, 255),	false),
    LAKE_DEEP(		15, 	"Deep Lake", 			new float[] {73f/255f, 135f/255f, 211f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    RIVER_DEEP(		16, 	"Deep River", 			new float[] {73f/255f, 135f/255f, 211f/255f, 1.0f}, 		fconvS(255, 255, 255),	false),
    ;

    public final int BIOME_ID;
    public final String BIOME_NAME;
    public final float[] BIOME_COLOR;
    public final float[] FOLIAGE_COLOR;
    public final boolean FOLIAGE_INDEX;

    BiomeType(int BIOME_ID, String BIOME_NAME, float[] BIOME_COLOR, float[] FOLIAGE_COLOR, boolean FOLIAGE_INDEX) {
        this.BIOME_ID = BIOME_ID;
        this.BIOME_NAME = BIOME_NAME;
        this.BIOME_COLOR = BIOME_COLOR;
        this.FOLIAGE_COLOR = FOLIAGE_COLOR;
        this.FOLIAGE_INDEX = FOLIAGE_INDEX;
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
            case 14 -> BARREN;
            case 15 -> LAKE_DEEP;
            case 16 -> RIVER_DEEP;
            default -> OCEAN;
        };
    }

    public static boolean isWater(BiomeType biomeType) {
        return switch (biomeType) {
            case OCEAN, RIVER, LAKE, OCEAN_DEEP, PUDDLE, RIVER_DEEP, LAKE_DEEP -> true;
            default -> false;
        };
    }

}
