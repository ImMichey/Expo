package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;

public interface PostProcessorLogic {

    BiomeType getBiome(BiomeType existingBiome, float noiseValue);

    TileLayerType getLayerType(TileLayerType existingLayerType, float noiseValue);

}