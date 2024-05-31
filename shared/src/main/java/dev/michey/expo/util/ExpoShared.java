package dev.michey.expo.util;

import dev.michey.expo.noise.BiomeType;

import java.util.Random;
import java.util.Set;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoShared {

    /** Server constants */
    public static final int DEFAULT_EXPO_SERVER_PORT = 25010;
    public static int DEFAULT_LOCAL_TICK_RATE = 60;
    public static final int DEFAULT_DEDICATED_TICK_RATE = 60;
    public static final int CLIENT_TIMEOUT_THRESHOLD = 5000;
    public static final long UNLOAD_CHUNKS_AFTER_MILLIS = ExpoTime.RealWorld.seconds(5); // 5 seconds
    public static final long SAVE_CHUNKS_AFTER_MILLIS = ExpoTime.RealWorld.seconds(5); // 1 minute
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 1_048_576 * 4; // 4 MB
    public static final int DEFAULT_OBJECT_BUFFER_SIZE = 1_048_576 * 4; // 4 MB
    public static final int SERVER_PROTOCOL_VERSION = 1;
    public static final int STEAM_APP_ID = 1598080;

    /** Dimension constants */
    public static final String DIMENSION_OVERWORLD = "overworld";
    public static final String DIMENSION_CAVE = "cave";

    /** Animation constants */
    public static final int FRAMES_PLAYER_ANIMATION_IDLE = 2;
    public static final int FRAMES_PLAYER_ANIMATION_WALK = 8;

    /** Game mechanic constants */
    public static final int CHUNK_SIZE = 256;
    public static final int TILE_SIZE = 16;
    public static final int ROW_TILES = CHUNK_SIZE / TILE_SIZE;
    public static int PLAYER_CHUNK_VIEW_RANGE_X = 9;
    public static int PLAYER_CHUNK_VIEW_RANGE_Y = 7;
    public static int PLAYER_CHUNK_VIEW_RANGE_DIR_X = (PLAYER_CHUNK_VIEW_RANGE_X - 1) / 2; // (9 - 1) / 2 = 4
    public static int PLAYER_CHUNK_VIEW_RANGE_DIR_Y = (PLAYER_CHUNK_VIEW_RANGE_Y - 1) / 2; // (7 - 1) / 2 = 3

    public static final float PLAYER_AUDIO_RANGE = 2f * CHUNK_SIZE;

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
    public static final float PLAYER_DEFAULT_ATTACK_ANGLE_SPAN = 180;                   // Attack Angle Span
    public static final float PLAYER_DEFAULT_ATTACK_KNOCKBACK_STRENGTH = 12.0f;         // Knockback Strength
    public static final float PLAYER_DEFAULT_ATTACK_KNOCKBACK_DURATION = 0.25f;         // Knockback Duration
    public static float PLAYER_ARM_MOVEMENT_SEND_RATE = 1f / 60f;

    public static final int CRAFTING_CATEGORY_MISC = 0;
    public static final int CRAFTING_CATEGORY_TOOLS = 1;
    public static final int CRAFTING_CATEGORY_FOOD = 2;
    public static final int CRAFTING_CATEGORY_3 = 3;

    public static final int PLAYER_ANIMATION_ID_PICKUP = 0;
    public static final int PLAYER_ANIMATION_ID_PLACE = 1;

    public static final int CONTAINER_ID_VOID = -1;
    public static final int CONTAINER_ID_PLAYER = -2;

    public static boolean DUMP_THREADS = false;
    public static boolean TRACK_PERFORMANCE = false;

    // Reset
    public static final String RESET = "\033[0m";  // Text Reset

    // Regular Colors
    public static final String BLACK = "\033[0;30m";   // BLACK
    public static final String RED = "\033[0;31m";     // RED
    public static final String GREEN = "\033[0;32m";   // GREEN
    public static final String YELLOW = "\033[0;33m";  // YELLOW
    public static final String BLUE = "\033[0;34m";    // BLUE
    public static final String PURPLE = "\033[0;35m";  // PURPLE
    public static final String CYAN = "\033[0;36m";    // CYAN
    public static final String WHITE = "\033[0;37m";   // WHITE

    // Bold
    public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
    public static final String RED_BOLD = "\033[1;31m";    // RED
    public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
    public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
    public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
    public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
    public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

    // Underline
    public static final String BLACK_UNDERLINED = "\033[4;30m";  // BLACK
    public static final String RED_UNDERLINED = "\033[4;31m";    // RED
    public static final String GREEN_UNDERLINED = "\033[4;32m";  // GREEN
    public static final String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
    public static final String BLUE_UNDERLINED = "\033[4;34m";   // BLUE
    public static final String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
    public static final String CYAN_UNDERLINED = "\033[4;36m";   // CYAN
    public static final String WHITE_UNDERLINED = "\033[4;37m";  // WHITE

    // Background
    public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
    public static final String RED_BACKGROUND = "\033[41m";    // RED
    public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
    public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
    public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
    public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
    public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
    public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE

    // High Intensity
    public static final String BLACK_BRIGHT = "\033[0;90m";  // BLACK
    public static final String RED_BRIGHT = "\033[0;91m";    // RED
    public static final String GREEN_BRIGHT = "\033[0;92m";  // GREEN
    public static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
    public static final String BLUE_BRIGHT = "\033[0;94m";   // BLUE
    public static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
    public static final String CYAN_BRIGHT = "\033[0;96m";   // CYAN
    public static final String WHITE_BRIGHT = "\033[0;97m";  // WHITE

    // Bold High Intensity
    public static final String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
    public static final String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
    public static final String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
    public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
    public static final String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
    public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
    public static final String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
    public static final String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE

    // High Intensity backgrounds
    public static final String BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
    public static final String RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
    public static final String GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
    public static final String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
    public static final String BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
    public static final String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
    public static final String CYAN_BACKGROUND_BRIGHT = "\033[0;106m";  // CYAN
    public static final String WHITE_BACKGROUND_BRIGHT = "\033[0;107m";   // WHITE

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


    // Per float array:
    //  [0] = xStart
    //  [1] = yStart
    //  [2] = xEnd
    //  [3] = yEnd
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

    public static void threadDump() {
        if(!DUMP_THREADS) return;
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            var all = Thread.getAllStackTraces();
            Set<Thread> threadSet = all.keySet();

            for(var x : threadSet) {
                log(x.toString() + " . " + x.isAlive() + "/" + x.isInterrupted());

                for(StackTraceElement ste : all.get(x)) {
                    log("\t" + ste.toString());
                }
            }
        }).start();
    }

    public static Object toDisplayNumber(float number) {
        if(number == ((int) number)) {
            return (int) number;
        }

        return number;
    }

    public static Object toDisplayNumber(float number, int roundPlaces) {
        if(number == ((int) number)) {
            return (int) number;
        }

        int asInt = (int) (number * (roundPlaces * 10));
        return ((float) asInt) / (roundPlaces * 10);
    }

}