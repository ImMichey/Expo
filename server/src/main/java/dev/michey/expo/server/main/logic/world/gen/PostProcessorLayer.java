package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;

import java.util.Arrays;

public class PostProcessorLayer implements PostProcessorLogic {

    public String noiseName;
    public TileLayerType[] checkTypes;
    public int processLayer;
    public TileLayerType replacementType;
    public float threshold;

    // Optional
    public float secondOptionThreshold;
    public TileLayerType secondOptionType;

    public PostProcessorLayer() {
        // KryoNet
    }

    public PostProcessorLayer(String noiseName, float threshold, String[] replacementKeys, String replaceType, String replaceWith, float thresholdSecond, String thresholdReplace) {
        this.noiseName = noiseName;
        this.threshold = threshold;

        boolean listAll = replacementKeys.length > 0 && replacementKeys[0].equals("*");
        TileLayerType[] all = TileLayerType.values();
        TileLayerType[] tlt = new TileLayerType[listAll ? all.length : replacementKeys.length];

        if(listAll) {
            System.arraycopy(TileLayerType.values(), 0, tlt, 0, all.length);
        } else {
            for(int i = 0; i < replacementKeys.length; i++) {
                tlt[i] = TileLayerType.valueOf(replacementKeys[i]);
            }
        }

        this.checkTypes = tlt;
        this.replacementType = TileLayerType.valueOf(replaceWith);
        this.processLayer = Integer.parseInt(String.valueOf(replaceType.charAt(replaceType.length() - 1)));

        this.secondOptionThreshold = thresholdSecond;
        if(this.secondOptionThreshold != -1.0f) secondOptionType = TileLayerType.valueOf(thresholdReplace);
    }

    @Override
    public BiomeType getBiome(BiomeType existingBiome, float noiseValue) {
        return null;
    }

    @Override
    public TileLayerType getLayerType(TileLayerType existingLayerType, float noiseValue) {
        if(noiseValue < threshold) return null;

        for(TileLayerType check : checkTypes) {
            if(check == existingLayerType) {
                if(secondOptionType != null && noiseValue >= secondOptionThreshold) return secondOptionType;
                return replacementType;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "PostProcessorLayer{" +
                "noiseName='" + noiseName + '\'' +
                ", checkTypes=" + Arrays.toString(checkTypes) +
                ", processLayer=" + processLayer +
                ", replacementType=" + replacementType +
                ", threshold=" + threshold +
                ", secondOptionThreshold=" + secondOptionThreshold +
                ", secondOptionType=" + secondOptionType +
                '}';
    }

}