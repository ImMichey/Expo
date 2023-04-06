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

    /** Dimension constants */
    public static final String DIMENSION_OVERWORLD = "overworld";
    public static final String DIMENSION_CAVE = "cave";

    /** Game mechanic constants */
    public static final int CHUNK_SIZE = 128;
    public static final int TILE_SIZE = 16;
    public static final int ROW_TILES = CHUNK_SIZE / TILE_SIZE;
    public static final int SPAWN_AREA_CHUNK_RANGE = 11;
    public static final int PLAYER_CHUNK_VIEW_RANGE = 11;
    public static final int PLAYER_CHUNK_VIEW_RANGE_ONE_DIR = (PLAYER_CHUNK_VIEW_RANGE - 1) / 2; // (11 - 1) / 2 = 5

    public static final float PLAYER_AUDIO_RANGE = PLAYER_CHUNK_VIEW_RANGE_ONE_DIR * CHUNK_SIZE;

    public static final int PLAYER_INVENTORY_SLOTS = 36 + 5;
    public static final int PLAYER_INVENTORY_SLOT_HEAD = 36;
    public static final int PLAYER_INVENTORY_SLOT_CHEST = 37;
    public static final int PLAYER_INVENTORY_SLOT_GLOVES = 38;
    public static final int PLAYER_INVENTORY_SLOT_LEGS = 39;
    public static final int PLAYER_INVENTORY_SLOT_FEET = 40;

    public static final int PLAYER_INVENTORY_SLOT_VOID = -1;
    public static final int PLAYER_INVENTORY_SLOT_CURSOR = -2;

    public static final int PLAYER_INVENTORY_ACTION_LEFT = 0;
    public static final int PLAYER_INVENTORY_ACTION_RIGHT = 1;
    public static final int PLAYER_INVENTORY_ACTION_MIDDLE = 2;

    public static final float PLAYER_DEFAULT_RANGE = 20.0f;
    public static final float PLAYER_DEFAULT_ATTACK_SPEED = 0.4f;
    public static final float PLAYER_DEFAULT_ATTACK_DAMAGE = 10.0f;
    public static final float PLAYER_ARM_MOVEMENT_SEND_RATE = 1f / 60f;

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

    /** Converts an absolute world position to a chunk position. */
    public static int posToChunk(float worldPosition) {
        float bonus = worldPosition < 0 ? CHUNK_SIZE : 0;
        return (int) ((worldPosition - bonus) / CHUNK_SIZE);
    }

    /** Converts an absolute world position to a tile position. */
    public static int posToTile(float worldPosition) {
        int cx = posToChunk(worldPosition);
        int tx = (int) ((worldPosition - (cx * CHUNK_SIZE)) / TILE_SIZE);
        return cx * ROW_TILES + tx;
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

}