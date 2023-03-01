package dev.michey.expo.server.command;

import dev.michey.expo.command.AbstractCommand;
import dev.michey.expo.command.CommandSyntaxException;
import dev.michey.expo.server.main.logic.world.ServerWorld;

public class ServerCommandItems extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "/items";
    }

    @Override
    public String getCommandDescription() {
        return "Debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/items";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        var players = ServerWorld.get().getMainDimension().getEntityManager().getAllPlayers();

        for(var p : players) {
            p.playerInventory.fillRandom();
        }
    }

}
