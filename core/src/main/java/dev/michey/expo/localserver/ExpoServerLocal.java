package dev.michey.expo.localserver;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.Expo;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoServerLocal extends ExpoServerBase {

    private Thread gameLogicThread;

    public ExpoServerLocal(String worldName) {
        super(true, worldName);
        if(ItemMapper.get() == null) {
            new ItemMapper(true, false);
            Expo.get().loadItemMapperTextures();
        }

        log("Initialized local ExpoServer");
    }

    public void consumePacket(Packet packet) {
        packetReader.handlePacket(packet);
    }

    @Override
    public boolean startServer() {
        // The local server requires no networking, so this will never fail.
        gameLogicThread = new Thread("ExpoServerLocal-GameLogic") {

            @Override
            public void run() {
                long initialTime = System.nanoTime();
                final double timeU = 1_000_000_000 / (double) ExpoShared.DEFAULT_SERVER_TICK_RATE;
                double deltaU = 0;
                int ticks = 0;
                long timer = System.currentTimeMillis();

                while(!gameLogicThread.isInterrupted()) {
                    long currentTime = System.nanoTime();
                    deltaU += (currentTime - initialTime) / timeU;
                    initialTime = currentTime;

                    if(deltaU >= 1) {
                        serverContainer.loop(ExpoServerLocal.get(), 1f / (float) ExpoShared.DEFAULT_SERVER_TICK_RATE);
                        ticks++;
                        deltaU--;
                    }

                    if(System.currentTimeMillis() - timer > 1000) {
                        tps = ticks;
                        ticks = 0;
                        timer += 1000;
                    }
                }
            }

        };

        gameLogicThread.start();
        log("Local Server ready");
        return true;
    }

    @Override
    public void stopServer() {
        log("Stopping local ExpoServer");
        running = false;
        serverContainer.cancelAll();
        gameLogicThread.interrupt();
        getWorldSaveHandler().updateAndSave(ServerWorld.get());
        ServerPlayer sp = ServerPlayer.getLocalPlayer();
        if(sp != null) sp.updateSaveFile();
        getWorldSaveHandler().getPlayerSaveHandler().saveAll();
    }

    @Override
    public void broadcastPacketTCP(Packet packet) {
        ExpoClientContainer.get().getPacketReader().handlePacketLocal(packet);
    }

    @Override
    public void broadcastPacketTCPExcept(Packet packet, Connection connection) {

    }

    @Override
    public void broadcastPacketUDP(Packet packet) {
        ExpoClientContainer.get().getPacketReader().handlePacketLocal(packet);
    }

    @Override
    public void broadcastPacketUDPExcept(Packet packet, Connection connection) {

    }

}
