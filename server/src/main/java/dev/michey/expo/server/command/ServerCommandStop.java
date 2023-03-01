package dev.michey.expo.server.command;

import dev.michey.expo.command.AbstractCommand;
import dev.michey.expo.command.CommandSyntaxException;
import dev.michey.expo.server.main.arch.ExpoServerBase;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerCommandStop extends AbstractCommand {

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
    public void executeCommand(String[] args) throws CommandSyntaxException {
        log("Received server stop signal, shutting down application...");
        ExpoServerBase.get().stopServer();
    }

}
