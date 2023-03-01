package dev.michey.expo.util;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.render.RenderContext;

public class InputUtils {

    /** Returns the x pos of the mouse on the screen from -1 to 1. */
    public static float getMouseOnScreenXNegPos() {
        float center = Gdx.graphics.getWidth() * 0.5f;
        float val = Gdx.input.getX();

        if(val >= center) {
            // Right side.
            return (val - center) / center;
        } else {
            // Left side.
            return -(1 - (val / center));
        }
    }

    /** Returns the y pos of the mouse on the screen from -1 to 1. */
    public static float getMouseOnScreenYNegPos() {
        float center = Gdx.graphics.getHeight() * 0.5f;
        float val = Gdx.input.getY();

        if(val >= center) {
            // Lower half.
            return -((val - center) / center);
        } else {
            // Upper half.
            return (1 - (val / center));
        }
    }

    public static float getMouseWorldX() {
        return getMouseWorldX(Gdx.input.getX());
    }

    public static float getMouseWorldX(float mx) {
        RenderContext rc = RenderContext.get();
        float cx = rc.expoCamera.camera.position.x;
        float vw = rc.expoCamera.camera.viewportWidth; // 1280 -> window width
        float cz = rc.expoCamera.camera.zoom;
        return cx - vw * 0.5f * cz + mx * cz;
    }

    public static float getMouseWorldY() {
        return getMouseWorldY(Math.abs(Gdx.input.getY() - Gdx.graphics.getHeight()));
    }

    public static float getMouseWorldY(float my) {
        RenderContext rc = RenderContext.get();
        float cy = rc.expoCamera.camera.position.y;
        float vh = rc.expoCamera.camera.viewportHeight; // 720 -> window height
        float cz = rc.expoCamera.camera.zoom;
        return cy - vh * 0.5f * cz + my * cz;
    }

}
