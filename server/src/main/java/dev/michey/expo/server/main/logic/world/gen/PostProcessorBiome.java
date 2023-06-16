package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;

import java.util.Arrays;

public class PostProcessorBiome implements PostProcessorLogic {

    public String noiseName;
    public BiomeType[] checkBiomes;
    public BiomeType replacementType;
    public float threshold;

    // Optional
    public float secondOptionThreshold;
    public BiomeType secondOptionType;

    public PostProcessorBiome() {
        // KryoNet
    }

    public PostProcessorBiome(String noiseName, float threshold, String[] replacementKeys, String replaceWith, float thresholdSecond, String thresholdReplace) {
        this.noiseName = noiseName;
        this.threshold = threshold;

        boolean listAll = replacementKeys.length > 0 && replacementKeys[0].equals("*");
        BiomeType[] all = BiomeType.values();
        BiomeType[] bt = new BiomeType[listAll ? all.length : replacementKeys.length];

        if(listAll) {
            System.arraycopy(BiomeType.values(), 0, bt, 0, all.length);
        } else {
            for(int i = 0; i < replacementKeys.length; i++) {
                bt[i] = BiomeType.valueOf(replacementKeys[i]);
            }
        }

        this.checkBiomes = bt;
        this.replacementType = BiomeType.valueOf(replaceWith);

        this.secondOptionThreshold = thresholdSecond;
        if(this.secondOptionThreshold != -1.0f) secondOptionType = BiomeType.valueOf(thresholdReplace);
    }

    @Override
    public BiomeType getBiome(BiomeType existingBiome, float noiseValue) {
        if(noiseValue < threshold) return null;

        for(BiomeType check : checkBiomes) {
            if(check == existingBiome) {
                if(secondOptionType != null && noiseValue >= secondOptionThreshold) return secondOptionType;
                return replacementType;
            }
        }

        return null;
    }

    @Override
    public TileLayerType getLayerType(TileLayerType existingLayerType, float noiseValue) {
        return null;
    }

    @Override
    public String toString() {
        return "PostProcessorBiome{" +
                "checkBiomes=" + Arrays.toString(checkBiomes) +
                ", replacementType=" + replacementType +
                ", secondOptionThreshold=" + secondOptionThreshold +
                ", secondOptionType=" + secondOptionType +
                '}';
    }

}