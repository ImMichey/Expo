package dev.michey.expo.server.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class GenerationUtils {

    public static Vector2 circularRandom(float radius) {
        return circular(MathUtils.random(360f), radius);
    }

    public static Vector2 circular(float angle, float radius) {
        return new Vector2(
                MathUtils.cosDeg(angle) * radius,
                MathUtils.sinDeg(angle) * radius
        );
    }

    public static Vector2[] positions(int amount, float radius) {
        float anglePer = 360.0f / amount;
        Vector2[] positions = new Vector2[amount];
        float randomOffset = MathUtils.random(360f);

        for(int i = 0; i < amount; i++) {
            positions[i] = circular(anglePer * i + randomOffset, radius);
        }

        return positions;
    }

    public static Vector2[] positions(int amount, float radiusMin, float radiusMax) {
        float anglePer = 360.0f / amount;
        Vector2[] positions = new Vector2[amount];
        float randomOffset = MathUtils.random(360f);

        for(int i = 0; i < amount; i++) {
            positions[i] = circular(anglePer * i + randomOffset, MathUtils.random(radiusMin, radiusMax));
        }

        return positions;
    }

    public static double angleBetween(float x, float y, float x2, float y2) {
        return Math.toDegrees(Math.atan2(y2 - y, x2 - x));
    }

    public static double angleBetween360(float x, float y, float x2, float y2) {
        double deg = Math.toDegrees(Math.atan2(y2 - y, x2 - x));

        if(deg < 0) {
            return 360d - Math.abs(deg);
        }

        return deg;
    }

}
