package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.screen.GameScreen;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ClientStatic;

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
        return "/world <name>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            String name = parseString(args, 1);
            success("Starting a single-player game session with world name [CYAN]" + name);
            ExpoServerLocal localServer = new ExpoServerLocal(name);

            if(localServer.startServer()) {
                Expo.get().switchToNewScreen(new GameScreen(localServer));
                ClientPackets.p0Auth(ClientStatic.PLAYER_USERNAME);
            }
        } else {
            error("You are already ingame. Use /quit.");
        }
    }

}
