package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.console.GameConsole;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;

public class CommandInquire extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/inquire";
    }

    @Override
    public String getCommandDescription() {
        return "Dump entity data";
    }

    @Override
    public String getCommandSyntax() {
        return "/inquire <entityId>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            error("You are not ingame.");
            return;
        }

        int entityId = parseI(args, 1);
        var entity = ClientEntityManager.get().getEntityById(entityId);

        if(entity == null) {
            error("Invalid entityId.");
            return;
        }

        GameConsole.get().addSystemMessage("-> " + entity.clientPosX + "," + entity.clientPosY);
    }

}