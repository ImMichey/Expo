package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunkGrid;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;

import java.util.LinkedList;
import java.util.List;

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
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        ServerWorld world = ServerWorld.get();

        sendToSender("=== ENTITYDUMP START ===", player);
        for(ServerEntityType type : ServerEntityType.values()) {
            LinkedList<ServerEntity> list = world.getMainDimension().getEntityManager().getEntitiesOf(type);
            sendToSender("- " + type.name() + ": " + list.size(), player);
        }
        sendToSender("=== ENTITYDUMP END ===", player);
    }

}