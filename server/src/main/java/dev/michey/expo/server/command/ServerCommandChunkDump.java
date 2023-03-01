package dev.michey.expo.server.command;

import dev.michey.expo.command.AbstractCommand;
import dev.michey.expo.command.CommandSyntaxException;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunkGrid;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerCommandChunkDump extends AbstractCommand {

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
    public void executeCommand(String[] args) throws CommandSyntaxException {
        ServerWorld world = ServerWorld.get();

        log("=== CHUNKDUMP START ===");
        for(ServerDimension dimension : world.getDimensions()) {
            log("dimension: " + dimension.getDimensionName() + " - spawn: " + dimension.getDimensionSpawnX() + "," + dimension.getDimensionSpawnY());
            ServerChunkGrid grid = dimension.getChunkHandler();
            grid.chunkdump();
        }
        log("=== CHUNKDUMP END ===");
    }

}