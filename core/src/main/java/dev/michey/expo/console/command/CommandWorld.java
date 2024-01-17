package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.screen.GameScreen;
import dev.michey.expo.server.main.logic.world.gen.WorldGen;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ExpoShared;

public class CommandWorld extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/world";
    }

    @Override
    public String getCommandDescription() {
        return "Starts a locally hosted game session with specified world name";
    }

    @Override
    public String getCommandSyntax() {
        return "/world <name> [seed]";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            String name = parseString(args, 1);
            int seed;

            if(args.length > 2) {
                try {
                    seed = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    seed = args[2].hashCode();
                }
            } else {
                seed = ExpoShared.RANDOM.nextInt();
            }

            success("Starting a single-player game session with world name [CYAN]" + name + " and seed " + seed);
            long a = System.currentTimeMillis();

            new WorldGen();
            ExpoServerLocal localServer = new ExpoServerLocal(name, seed);

            if(localServer.startServer()) {
                ExpoLogger.log("Started local server in " + (System.currentTimeMillis() - a) + "ms");
                Expo.get().switchToNewScreen(new GameScreen(localServer));
                ClientPackets.p0ConnectReq(null);
            }
        } else {
            error("You are already ingame. Use /quit.");
        }
    }

}
