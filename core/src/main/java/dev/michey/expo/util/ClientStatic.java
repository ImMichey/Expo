package dev.michey.expo.util;

import com.badlogic.gdx.graphics.Color;

public class ClientStatic {

    /** Player properties */
    public static String PLAYER_USERNAME = System.getProperty("user.name");

    /** Client constants */
    public static final String SCREEN_MENU = "Menu";
    public static final String SCREEN_GAME = "Game";
    public static final float DEFAULT_CAMERA_ZOOM = 0.5f;
    public static final float CAMERA_ANIMATION_MIN_ZOOM = 0.075f;
    public static final boolean DEV_MODE = true; // Enables ImGui, Console, etc.

    public static final Color COLOR_ARMOR_TEXT = Color.valueOf("#a2cadc");
    public static final Color COLOR_CRAFT_TEXT = Color.valueOf("#c0ad9d");
    public static final Color COLOR_CRAFT_GREEN = Color.valueOf("#99e550");
    public static final Color COLOR_CRAFT_RED = Color.valueOf("#d72838");
    public static final Color COLOR_CRAFT_INGREDIENTS = Color.valueOf("#e1dbdb");

}