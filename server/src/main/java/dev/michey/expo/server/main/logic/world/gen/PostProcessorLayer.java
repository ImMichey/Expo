package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.util.Pair;

import java.util.Arrays;

public class PostProcessorLayer implements PostProcessorLogic {

    public String noiseName;
    public TileLayerType[] checkTypes;
    public int processLayer;
    public int processLayerSecond;
    public TileLayerType replacementType;
    public float thresholdA;
    public float thresholdB;

    // Optional
    public float secondOptionThreshold;
    public TileLayerType secondOptionType;

    public PostProcessorLayer() {
        // KryoNet
    }

    public PostProcessorLayer(String noiseName, float thresholdA, float thresholdB, String[] replacementKeys,
                              String replaceType, String replaceWith, float thresholdSecond, String thresholdReplace, String replaceTypeSecond) {
        this.noiseName = noiseName;
        this.thresholdA = thresholdA;
        this.thresholdB = thresholdB;

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
        this.processLayerSecond = Integer.parseInt(String.valueOf(replaceTypeSecond.charAt(replaceTypeSecond.length() - 1)));

        this.secondOptionThreshold = thresholdSecond;
        if(this.secondOptionThreshold != -1.0f) secondOptionType = TileLayerType.valueOf(thresholdReplace);
    }

    @Override
    public BiomeType getBiome(BiomeType existingBiome, float noiseValue) {
        return null;
    }

    @Override
    public Pair<TileLayerType, Integer> getLayerType(TileLayerType existingLayerType, float noiseValue) {
        if(noiseValue < thresholdA) return null;
        if(noiseValue > thresholdB) return null;

        for(TileLayerType check : checkTypes) {
            if(check == existingLayerType) {
                if(secondOptionType != null && noiseValue >= secondOptionThreshold) {
                    return new Pair<>(secondOptionType, processLayerSecond);
                }
                return new Pair<>(replacementType, processLayer);
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
                ", thresholdA=" + thresholdA +
                ", thresholdB=" + thresholdB +
                ", secondOptionThreshold=" + secondOptionThreshold +
                ", secondOptionType=" + secondOptionType +
                '}';
    }

}