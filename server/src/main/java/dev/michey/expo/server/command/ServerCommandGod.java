package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class ServerCommandGod extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/god";
    }

    @Override
    public String getCommandDescription() {
        return "Toggles god mode";
    }

    @Override
    public String getCommandSyntax() {
        return "/god";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        if(player != null) {
            player.god = !player.god;
            sendToSender("God: " + (player.god ? "ON" : "OFF"), player);
        }
    }

}
