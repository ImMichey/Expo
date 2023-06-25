package dev.michey.expo.server.main.logic.world.gen;

public class NoisePostProcessor {

    public NoiseWrapper noiseWrapper;
    public PostProcessorLogic postProcessorLogic;
    public int priority;

    public NoisePostProcessor() {
        // KryoNet
    }

    public NoisePostProcessor(int priority, NoiseWrapper noiseWrapper, PostProcessorLogic postProcessorLogic) {
        this.priority = priority;
        this.noiseWrapper = noiseWrapper;
        this.postProcessorLogic = postProcessorLogic;
    }

    @Override
    public String toString() {
        return "NoisePostProcessor{" +
                "noiseWrapper=" + noiseWrapper +
                ", postProcessorLogic=" + postProcessorLogic +
                ", priority=" + priority +
                '}';
    }

}