package dev.michey.expo.server.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class GenerationUtils {

    public static Vector2 circular(float angle, float radius) {
        return new Vector2(
                MathUtils.cosDeg(angle) * radius,
                MathUtils.sinDeg(angle) * radius
        );
    }

    public static Vector2 circular(float angle, float radiusX, float radiusY) {
        return new Vector2(
                MathUtils.cosDeg(angle) * radiusX,
                MathUtils.sinDeg(angle) * radiusY
        );
    }

    public static Vector2[] positions(int amount, float radius) {
        float anglePer = 360.0f / amount;
        Vector2[] positions = new Vector2[amount];

        for(int i = 0; i < amount; i++) {
            positions[i] = circular(anglePer * i, radius);
        }

        return positions;
    }

    public static Vector2[] positions(int amount, float radiusMin, float radiusMax) {
        float anglePer = 360.0f / amount;
        Vector2[] positions = new Vector2[amount];

        for(int i = 0; i < amount; i++) {
            positions[i] = circular(anglePer * i, MathUtils.random(radiusMin, radiusMax));
        }

        return positions;
    }

}
