package dev.michey.expo.logic.container;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.client.ExpoClient;
import dev.michey.expo.client.ExpoClientPacketEvaluator;
import dev.michey.expo.client.ExpoClientPacketReader;
import dev.michey.expo.input.IngameInput;
import dev.michey.expo.lang.Lang;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ExpoClientContainer {

    /** Singleton */
    private static ExpoClientContainer INSTANCE;

    /** Local server communication */
    private ExpoServerLocal localServer;
    private ExpoClientPacketReader packetReader;
    private ExpoClientPacketEvaluator packetEvaluator;

    /** Dedicated server communication */
    private ExpoClient client;
    private String loadingMessage = Lang.str("ui.multiplayer.initializing");

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
        packetReader = new ExpoClientPacketReader();
        packetEvaluator = new ExpoClientPacketEvaluator(packetReader);

        INSTANCE = this;

        if(client != null && client.drainAfterInitQueue != null) {
            while(!client.drainAfterInitQueue.isEmpty()) {
                var packet = client.drainAfterInitQueue.poll();
                packetReader.handlePacket(packet, false);
            }

            client.drainAfterInitQueue = null;
        }
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

    private String inOutString = "";

    public void render() {
        float d = RenderContext.get().delta;
        float serverDelta = 1f / (float) serverTickRate;
        clientWorld.tickWorld(d, serverDelta);
        playerUI.update();

        if(client != null) {
            long now = System.currentTimeMillis();

            if(now - client.lastBytesUpdate >= 1000) {
                client.lastBytesUpdate = now;

                int inTraffic = client.bytesInTcp + client.bytesInUdp;
                int outTraffic = client.bytesOutTcp + client.bytesOutUdp;

                client.bytesInTcp = 0;
                client.bytesInUdp = 0;
                client.bytesOutTcp = 0;
                client.bytesOutUdp = 0;

                inOutString = "In/Out data/s: [CYAN]" + readableFileSize(inTraffic) + "[WHITE]/[CYAN]" + readableFileSize(outTraffic);
            }
        }

        if(RenderContext.get().drawHUD) {
            // Draw shadows to shadow FBO.
            RenderContext.get().hudFbo.begin();
            clientWorld.transparentScreen();

            RenderContext r = RenderContext.get();
            r.hudBatch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

            if(playerUI.loadingScreen) {
                // World loading hook
                r.hudBatch.begin();
                r.hudBatch.setColor(Color.BLACK);
                r.hudBatch.draw(playerUI.whiteSquare, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                r.hudBatch.setColor(Color.WHITE);

                String string;

                if(ExpoServerLocal.get() != null) {
                    string = Lang.str("ui.world.loading", ExpoServerLocal.get().getWorldSaveHandler().getWorldName());
                } else {
                    string = loadingMessage;
                }

                r.globalGlyph.setText(r.m6x11_border_use, string);
                float w = r.globalGlyph.width;
                float h = r.globalGlyph.height;

                r.m6x11_border_use.draw(r.hudBatch, string, (Gdx.graphics.getWidth() - w) * 0.5f, (Gdx.graphics.getHeight() - h) * 0.5f + h);

                r.hudBatch.end();
            } else {
                playerUI.render();
            }

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
            if(ExpoServerBase.get() == null) {
                playerUI.glyphLayout.setText(useFont, inOutString);
                float w3 = playerUI.glyphLayout.width;
                useFont.draw(r.hudBatch, inOutString, Gdx.graphics.getWidth() - w3 - spacing, Gdx.graphics.getHeight() - h * 2 - spacing * 3);
            }
            if(ClientPlayer.getLocalPlayer() != null) {
                float x = ClientPlayer.getLocalPlayer().clientPosX;
                float y = ClientPlayer.getLocalPlayer().clientPosY;

                String pos = "Pos [CYAN][" + String.format(Locale.US, "%.2f", x) + " " + String.format(Locale.US, "%.2f", y) + "] [YELLOW][" +
                        ExpoShared.posToChunk(x) + " " + ExpoShared.posToChunk(y) + "]";
                playerUI.glyphLayout.setText(useFont, pos);
                float w3 = playerUI.glyphLayout.width;
                useFont.draw(r.hudBatch, pos, Gdx.graphics.getWidth() - w3 - spacing, Gdx.graphics.getHeight() - h * 3 - spacing * 4);
            }
            long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            String mem = readableFileSize(memory) + "/" + readableFileSize(Runtime.getRuntime().totalMemory()) + " Memory usage";
            playerUI.glyphLayout.setText(useFont, mem);
            useFont.draw(r.hudBatch, mem, Gdx.graphics.getWidth() - playerUI.glyphLayout.width - spacing, Gdx.graphics.getHeight() - h * 4 - spacing * 5);
            r.hudBatch.end();

            RenderContext.get().hudFbo.end();

            r.hudBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); //default blend mode
        }

        clientWorld.renderWorld();
        packetEvaluator.handlePackets();
    }

    public String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public void stopSession() {
        ExpoClientContainer.get().clientWorld.getClientChunkGrid().executorService.shutdown();

        if(client != null) {
            client.disconnect();
        } else {
            ServerWorld.get().mtServiceTick.shutdown();
            ServerWorld.get().mtVirtualService.shutdown();

            try {
                if(!ServerWorld.get().mtVirtualService.awaitTermination(60, TimeUnit.SECONDS)) {
                    int didntFinish = ServerWorld.get().mtVirtualService.shutdownNow().size();
                    ExpoLogger.logerr("Failed to save " + didntFinish + " chunks (thread timeout).");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            localServer.stopServer();
        }

        AudioEngine.get().killAll();
        INSTANCE = null;
    }

    public void setLoadingMessage(String loadingMessage) {
        this.loadingMessage = loadingMessage;
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

    public ExpoClientPacketEvaluator getPacketEvaluator() {
        return packetEvaluator;
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
