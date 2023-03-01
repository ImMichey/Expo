package dev.michey.expo.server.main.arch;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Server;
import dev.michey.expo.command.CommandResolver;
import dev.michey.expo.server.command.*;
import dev.michey.expo.server.config.ExpoServerConfiguration;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.fs.whitelist.ServerWhitelist;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.packet.ExpoServerListener;
import dev.michey.expo.server.main.packet.ExpoServerRegistry;
import dev.michey.expo.server.packet.Packet;

import java.io.IOException;
import java.util.Scanner;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoServerDedicated extends ExpoServerBase implements ApplicationListener {

    /** Server properties */
    protected final ExpoServerConfiguration config;
    private ServerWhitelist whitelist;

    /** Kryo layer */
    private Server kryoServer;
    private ExpoServerListener packetListener;

    /** Connection handler */
    private final PlayerConnectionHandler connectionHandler;

    /** Command line input */
    private CommandResolver resolver;

    /** Threads */
    private Thread consoleCommandThread;

    public ExpoServerDedicated(ExpoServerConfiguration config) {
        super(false, config.getWorldName());
        this.config = config;
        if(config.isWhitelistEnabled()) {
            whitelist = new ServerWhitelist();
            if(!whitelist.load()) {
                log("Failed to load ServerWhitelist, aborting application.");
                System.exit(0);
            }
        }
        connectionHandler = new PlayerConnectionHandler();
        log("Initialized dedicated ExpoServer with port " + config.getServerPort() + " and tick rate " + config.getServerTps());
    }

    @Override
    public boolean startServer() {
        log("Starting Kryo Server instance, buffer sizes: " + config.getWriteBufferSize() + " & " + config.getObjectBufferSize());
        kryoServer = new Server(config.getWriteBufferSize(), config.getObjectBufferSize());
        kryoServer.start();

        try {
            log("Attempting to bind server on port " + config.getServerPort());
            kryoServer.bind(config.getServerPort(), config.getServerPort());
        } catch (IOException e) {
            log("Failed to bind server on port " + config.getServerPort());
            e.printStackTrace();
            return false;
        }

        ExpoServerRegistry.registerPackets(kryoServer.getKryo());
        packetListener = new ExpoServerListener(packetReader);
        kryoServer.addListener(packetListener);

        // Start console input thread to poll commands
        if(config.isConsoleInput()) {
            resolver = new CommandResolver();
            resolver.addCommand(new ServerCommandHelp());
            resolver.addCommand(new ServerCommandStop());
            resolver.addCommand(new ServerCommandWhitelist());
            resolver.addCommand(new ServerCommandChunkDump());
            resolver.addCommand(new ServerCommandEntityDump());
            resolver.addCommand(new ServerCommandTime());
            resolver.addCommand(new ServerCommandItems());

            consoleCommandThread = new Thread("ExpoServerDedicated-ConsoleCommandScanner") {

                @Override
                public void run() {
                    Scanner scanner = new Scanner(System.in);

                    while(!consoleCommandThread.isInterrupted()) {
                        String cmd = scanner.nextLine();
                        log("User input: '" + cmd + "'");
                        resolver.resolveCommand(cmd);
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

    @Override
    public void broadcastPacketTCP(Packet packet) {
        kryoServer.sendToAllTCP(packet);
    }

    @Override
    public void broadcastPacketUDP(Packet packet) {
        kryoServer.sendToAllUDP(packet);
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
