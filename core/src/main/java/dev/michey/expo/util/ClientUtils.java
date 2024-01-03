package dev.michey.expo.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.render.RenderContext;

import java.io.File;
import java.util.zip.Deflater;

import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public class ClientUtils {

    public static void log(String message, int keycode) {
        if(Gdx.input.isKeyJustPressed(keycode)) {
            ExpoLogger.log(message);
        }
    }

    public static void logf(String message, int keycode) {
        if(Gdx.input.isKeyPressed(keycode)) {
            ExpoLogger.log(message);
        }
    }

    public static Vector2 entityPosToHudPos(float x, float y) {
        OrthographicCamera c = RenderContext.get().expoCamera.camera;
        float endX = c.position.x + (c.viewportWidth * c.zoom);
        float endY = c.position.y + (c.viewportHeight * c.zoom);

        float progX = (x - c.position.x) / (endX - c.position.x) + 0.5f;
        float absoluteX = progX * Gdx.graphics.getWidth();

        float progY = (y - c.position.y) / (endY - c.position.y) + 0.5f;
        float absoluteY = progY * Gdx.graphics.getHeight();

        return new Vector2(absoluteX, absoluteY);
    }

    public static void takeScreenshot(String name) {
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());

        new Thread(() -> {
            PixmapIO.writePNG(Gdx.files.absolute(ExpoLogger.getLocalPath() + File.separator + "screenshots/" + name + ".png"), pixmap, Deflater.DEFAULT_COMPRESSION, true);
            pixmap.dispose();
        }).start();
    }

    public static void takeScreenshot(String name, int onKeyCode) {
        if(Gdx.input.isKeyJustPressed(onKeyCode) && DEV_MODE) {
            takeScreenshot(name);
        }
    }

    /** Converts the ray amount to a settings-based ray count. */
    public static int rays(int base) {
        int quality = GameSettings.get().lightQuality;
        return base / quality;
    }

    public static Color darker(Color src, float factor) {
        float r = src.r;
        float g = src.g;
        float b = src.b;
        return new Color(r * factor, g * factor, b * factor, src.a);
    }

    public static Color darker(Color src) {
        float factor = 0.975f;
        float r = src.r;
        float g = src.g;
        float b = src.b;
        return new Color(r * factor, g * factor, b * factor, src.a);
    }

}