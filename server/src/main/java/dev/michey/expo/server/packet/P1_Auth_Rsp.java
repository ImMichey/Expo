package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.world.gen.WorldGenSettings;

public class P1_Auth_Rsp extends Packet {

    public boolean authorized;
    public String message;
    public int serverTps;

    public int worldSeed;

    public WorldGenSettings genSettings;

}
