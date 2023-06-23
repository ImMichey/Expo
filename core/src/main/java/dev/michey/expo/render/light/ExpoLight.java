package dev.michey.expo.render.light;

import com.badlogic.gdx.graphics.Color;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.light.box2dport.PointLight;

public class ExpoLight {

    /** Direct access light. */
    public PointLight box2dLight;

    public ExpoLight(float distance, int rays, float constant, float quadratic) {
        box2dLight = new PointLight(RenderContext.get().lightEngine.rayHandler, rays);
        box2dLight.setXray(true);
        box2dLight.setFalloff(constant, 0.0f, quadratic);
        box2dLight.setDistance(distance);
    }

    public ExpoLight(float distance) {
        this(distance, (int) distance, 1.0f, 0f);
    }

    public void update(float x, float y) {
        box2dLight.setPosition(x, y);
        /* // DEBUG
        box2dLight.setFalloff(ExpoLightEngine.CONSTANT_LIGHT_VALUE, ExpoLightEngine.LINEAR_LIGHT_VALUE, ExpoLightEngine.QUADRATIC_LIGHT_VALUE);
        box2dLight.setDistance(ExpoLightEngine.DISTANCE_LIGHT_VALUE);
        color(ExpoLightEngine.COLOR_LIGHT_VALUE);
         */
    }

    public void color(Color color) {
        box2dLight.setColor(color);
    }

    public void color(float r, float g, float b, float a) {
        box2dLight.setColor(r, g, b, a);
    }

    public void colorAlpha(float a) {
        box2dLight.setColor(box2dLight.getColor().r, box2dLight.getColor().g, box2dLight.getColor().b, a);
    }

    public void delete() {
        box2dLight.remove();
    }

}
