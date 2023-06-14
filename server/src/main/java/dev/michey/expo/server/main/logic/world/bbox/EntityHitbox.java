package dev.michey.expo.server.main.logic.world.bbox;

import dev.michey.expo.util.ExpoShared;
import org.json.JSONArray;

import java.awt.image.renderable.RenderContext;

public class EntityHitbox extends BBox {

    public EntityHitbox(JSONArray floatArray) {
        super(floatArray);
    }

    public boolean intersecting(float entityX, float entityY, float x, float y) {
        float[] cornerPoints = toWorld(entityX, entityY);
        return x >= cornerPoints[0] && y >= cornerPoints[1] && x <= cornerPoints[2] && y <= cornerPoints[3];
    }

}