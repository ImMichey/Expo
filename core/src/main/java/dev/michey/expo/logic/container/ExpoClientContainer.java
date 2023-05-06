package dev.michey.expo.logic.container;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.michey.expo.client.ExpoClient;
import dev.michey.expo.client.ExpoClientPacketReader;
import dev.michey.expo.input.IngameInput;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.ClientStatic;

import java.util.concurrent.ConcurrentHashMap;

public class ExpoClientContainer {

    /** Singleton */
    private static ExpoClientContainer INSTANCE;

    /** Local server communication */
    private ExpoServerLocal localServer;
    private ExpoClientPacketReader packetReader;

    /** Dedicated server communication */
    private ExpoClient client;

    /** The game world */
    private ClientWorld clientWorld;
    private int serverTickRate;

    /** User input */
    private IngameInput input;

    /** Player UI */
    private PlayerUI playerUI;
    private ConcurrentHashMap<String, Integer> playerOnlineList;

    public ExpoClientContainer(ExpoServerLocal localServer) {
        this.localServer = localServer;
        packetReader = new ExpoClientPacketReader();
        init();
    }

    public ExpoClientContainer(ExpoClient client) {
        this.client = client;
        init();
    }

    public void notifyPlayerJoin(String username) {
        playerOnlineList.put(username, 0);
        playerUI.onPlayerJoin(username);
    }

    public void notifyPlayerQuit(String username) {
        playerOnlineList.remove(username);
        playerUI.onPlayerQuit(username);
    }

    private void init() {
        input = new IngameInput();
        clientWorld = new ClientWorld();
        playerUI = new PlayerUI();
        playerOnlineList = new ConcurrentHashMap<>();
        INSTANCE = this;
    }

    public void sendPacketTcp(Packet packet) {
        if(client != null) {
            client.sendPacketTcp(packet);
        } else {
            localServer.consumePacket(packet);
        }
    }

    public void sendPacketUdp(Packet packet) {
        if(client != null) {
            client.sendPacketUdp(packet);
        } else {
            localServer.consumePacket(packet);
        }
    }

    public void render() {
        float d = RenderContext.get().delta;
        float serverDelta = 1f / (float) serverTickRate;
        clientWorld.tickWorld(d, serverDelta);
        clientWorld.renderWorld();
        playerUI.update();

        if(RenderContext.get().drawHUD) {
            playerUI.render();

            RenderContext r = RenderContext.get();
            BitmapFont useFont = r.m5x7_border_all[0];

            String version = "Expo v" + ClientStatic.GAME_VERSION;
            playerUI.glyphLayout.setText(useFont, version);
            float w = playerUI.glyphLayout.width;

            String fps = "FPS: " + Gdx.graphics.getFramesPerSecond();
            playerUI.glyphLayout.setText(useFont, fps);
            float w2 = playerUI.glyphLayout.width;

            float h = playerUI.glyphLayout.height;
            float spacing = 4;

            r.hudBatch.begin();
            useFont.draw(r.hudBatch, version, Gdx.graphics.getWidth() - w - spacing, Gdx.graphics.getHeight() - spacing);
            useFont.draw(r.hudBatch, fps, Gdx.graphics.getWidth() - w2 - spacing, Gdx.graphics.getHeight() - h - spacing * 2);
            r.hudBatch.end();
        }

        if(client != null) {
            client.getPacketListener().evaluatePackets();
        }
    }

    public void stopSession() {
        if(client != null) {
            client.disconnect();
        } else {
            localServer.stopServer();
        }
    }

    public ExpoClient getClient() {
        return client;
    }

    public IngameInput getInput() {
        return input;
    }

    public ExpoClientPacketReader getPacketReader() {
        return packetReader;
    }

    public void setServerTickRate(int serverTickRate) {
        this.serverTickRate = serverTickRate;
    }

    public int getServerTickRate() {
        return serverTickRate;
    }

    public ClientWorld getClientWorld() {
        return clientWorld;
    }

    public PlayerUI getPlayerUI() {
        return playerUI;
    }

    public ConcurrentHashMap<String, Integer> getPlayerOnlineList() {
        return playerOnlineList;
    }

    public static ExpoClientContainer get() {
        return INSTANCE;
    }

}
