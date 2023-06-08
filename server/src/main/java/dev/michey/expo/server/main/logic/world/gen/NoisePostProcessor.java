package dev.michey.expo.server.main.logic.world.gen;

public class NoisePostProcessor {

    public NoiseWrapper noiseWrapper;
    public float threshold;

    public NoisePostProcessor() {
        // KryoNet
    }

    public NoisePostProcessor(NoiseWrapper noiseWrapper, float threshold) {
        this.noiseWrapper = noiseWrapper;
        this.threshold = threshold;
    }

    @Override
    public String toString() {
        return "NoisePostProcessor{" +
                "noiseWrapper=" + noiseWrapper +
                ", threshold=" + threshold +
                '}';
    }

}
