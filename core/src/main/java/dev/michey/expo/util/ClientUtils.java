package dev.michey.expo.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.log.ExpoLogger;

public class ClientUtils {

    public static void log(String message, int keycode) {
        if(Gdx.input.isKeyJustPressed(keycode)) {
            ExpoLogger.log(message);
        }
    }

    public static float getWind(float u_strength, float u_time, float u_offset, float u_speed) {
        float time = u_time * u_speed + u_offset;
        double diff = Math.pow(-u_strength, 2.0);

        double aa = u_strength + diff + MathUtils.sin(time) * diff;


        double strength = MathUtils.clamp(aa, 0.0, u_strength);

        // float strength = clamp(u_strength + diff + sin(time) * diff, u_strength, 0.0) * 100.0;
        log(time + ", " + aa + ", " + diff + ", " + strength, Input.Keys.T);
        double wind = (MathUtils.sin(time) + MathUtils.cos(time)) * aa;
        return (float) wind;
    }

}
