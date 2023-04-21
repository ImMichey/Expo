package dev.michey.expo.server.command;

import dev.michey.expo.command.abstraction.AbstractCommand;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class ServerCommandHelp extends AbstractServerCommand {

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
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        sendToSender("All available commands: ", player);

        for(AbstractCommand command : getResolver().getCommands()) {
            sendToSender(command.getCommandName() + " - " + command.getCommandDescription(), player);
        }
    }

}
