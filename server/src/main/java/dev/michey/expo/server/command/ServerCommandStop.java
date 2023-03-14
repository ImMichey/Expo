package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;

public class ServerCommandStop extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/stop";
    }

    @Override
    public String getCommandDescription() {
        return "Stops the game server";
    }

    @Override
    public String getCommandSyntax() {
        return null;
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        sendToSender("Received server stop signal, shutting down application...", player);
        ExpoServerBase.get().stopServer();
    }

}
