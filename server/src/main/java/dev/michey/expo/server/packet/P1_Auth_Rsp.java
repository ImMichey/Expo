package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.world.gen.BiomeDefinition;
import dev.michey.expo.server.main.logic.world.gen.WorldGenNoiseSettings;

import java.util.List;

public class P1_Auth_Rsp extends Packet {

    public boolean authSuccessful;
    public String authMessage;

    public int serverTps;
    public int worldSeed;
    public WorldGenNoiseSettings noiseSettings;
    public List<BiomeDefinition> biomeDefinitionList;

}
