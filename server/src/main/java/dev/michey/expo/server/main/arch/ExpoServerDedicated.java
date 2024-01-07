package dev.michey.expo.server.main.arch;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import dev.michey.expo.server.config.ExpoServerConfiguration;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.fs.whitelist.ServerWhitelist;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.packet.ExpoPacketEvaluatorDedicated;
import dev.michey.expo.server.main.packet.ExpoServerRegistry;
import dev.michey.expo.server.packet.Packet;
import dev.michey.expo.server.steam.ExpoServerSteam;
import dev.michey.expo.util.ExpoShared;

import java.io.IOException;
import java.util.Scanner;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoServerDedicated extends ExpoServerBase implements ApplicationListener {

    /** Server properties */
    protected final ExpoServerConfiguration config;
    private ServerWhitelist whitelist;

    /** Kryo layer */
    private Server kryoServer;

    /** Connection handler */
    private final PlayerConnectionHandler connectionHandler;

    /** Threads */
    private Thread consoleCommandThread;

    /** Steam auth */
    private ExpoServerSteam expoServerSteam;

    public ExpoServerDedicated(ExpoServerConfiguration config) {
        super(false, config.getWorldName(), ExpoShared.RANDOM.nextInt());
        this.config = config;
        if(config.isWhitelistEnabled()) {
            whitelist = new ServerWhitelist();
            if(!whitelist.load()) {
                log("Failed to load ServerWhitelist, aborting application.");
                System.exit(0);
            }
        }
        if(config.isAuthPlayersEnabled()) {
            expoServerSteam = new ExpoServerSteam(config.getSteamWebApiKey());
        }
        connectionHandler = new PlayerConnectionHandler();
        log("Initialized dedicated ExpoServer with port " + config.getServerPort() + " and tick rate " + config.getServerTps());
    }

    @Override
    public boolean startServer() {
        log("Starting Kryo Server instance, buffer sizes: " + config.getWriteBufferSize() + " & " + config.getObjectBufferSize());
        kryoServer = new Server(config.getWriteBufferSize(), config.getObjectBufferSize());
        kryoServer.start();

        ExpoServerRegistry.registerPackets(kryoServer.getKryo());
        kryoServer.addListener((ExpoPacketEvaluatorDedicated) packetEvaluator);

        try {
            log("Attempting to bind server on port " + config.getServerPort());
            kryoServer.bind(config.getServerPort(), config.getServerPort());
        } catch (IOException e) {
            log("Failed to bind server on port " + config.getServerPort());
            e.printStackTrace();
            return false;
        }

        // Start console input thread to poll commands
        if(config.isConsoleInput()) {
            consoleCommandThread = new Thread("ExpoServerDedicated-ConsoleCommandScanner") {

                @Override
                public void run() {
                    Scanner scanner = new Scanner(System.in);

                    while(!consoleCommandThread.isInterrupted()) {
                        String cmd = scanner.nextLine();
                        log("User input: '" + cmd + "'");
                        ExpoServerBase.get().getCommandResolver().resolveCommand(cmd, null, false);
                    }
                }

            };

            consoleCommandThread.start();
        }

        connectionHandler.startPingUpdateScheduler();

        log("Dedicated Server ready");
        return true;
    }

    @Override
    public void stopServer() {
        log("Stopping dedicated ExpoServer");
        running = false;
        connectionHandler.stopPingUpdateScheduler();
        if(kryoServer != null) kryoServer.stop();
        serverContainer.cancelAll();
        consoleCommandThread.interrupt();
        getWorldSaveHandler().updateAndSave(ServerWorld.get());
        getWorldSaveHandler().getPlayerSaveHandler().saveAll();
        Gdx.app.exit();
    }

    public ExpoServerSteam getSteamHandler() {
        return expoServerSteam;
    }

    @Override
    public void broadcastPacketTCP(Packet packet) {
        kryoServer.sendToAllTCP(packet);
    }

    @Override
    public void broadcastPacketUDP(Packet packet) {
        kryoServer.sendToAllUDP(packet);
    }

    @Override
    public void broadcastPacketTCPExcept(Packet packet, Connection connection) {
        kryoServer.sendToAllExceptTCP(connection.getID(), packet);
    }

    @Override
    public void broadcastPacketUDPExcept(Packet packet, Connection connection) {
        kryoServer.sendToAllExceptUDP(connection.getID(), packet);
    }

    @Override
    public void create() {
        if(!startServer()) stopServer();
    }

    @Override
    public void render() {
        tps = Gdx.graphics.getFramesPerSecond();
        float d = 1f / (float) config.getServerTps();
        serverContainer.loop(this, d);
    }

    /** All regular GDX methods below can be ignored safely. */
    @Override
    public void resize(int width, int height) { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void dispose() { }

}
