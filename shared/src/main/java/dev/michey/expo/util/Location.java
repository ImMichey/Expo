package dev.michey.expo.util;

public class Location {

    public String dimension;
    public float x;
    public float y;

    public Location(String dimension, float x, float y) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
    }

    public Location() {
        this.dimension = ExpoShared.DIMENSION_OVERWORLD;
        this.x = 0f;
        this.y = 0f;
    }

}
