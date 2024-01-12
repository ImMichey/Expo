package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;

public class ServerCommandSaveall extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/saveall";
    }

    @Override
    public String getCommandDescription() {
        return "Saves all inactive chunks";
    }

    @Override
    public String getCommandSyntax() {
        return null;
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        sendToSender("Saving all inactive chunks...", player);

        for(ServerDimension dim : ServerWorld.get().getDimensions()) {
            for(var x : dim.getChunkHandler().inactiveChunkMap.values()) {
                x.value = 0L;
            }
        }
    }

}
