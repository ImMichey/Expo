package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;

public class GenerationTile {

    public BiomeType type;
    public boolean index;
    public float wx, wy;

    public GenerationTile(BiomeType type, boolean index, float wx, float wy) {
        this.type = type;
        this.index = index;
        this.wx = wx;
        this.wy = wy;
    }

}
