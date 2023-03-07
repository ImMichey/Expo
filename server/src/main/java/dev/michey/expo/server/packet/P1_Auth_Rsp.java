package dev.michey.expo.server.packet;

public class P1_Auth_Rsp extends Packet {

    public boolean authorized;
    public String message;
    public int serverTps;
    public int worldSeed;

}
