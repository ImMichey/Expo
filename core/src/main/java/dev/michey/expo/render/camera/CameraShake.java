package dev.michey.expo.render.camera;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.util.ExpoShared;

import java.util.Random;

public class CameraShake {

    private static float time = 0;
    private static float currentTime = 1;
    private static float power = 0;
    private static float currentPower = 0;
    private static Random random;
    private static Vector2 position = null;

    private static Vector2 OLD_POS = new Vector2();
    private static Vector2 CURRENT_POS = new Vector2();

    public static void invoke(float rumblePower, float rumbleLength) {
        random = new Random();
        power = rumblePower;
        time = rumbleLength;
        currentTime = 0;
        position = null;
    }

    public static void invoke(float rumblePower, float rumbleLength, Vector2 origin) {
        random = new Random();
        power = rumblePower;
        time = rumbleLength;
        currentTime = 0;
        position = origin;
    }

    public static void tick(float delta) {
        if(currentTime <= time) {
            OLD_POS.set(CURRENT_POS);
            currentPower = power * ((time - currentTime) / time);

            float multiplier = 1.0f;

            if(position != null && ClientPlayer.getLocalPlayer() != null) {
                float dst = position.dst(ClientPlayer.getLocalPlayer().clientPosX, ClientPlayer.getLocalPlayer().clientPosY);
                float mr = ExpoShared.PLAYER_AUDIO_RANGE * 2;

                if(dst > mr) {
                    multiplier = 0.0f;
                } else {
                    multiplier = 1f - dst / mr;
                }
            }

            CURRENT_POS.x = (random.nextFloat() - 0.5f) * 2 * currentPower * multiplier;
            CURRENT_POS.y = (random.nextFloat() - 0.5f) * 2 * currentPower * multiplier;

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
