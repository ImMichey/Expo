package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimensionEntityManager;

public class ServerCommandEntityDump extends AbstractServerCommand {

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
    public void executeCommand(String[] args, ServerPlayer sender, boolean ignoreLogging) throws CommandSyntaxException {
        ServerWorld world = ServerWorld.get();

        sendToSender("=== ENTITY DUMP START ===", sender);
        for(ServerDimension dimension : world.getDimensions()) {
            ServerDimensionEntityManager m = dimension.getEntityManager();

            sendToSender("- DIMENSION: " + dimension.getDimensionName() + " " + m.entityCount() + " entities", sender);

            for(ServerEntityType t : m.getExistingEntityTypes()) {
                sendToSender("- TYPE: " + t.ENTITY_NAME + " - " + m.getEntitiesOf(t).size() + " entities", sender);
            }
        }
        sendToSender("=== ENTITY DUMP END ===", sender);
    }

}