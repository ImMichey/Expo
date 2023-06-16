package dev.michey.expo.server.main.logic.world.gen;

import make.some.noise.Noise;

public class NoiseWrapper {

    public String name;
    public int octaves;
    public int type;
    public int fractalType;
    public float frequency;
    public int seedOffset;

    public NoiseWrapper() {
        // KryoNet
    }

    public NoiseWrapper(String name, int octaves, int type, int fractalType, float frequency, int seedOffset) {
        this.name = name;
        this.octaves = octaves;
        this.type = type;
        this.fractalType = fractalType;
        this.frequency = frequency;
        this.seedOffset = seedOffset;
    }

    public void applyTo(Noise noise) {
        noise.setFrequency(frequency);
        if(fractalType != -1) noise.setFractalType(fractalType);
        noise.setNoiseType(type);
        noise.setFractalOctaves(octaves);
        noise.setSeed(noise.getSeed() + seedOffset);
    }

    @Override
    public String toString() {
        return "NoiseWrapper{" +
                "name='" + name + '\'' +
                ", octaves=" + octaves +
                ", type=" + type +
                ", fractalType=" + fractalType +
                ", frequency=" + frequency +
                ", seedOffset=" + seedOffset +
                '}';
    }

}
