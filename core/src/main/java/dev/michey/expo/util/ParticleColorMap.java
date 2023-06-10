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

    public static final Color COLOR_PARTICLE_MUSHROOM_1 = Color.valueOf("#f5ddd1");
    public static final Color COLOR_PARTICLE_MUSHROOM_2 = Color.valueOf("#e1ccc2");

    public static final Color COLOR_PARTICLE_SAND_1 = Color.valueOf("#fffee8");
    public static final Color COLOR_PARTICLE_SAND_2 = Color.valueOf("#ffffec");

    public static final Color COLOR_PARTICLE_WHEAT_1 = Color.valueOf("#e6cb7d");
    public static final Color COLOR_PARTICLE_WHEAT_2 = Color.valueOf("#ffee91");

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
