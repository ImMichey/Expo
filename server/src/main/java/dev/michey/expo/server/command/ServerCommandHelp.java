package dev.michey.expo.server.command;

import dev.michey.expo.command.AbstractCommand;
import dev.michey.expo.command.CommandSyntaxException;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerCommandHelp extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "/help";
    }

    @Override
    public String getCommandDescription() {
        return "Lists all available server commands";
    }

    @Override
    public String getCommandSyntax() {
        return null;
    }

    @Override
    public void executeCommand(String[] args) {
        log("All available commands: ");

        for(AbstractCommand command : getResolver().getCommands()) {
            log(command.getCommandName() + " - " + command.getCommandDescription());
        }
    }

}
