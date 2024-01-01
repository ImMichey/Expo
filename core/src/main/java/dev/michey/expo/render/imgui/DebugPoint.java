package dev.michey.expo.render.imgui;

import com.badlogic.gdx.graphics.Color;

public class DebugPoint {

    public float x;
    public float y;
    public Color color;
    public String marker;

    public DebugPoint(float x, float y, Color color, String marker) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.marker = marker;
    }

    public DebugPoint(float x, float y) {
        this(x, y, Color.WHITE, null);
    }

    public DebugPoint(float x, float y, String marker) {
        this(x, y, Color.WHITE, marker);
    }

}