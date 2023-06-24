package dev.michey.expo.render.light;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.light.box2dport.PointLight;

public class ExpoLight {

    /** Direct access light. */
    public PointLight box2dLight;

    public boolean pulsating = false;
    public float pulsatingAlpha = 0f;
    public float pulsatingSpeed = 1f;
    public boolean pulsatingDirection = true;
    public float pulsatingMinDistance;
    public float pulsatingMaxDistance;

    public ExpoLight(float distance, int rays, float constant, float quadratic) {
        box2dLight = new PointLight(RenderContext.get().lightEngine.rayHandler, rays);
        box2dLight.setXray(true);
        box2dLight.setFalloff(constant, 0.0f, quadratic);
        box2dLight.setDistance(distance);
    }

    public ExpoLight(float distance) {
        this(distance, (int) distance, 1.0f, 0f);
    }

    public void update(float x, float y, float delta) {
        box2dLight.setPosition(x, y);

        if(pulsating) {
            if(pulsatingDirection) {
                pulsatingAlpha += delta * pulsatingSpeed;

                if(pulsatingAlpha >= 1.0f) {
                    pulsatingAlpha = 1.0f;
                    pulsatingDirection = false;
                }
            } else {
                pulsatingAlpha -= delta * pulsatingSpeed;

                if(pulsatingAlpha <= 0.0f) {
                    pulsatingAlpha = 0.0f;
                    pulsatingDirection = true;
                }
            }

            box2dLight.setDistance(pulsatingMinDistance + pulsatingMaxDistance * Interpolation.smooth.apply(pulsatingAlpha));
        }
    }

    public void setPulsating(float speed, float minDistance, float maxDistance) {
        pulsating = true;
        pulsatingSpeed = speed;
        pulsatingMinDistance = minDistance;
        pulsatingMaxDistance = maxDistance - minDistance;
        pulsatingAlpha = MathUtils.random();
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
