package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class CommandSpeed extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/speed";
    }

    @Override
    public String getCommandDescription() {
        return "Sets the local player speed";
    }

    @Override
    public String getCommandSyntax() {
        return "/speed <amount>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(Expo.get().isPlaying()) {
            float amount = parseF(args, 1);

            if(ServerPlayer.getLocalPlayer() != null) {
                ServerPlayer.getLocalPlayer().playerSpeed = amount;
                success("Set player speed to [CYAN]" + amount);
            }
        } else {
            error("You are not ingame.");
        }
    }

}
