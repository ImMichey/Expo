package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunkGrid;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;

import java.util.Arrays;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerCommandChunkDump extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/chunkdump";
    }

    @Override
    public String getCommandDescription() {
        return "Debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/chunkdump";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        ServerWorld world = ServerWorld.get();

        sendToSender("=== CHUNKDUMP START ===", player);
        for(ServerDimension dimension : world.getDimensions()) {
            sendToSender("dimension: " + dimension.getDimensionName() + " - spawn: " + dimension.getDimensionSpawnX() + "," + dimension.getDimensionSpawnY(), player);
            ServerChunkGrid grid = dimension.getChunkHandler();
            grid.chunkdump();
        }
        sendToSender("=== CHUNKDUMP END ===", player);

        ServerChunk sc = player.getChunkGrid().getChunk(player.chunkX, player.chunkY);
    }

}