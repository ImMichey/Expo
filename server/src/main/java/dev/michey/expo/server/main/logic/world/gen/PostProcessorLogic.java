package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.util.Pair;

public interface PostProcessorLogic {

    BiomeType getBiome(BiomeType existingBiome, float noiseValue);

    Pair<TileLayerType, Integer> getLayerType(TileLayerType existingLayerType, float noiseValue);

}