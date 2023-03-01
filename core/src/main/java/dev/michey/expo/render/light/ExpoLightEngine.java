package dev.michey.expo.render.light;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.light.box2dport.RayHandler;
import dev.michey.expo.render.light.box2dport.RayHandlerOptions;

public class ExpoLightEngine {

    /** Box2D light engine port */
    public RayHandler rayHandler;

    /** The light FBO quality (1 = best, 4 = worst) */
    public int LIGHT_QUALITY = 4;

    public ExpoLightEngine() {
        RayHandlerOptions options = new RayHandlerOptions();
        options.setDiffuse(true);
        options.setGammaCorrection(false);

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
