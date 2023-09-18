package dev.michey.expo.util;

import com.badlogic.gdx.graphics.Color;

public class ClientStatic {

    /** Player properties */
    public static String PLAYER_USERNAME = System.getProperty("user.name");

    /** Client constants */
    public static final String SCREEN_MENU = "Menu";
    public static final String SCREEN_GAME = "Game";
    public static float DEFAULT_CAMERA_ZOOM = 0.5f; // Modified by GameSettings
    public static final float CAMERA_ANIMATION_MIN_ZOOM = 0.075f;
    public static boolean DEV_MODE = false; // Enables ImGui, Console, etc.

    public static final String GAME_VERSION = "0.0.0 (Unstable)";

    public static final Color COLOR_ARMOR_TEXT = Color.valueOf("#a2cadc");

    public static final Color COLOR_CRAFT_TEXT = Color.valueOf("#c0ad9d");
    public static final Color COLOR_CRAFT_GREEN = Color.valueOf("#99e550");
    public static final Color COLOR_CRAFT_RED = Color.valueOf("#d72838");
    public static final Color COLOR_CRAFT_INGREDIENTS = Color.valueOf("#e1dbdb");

    public static final Color COLOR_DAMAGE_TINT = Color.valueOf("#ca3636");

    public static final float[] CAMERA_ZOOM_LEVELS = new float[] {0.1f, 0.2f, 0.25f, 1f / 3f, 0.5f, 1.0f};
    public static int DEFAULT_CAMERA_ZOOM_INDEX = 4;

}