package dev.michey.expo.util;

import dev.michey.expo.noise.BiomeType;

import java.util.Random;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoShared {

    /** Server constants */
    public static final int DEFAULT_EXPO_SERVER_PORT = 25010;
    public static final int DEFAULT_SERVER_TICK_RATE = 60;
    public static final int CLIENT_TIMEOUT_THRESHOLD = 5000;
    public static final long UNLOAD_CHUNKS_AFTER_MILLIS = ExpoTime.RealWorld.seconds(5); // 5 seconds
    public static final long SAVE_CHUNKS_AFTER_MILLIS = ExpoTime.RealWorld.minutes(3); // 3 minutes
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 1_048_576 * 4; // 4 MB
    public static final int DEFAULT_OBJECT_BUFFER_SIZE = 1_048_576 * 4; // 4 MB
    public static final int SERVER_PROTOCOL_VERSION = 1;

    /** Dimension constants */
    public static final String DIMENSION_OVERWORLD = "overworld";
    public static final String DIMENSION_CAVE = "cave";

    /** Game mechanic constants */
    public static final int CHUNK_SIZE = 256;
    public static final int TILE_SIZE = 16;
    public static final int ROW_TILES = CHUNK_SIZE / TILE_SIZE;
    public static final int SPAWN_AREA_CHUNK_RANGE = 9;
    public static final int PLAYER_CHUNK_VIEW_RANGE_X = 7;
    public static final int PLAYER_CHUNK_VIEW_RANGE_Y = 5;
    public static final int PLAYER_CHUNK_VIEW_RANGE_DIR_X = (PLAYER_CHUNK_VIEW_RANGE_X - 1) / 2; // (7 - 1) / 2 = 3
    public static final int PLAYER_CHUNK_VIEW_RANGE_DIR_Y = (PLAYER_CHUNK_VIEW_RANGE_Y - 1) / 2; // (5 - 1) / 2 = 2

    public static final float PLAYER_AUDIO_RANGE = PLAYER_CHUNK_VIEW_RANGE_DIR_X * 0.5f * CHUNK_SIZE;

    public static final int PLAYER_INVENTORY_NO_ARMOR_SLOT_AMOUNT = 36;
    public static final int PLAYER_INVENTORY_SLOTS = PLAYER_INVENTORY_NO_ARMOR_SLOT_AMOUNT + 5;
    public static final int PLAYER_INVENTORY_SLOT_HEAD = 36;
    public static final int PLAYER_INVENTORY_SLOT_CHEST = 37;
    public static final int PLAYER_INVENTORY_SLOT_GLOVES = 38;
    public static final int PLAYER_INVENTORY_SLOT_LEGS = 39;
    public static final int PLAYER_INVENTORY_SLOT_FEET = 40;

    public static final int PLAYER_INVENTORY_SLOT_CRAFT_OPEN = 100;
    public static final int PLAYER_INVENTORY_SLOT_CRAFT_ARROW_LEFT = 101;
    public static final int PLAYER_INVENTORY_SLOT_CRAFT_ARROW_RIGHT = 102;
    public static final int PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_MISC = 103;
    public static final int PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_TOOLS = 104;
    public static final int PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_FOOD = 105;
    public static final int PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_3 = 106;
    public static final int PLAYER_INVENTORY_SLOT_CRAFT_RECIPE_BASE = 107;

    public static final int PLAYER_INVENTORY_SLOT_VOID = -1;
    public static final int PLAYER_INVENTORY_SLOT_CURSOR = -2;

    public static final int PLAYER_INVENTORY_ACTION_LEFT = 0;
    public static final int PLAYER_INVENTORY_ACTION_RIGHT = 1;
    public static final int PLAYER_INVENTORY_ACTION_MIDDLE = 2;

    public static final float PLAYER_DEFAULT_RANGE = 24.0f;
    public static final float PLAYER_DEFAULT_ATTACK_SPEED = 0.4f;
    public static final float PLAYER_DEFAULT_ATTACK_DAMAGE = 10.0f;
    public static final float PLAYER_DEFAULT_HARVEST_SPEED = 10.0f;
    public static final float PLAYER_DEFAULT_ATTACK_ANGLE_SPAN = 180;
    public static final float PLAYER_DEFAULT_ATTACK_KNOCKBACK_STRENGTH = 12.0f;
    public static final float PLAYER_DEFAULT_ATTACK_KNOCKBACK_DURATION = 0.33f;
    public static final float PLAYER_ARM_MOVEMENT_SEND_RATE = 1f / 60f;

    public static final int CRAFTING_CATEGORY_MISC = 0;
    public static final int CRAFTING_CATEGORY_TOOLS = 1;
    public static final int CRAFTING_CATEGORY_FOOD = 2;

    /** Global random */
    public static final Random RANDOM = new Random();

    /** Returns a pair of the converted integer and a boolean whether the conversion was successful or not. */
    public static Pair<Integer, Boolean> asInt(String value) {
        int intValue = 0;
        boolean success = false;

        try {
            intValue = Integer.parseInt(value);
            success = true;
        } catch (NumberFormatException ignored) { }

        return new Pair<>(intValue, success);
    }

    /** Returns a pair of the converted float and a boolean whether the conversion was successful or not. */
    public static Pair<Float, Boolean> asFloat(String value) {
        float floatValue = 0.0f;
        boolean success = false;

        try {
            floatValue = Float.parseFloat(value);
            success = true;
        } catch (NumberFormatException ignored) { }

        return new Pair<>(floatValue, success);
    }

    /** Returns a random BiomeType. */
    public static BiomeType randomBiome() {
        BiomeType[] bt = BiomeType.values();
        return bt[RANDOM.nextInt(bt.length)];
    }

    /** Converts an absolute world position to a tile position. */
    public static int posToTile(float worldPosition) {
        return floor(worldPosition) >> 4;
    }

    /** Converts an absolute world position to a chunk position. */
    public static int posToChunk(float worldPosition) {
        return floor(worldPosition) >> 8;
    }

    public static int floor(double num) {
        int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    /** Converts a chunk position to an absolute world position. */
    public static int chunkToPos(int chunkPosition) {
        return chunkPosition * CHUNK_SIZE;
    }

    /** Converts a tile position to an absolute world position. */
    public static int tileToPos(int tilePosition) {
        return tilePosition * TILE_SIZE;
    }

    public static boolean inAngleProximity(double baseAngle, double checkAngle, double angleSpan) {
        double halfSpan = angleSpan * 0.5d;
        double minus = baseAngle - halfSpan;
        double plus = baseAngle + halfSpan;

        if(minus >= 0 && plus <= 360) {
            return checkAngle >= minus && checkAngle <= plus;
        }

        boolean m, p;

        if(minus < 0) {
            double ceil = 360 + minus;
            m = checkAngle >= ceil;
        } else {
            m = checkAngle >= minus;
        }

        if(plus > 360) {
            double floor = plus - 360;
            p = checkAngle <= floor;
        } else {
            p = checkAngle <= plus;
        }

        return m || p;
    }

    public static boolean overlap(float[] vertices1, float[] vertices2) {
        float drawStartX = vertices2[0];
        float drawStartY = vertices2[1];

        float drawEndX = vertices2[2];
        float drawEndY = vertices2[3];

        return vertices1[0] < drawEndX
                && vertices1[2] > drawStartX
                && vertices1[1] < drawEndY
                && vertices1[3] > drawStartY;
    }

}