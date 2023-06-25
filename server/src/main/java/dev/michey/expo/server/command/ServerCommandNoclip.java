package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class ServerCommandNoclip extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/noclip";
    }

    @Override
    public String getCommandDescription() {
        return "Debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/noclip";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        if(player != null) {
            player.noclip = !player.noclip;
            sendToSender("Noclip: " + (player.noclip ? "ON" : "OFF"), player);
        }
    }

}
