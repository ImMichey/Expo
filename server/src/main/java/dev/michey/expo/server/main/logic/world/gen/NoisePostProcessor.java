package dev.michey.expo.server.main.logic.world.gen;

public class NoisePostProcessor {

    public NoiseWrapper noiseWrapper;
    public PostProcessorLogic postProcessorLogic;

    public NoisePostProcessor() {
        // KryoNet
    }

    public NoisePostProcessor(NoiseWrapper noiseWrapper, PostProcessorLogic postProcessorLogic) {
        this.noiseWrapper = noiseWrapper;
        this.postProcessorLogic = postProcessorLogic;
    }

    @Override
    public String toString() {
        return "NoisePostProcessor{" +
                "noiseWrapper=" + noiseWrapper +
                ", postProcessorLogic=" + postProcessorLogic +
                '}';
    }

}