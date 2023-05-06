package dev.michey.expo.server.packet;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.world.gen.WorldGenNoiseSettings;

import java.util.HashMap;

public class P1_Auth_Rsp extends Packet {

    public boolean authorized;
    public String message;
    public int serverTps;

    public int worldSeed;

    public WorldGenNoiseSettings noiseSettings;
    public HashMap<BiomeType, float[]> biomeDataMap;

}
