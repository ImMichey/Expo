package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;

public class ServerCommandItems extends AbstractServerCommand {

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
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        if(player == null) {
            var players = ServerWorld.get().getMainDimension().getEntityManager().getAllPlayers();

            for(var p : players) {
                p.playerInventory.fillRandom();
            }

            sendToSender("Set a new inventory for every player", null);
        } else {
            player.playerInventory.fillRandom();
            sendToSender("You received a new inventory", player);
        }
    }

}
