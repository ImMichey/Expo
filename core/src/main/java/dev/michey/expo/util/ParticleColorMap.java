package dev.michey.expo.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ParticleColorMap {

    /** Map */
    public static final HashMap<Integer, List<Color>> COLOR_MAP;

    /** Color constants */
    public static final Color COLOR_PARTICLE_SOIL_1 = Color.valueOf("#38352b");

    public static final Color COLOR_PARTICLE_GRASS_1 = Color.valueOf("#87d46b");
    public static final Color COLOR_PARTICLE_GRASS_2 = Color.valueOf("#70cc4f");

    public static final Color COLOR_PARTICLE_BLUEBERRYBUSH_1 = Color.valueOf("#67b34a");
    public static final Color COLOR_PARTICLE_BLUEBERRYBUSH_2 = Color.valueOf("#519737");

    public static final Color COLOR_PARTICLE_MUSHROOM_1 = Color.valueOf("#f5ddd1");
    public static final Color COLOR_PARTICLE_MUSHROOM_2 = Color.valueOf("#e1ccc2");

    public static final Color COLOR_PARTICLE_SAND_1 = Color.valueOf("#fffee8");
    public static final Color COLOR_PARTICLE_SAND_2 = Color.valueOf("#ffffec");

    public static final Color COLOR_PARTICLE_WHEAT_1 = Color.valueOf("#e6cb7d");
    public static final Color COLOR_PARTICLE_WHEAT_2 = Color.valueOf("#ffee91");

    public static final Color COLOR_PARTICLE_LOG_1 = Color.valueOf("#1e180e");
    public static final Color COLOR_PARTICLE_LOG_2 = Color.valueOf("#563d1a");
    public static final Color COLOR_PARTICLE_LOG_3 = Color.valueOf("#51432e");

    public static final Color COLOR_PARTICLE_STICK_1 = Color.valueOf("#604c31");
    public static final Color COLOR_PARTICLE_STICK_2 = Color.valueOf("#604828");

    public static final Color COLOR_PARTICLE_ROCK_1 = Color.valueOf("#ccd7d7");
    public static final Color COLOR_PARTICLE_ROCK_2 = Color.valueOf("#b1baba");
    public static final Color COLOR_PARTICLE_COAL_1 = Color.valueOf("#10021b");
    public static final Color COLOR_PARTICLE_COAL_2 = Color.valueOf("#2e2d32");

    public static final Color COLOR_PARTICLE_DYNAMIC_ROCK_1 = Color.valueOf("#808683");
    public static final Color COLOR_PARTICLE_DYNAMIC_ROCK_2 = Color.valueOf("#8e9a94");

    public static final Color COLOR_PARTICLE_DYNAMIC_DIRT_1 = Color.valueOf("#9d8b72");
    public static final Color COLOR_PARTICLE_DYNAMIC_DIRT_2 = Color.valueOf("#a28c6b");

    public static final Color COLOR_PARTICLE_DYNAMIC_OAK_PLANK_1 = Color.valueOf("#8e9a94");
    public static final Color COLOR_PARTICLE_DYNAMIC_OAK_PLANK_2 = Color.valueOf("#8e9a94");

    public static final Color COLOR_PARTICLE_BLOOD_1 = Color.valueOf("#bb2525");
    public static final Color COLOR_PARTICLE_BLOOD_2 = Color.valueOf("#721d1d");

    public static final Color COLOR_PARTICLE_SMOKE_1 = Color.valueOf("#978d8d");
    public static final Color COLOR_PARTICLE_SMOKE_2 = Color.valueOf("#b6b0b0");
    public static final Color COLOR_PARTICLE_SMOKE_3 = Color.valueOf("#777373");

    public static final Color COLOR_PARTICLE_DUST_1 = Color.valueOf("#c5b7a3");
    public static final Color COLOR_PARTICLE_DUST_2 = Color.valueOf("#b6a791");
    public static final Color COLOR_PARTICLE_DUST_3 = Color.valueOf("#c7b292");
    public static final Color COLOR_PARTICLE_DUST_4 = Color.valueOf("#b39d7d");

    public static final Color COLOR_PARTICLE_STONE_DUST_1 = Color.valueOf("#a49783");
    public static final Color COLOR_PARTICLE_STONE_DUST_2 = Color.valueOf("#908472");
    public static final Color COLOR_PARTICLE_STONE_DUST_3 = Color.valueOf("#cdbca5");

    public static final Color COLOR_PARTICLE_WATER_1 = Color.valueOf("#84b6e0");
    public static final Color COLOR_PARTICLE_WATER_2 = Color.valueOf("#8dbae0");
    public static final Color COLOR_PARTICLE_WATER_3 = Color.valueOf("#78acda");

    static {
        COLOR_MAP = new HashMap<>();

        colorEntry(0, COLOR_PARTICLE_SOIL_1);

        colorEntry(1, COLOR_PARTICLE_GRASS_1);
        colorEntry(1, COLOR_PARTICLE_GRASS_2);

        colorEntry(2, COLOR_PARTICLE_SAND_1);
        colorEntry(2, COLOR_PARTICLE_SAND_2);

        colorEntry(3, COLOR_PARTICLE_MUSHROOM_1);
        colorEntry(3, COLOR_PARTICLE_MUSHROOM_2);

        colorEntry(4, COLOR_PARTICLE_WHEAT_1);
        colorEntry(4, COLOR_PARTICLE_WHEAT_2);

        colorEntry(5, COLOR_PARTICLE_LOG_1);
        colorEntry(5, COLOR_PARTICLE_LOG_2);
        colorEntry(5, COLOR_PARTICLE_LOG_3);

        colorEntry(6, COLOR_PARTICLE_STICK_1);
        colorEntry(6, COLOR_PARTICLE_STICK_2);

        colorEntry(7, COLOR_PARTICLE_ROCK_1);
        colorEntry(7, COLOR_PARTICLE_ROCK_2);

        colorEntry(8, COLOR_PARTICLE_ROCK_1);
        colorEntry(8, COLOR_PARTICLE_ROCK_2);
        colorEntry(8, COLOR_PARTICLE_COAL_1);
        colorEntry(8, COLOR_PARTICLE_COAL_2);

        colorEntry(9, COLOR_PARTICLE_BLUEBERRYBUSH_1);
        colorEntry(9, COLOR_PARTICLE_BLUEBERRYBUSH_2);

        colorEntry(10, COLOR_PARTICLE_DYNAMIC_ROCK_1);
        colorEntry(10, COLOR_PARTICLE_DYNAMIC_ROCK_2);

        colorEntry(11, COLOR_PARTICLE_DYNAMIC_DIRT_1);
        colorEntry(11, COLOR_PARTICLE_DYNAMIC_DIRT_2);

        colorEntry(12, COLOR_PARTICLE_DYNAMIC_OAK_PLANK_1);
        colorEntry(12, COLOR_PARTICLE_DYNAMIC_OAK_PLANK_2);

        colorEntry(13, COLOR_PARTICLE_BLOOD_1);
        colorEntry(13, COLOR_PARTICLE_BLOOD_2);

        colorEntry(14, COLOR_PARTICLE_SMOKE_1);
        colorEntry(14, COLOR_PARTICLE_SMOKE_2);
        colorEntry(14, COLOR_PARTICLE_SMOKE_3);

        colorEntry(15, COLOR_PARTICLE_DUST_1);
        colorEntry(15, COLOR_PARTICLE_DUST_2);
        colorEntry(15, COLOR_PARTICLE_DUST_3);
        colorEntry(15, COLOR_PARTICLE_DUST_4);

        colorEntry(16, COLOR_PARTICLE_STONE_DUST_1);
        colorEntry(16, COLOR_PARTICLE_STONE_DUST_2);
        colorEntry(16, COLOR_PARTICLE_STONE_DUST_3);

        colorEntry(17, COLOR_PARTICLE_WATER_1);
        colorEntry(17, COLOR_PARTICLE_WATER_2);
        colorEntry(17, COLOR_PARTICLE_WATER_3);
    }

    public static List<Color> of(int id) {
        return COLOR_MAP.get(id);
    }

    public static Color random(int id) {
        int size = COLOR_MAP.get(id).size();
        return COLOR_MAP.get(id).get(MathUtils.random(0, size - 1));
    }

    public static void colorEntry(int id, Color color) {
        if(!COLOR_MAP.containsKey(id)) {
            COLOR_MAP.put(id, new LinkedList<>());
        }

        COLOR_MAP.get(id).add(color);
    }

}
