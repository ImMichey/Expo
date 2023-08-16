package dev.michey.expo.render.light;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.light.box2dport.RayHandler;
import dev.michey.expo.render.light.box2dport.RayHandlerOptions;
import dev.michey.expo.util.GameSettings;

public class ExpoLightEngine {

    /** Box2D light engine port */
    public RayHandler rayHandler;

    /** The light FBO quality (1 = best, 4 = worst) */
    public int LIGHT_QUALITY = 2;

    /** Debug */
    public static float CONSTANT_LIGHT_VALUE = 0.5f;
    public static float LINEAR_LIGHT_VALUE = 0.5f;
    public static float QUADRATIC_LIGHT_VALUE = 0.5f;
    public static float DISTANCE_LIGHT_VALUE = 128.0f;
    public static Color COLOR_LIGHT_VALUE = new Color(1.0f, 1.0f, 0.0f, 1.0f);

    public ExpoLightEngine() {
        RayHandlerOptions options = new RayHandlerOptions();
        options.setDiffuse(true);
        options.setGammaCorrection(false);

        LIGHT_QUALITY = GameSettings.get().lightQuality;
        rayHandler = new RayHandler(null, Gdx.graphics.getWidth() / LIGHT_QUALITY, Gdx.graphics.getHeight() / LIGHT_QUALITY, options);
    }

    /** Updates the underlying shaders and frame buffers. */
    public void resize(int w, int h) {
        rayHandler.resizeFBO(w / LIGHT_QUALITY, h / LIGHT_QUALITY);
    }

    /** Updates the light engine's viewport. */
    public void updateCamera() {
        rayHandler.setCombinedMatrix(RenderContext.get().expoCamera.camera);
    }

    public void setLighting(float r, float g, float b, float brightness) {
        rayHandler.setAmbientLight(r * brightness, g * brightness, b * brightness, 1f);
    }

    public void render() {
        rayHandler.updateAndRender();
    }

}
