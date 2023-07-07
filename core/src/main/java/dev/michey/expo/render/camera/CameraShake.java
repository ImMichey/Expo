package dev.michey.expo.render.camera;

import com.badlogic.gdx.math.Vector2;

import java.util.Random;

public class CameraShake {

    private static float time = 0;
    private static float currentTime = 1;
    private static float power = 0;
    private static float currentPower = 0;
    private static Random random;

    private static Vector2 OLD_POS = new Vector2();
    private static Vector2 CURRENT_POS = new Vector2();

    public static void invoke(float rumblePower, float rumbleLength) {
        random = new Random();
        power = rumblePower;
        time = rumbleLength;
        currentTime = 0;
    }

    public static void tick(float delta) {
        if(currentTime <= time) {
            OLD_POS.set(CURRENT_POS);
            currentPower = power * ((time - currentTime) / time);

            CURRENT_POS.x = (random.nextFloat() - 0.5f) * 2 * currentPower;
            CURRENT_POS.y = (random.nextFloat() - 0.5f) * 2 * currentPower;

            currentTime += delta;
        } else {
            if(time != 0) {
                time = 0;
                OLD_POS.set(CURRENT_POS);
                CURRENT_POS.set(0, 0);
            } else {
                OLD_POS.set(0, 0);
            }
        }
    }

    public static Vector2 getCurrentPos() {
        return CURRENT_POS;
    }

    public static Vector2 getOldPos() {
        return OLD_POS;
    }

}
