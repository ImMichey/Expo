package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;

public class CommandTime extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/time";
    }

    @Override
    public String getCommandDescription() {
        return "Sets your current dimension's world time";
    }

    @Override
    public String getCommandSyntax() {
        return "/time <value>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(Expo.get().isPlaying()) {
            float time = parseF(args, 1);

            if(time < 0) {
                error("Invalid time value (must be larger than 0).");
                return;
            }

            ExpoClientContainer.get().getClientWorld().worldTime = time;
            ServerPlayer.getLocalPlayer().getDimension().dimensionTime = time;
        } else {
            error("You are not ingame.");
        }
    }

}
