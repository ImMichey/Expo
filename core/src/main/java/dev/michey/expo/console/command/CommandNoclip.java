package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class CommandNoclip extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/noclip";
    }

    @Override
    public String getCommandDescription() {
        return "Disables the local player physics box";
    }

    @Override
    public String getCommandSyntax() {
        return "/noclip";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(Expo.get().isPlaying()) {
            if(ServerPlayer.getLocalPlayer() != null) {
                ServerPlayer.getLocalPlayer().noclip = !ServerPlayer.getLocalPlayer().noclip;
            }
        } else {
            error("You are not ingame.");
        }
    }

}
