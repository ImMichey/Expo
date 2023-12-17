package dev.michey.expo.server.main.logic.world.gen;

import org.json.JSONArray;

public class EntityBoundsEntry {

    public float xOffset;
    public float yOffset;
    public float width;
    public float height;

    public EntityBoundsEntry(JSONArray array) {
        this.xOffset = array.getFloat(0);
        this.yOffset = array.getFloat(1);
        this.width = array.getFloat(2);
        this.height = array.getFloat(3);
    }

    /** Returns the two corner points of this entity after their entity dimensions were applied. */
    public float[] toWorld(float x, float y) {
        return new float[] {x + xOffset, y + yOffset, x + xOffset + width, y + yOffset + height};
    }

}