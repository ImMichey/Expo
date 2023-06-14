package dev.michey.expo.server.main.logic.world.bbox;

import org.json.JSONArray;

public class BBox {

    public float xOffset;
    public float yOffset;
    public float width;
    public float height;

    public BBox(JSONArray floatArray) {
        this.xOffset = floatArray.getFloat(0);
        this.yOffset = floatArray.getFloat(1);
        this.width = floatArray.getFloat(2);
        this.height = floatArray.getFloat(3);
    }

    public BBox(float xOffset, float yOffset, float width, float height) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
    }

    /** Returns the two corner points of this entity after their entity dimensions were applied. */
    public float[] toWorld(float x, float y) {
        return new float[] {x + xOffset, y + yOffset, x + xOffset + width, y + yOffset + height};
    }

    public float[] toWorldAdvanced(float x, float y) {
        return new float[] {
                x + xOffset, y + yOffset,
                x + xOffset + width, y + xOffset,
                x + xOffset + width, y + yOffset + height,
                x + xOffset, y + yOffset + height
        };
    }

}