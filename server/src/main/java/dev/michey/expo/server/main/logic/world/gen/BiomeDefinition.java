package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;

public class BiomeDefinition {

    public BiomeType biomeType;
    public float[] noiseBoundaries;
    public int priority;

    public BiomeDefinition(BiomeType biomeType, float[] noiseBoundaries, int priority) {
        this.biomeType = biomeType;
        this.noiseBoundaries = noiseBoundaries;
        this.priority = priority;
    }

}
