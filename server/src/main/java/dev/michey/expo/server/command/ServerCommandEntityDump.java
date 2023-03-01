package dev.michey.expo.server.command;

import dev.michey.expo.command.AbstractCommand;
import dev.michey.expo.command.CommandSyntaxException;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimensionEntityManager;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerCommandEntityDump extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "/entitydump";
    }

    @Override
    public String getCommandDescription() {
        return "Debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/entitydump";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        ServerWorld world = ServerWorld.get();

        log("=== ENTITY DUMP START ===");
        for(ServerDimension dimension : world.getDimensions()) {
            ServerDimensionEntityManager m = dimension.getEntityManager();

            log("- DIMENSION: " + dimension.getDimensionName() + " " + m.entityCount() + " entities");

            for(ServerEntityType t : m.getExistingEntityTypes()) {
                log("- TYPE: " + t.ENTITY_NAME + " - " + m.getEntitiesOf(t).size() + " entities");
            }
        }
        log("=== ENTITY DUMP END ===");
    }

}