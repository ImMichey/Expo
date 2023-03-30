package dev.michey.expo.util;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.log.ExpoLogger;

public class ClientUtils {

    public static void log(String message, int keycode) {
        if(Gdx.input.isKeyJustPressed(keycode)) {
            ExpoLogger.log(message);
        }
    }

}
