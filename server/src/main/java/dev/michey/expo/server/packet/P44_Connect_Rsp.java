package dev.michey.expo.server.packet;

public class P44_Connect_Rsp extends Packet {

    public boolean credentialsSuccessful;
    public boolean requiresSteamTicket;
    public String message;

}
