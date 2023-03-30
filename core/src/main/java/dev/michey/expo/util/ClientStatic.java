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

    public static final Color COLOR_PARTICLE_GRASS_1 = Color.valueOf("#87d46b");
    public static final Color COLOR_PARTICLE_GRASS_2 = Color.valueOf("#70cc4f");

    public static final Color COLOR_PARTICLE_MUSHROOM_1 = Color.valueOf("#f5ddd1");
    public static final Color COLOR_PARTICLE_MUSHROOM_2 = Color.valueOf("#e1ccc2");

    // public static final Color COLOR_WEATHER_RAIN_OVERLAY = Color.valueOf("#68c7db");

}