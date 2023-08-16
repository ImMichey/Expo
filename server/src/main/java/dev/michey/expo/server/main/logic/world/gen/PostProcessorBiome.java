package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

        LinkedList<BiomeType> biomeList = new LinkedList<>();

        if(replacementKeys.length > 0 && replacementKeys[0].equals("*")) {
            biomeList.addAll(Arrays.asList(BiomeType.values()));
        }

        for(String rk : replacementKeys) {
            if(rk.equals("*")) continue;
            if(rk.startsWith("!")) {
                biomeList.remove(BiomeType.valueOf(rk.substring(1)));
            } else {
                biomeList.add(BiomeType.valueOf(rk));
            }
        }

        BiomeType[] array = new BiomeType[biomeList.size()];
        for(int i = 0; i < array.length; i++) array[i] = biomeList.get(i);
        this.checkBiomes =  array;
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