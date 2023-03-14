package dev.michey.expo.util;

import com.badlogic.gdx.graphics.Color;

public class ClientStatic {

    /** Player properties */
    public static String PLAYER_USERNAME = System.getProperty("user.name");

    /** Client constants */
    public static final String SCREEN_MENU = "Menu";
    public static final String SCREEN_GAME = "Game";
    public static final float DEFAULT_CAMERA_ZOOM = 1f / 3f;
    public static final float CAMERA_ANIMATION_MIN_ZOOM = 0.075f;
    public static final boolean DEV_MODE = true; // Enables ImGui, Console, etc.

    /** Color constants */
    public static final Color COLOR_ARMOR_TEXT = Color.valueOf("#a2cadc");

}