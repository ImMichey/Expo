package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class ServerCommandSpeed extends AbstractServerCommand {

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
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        if(player != null) {
            player.playerSpeed = parseF(args, 1);
            sendToSender("Your new speed is " + player.playerSpeed, player);
        }
    }

}
