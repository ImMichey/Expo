package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.util.EntityRemovalReason;

public class ServerCommandKill extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/kill";
    }

    @Override
    public String getCommandDescription() {
        return "Kills the specified entity";
    }

    @Override
    public String getCommandSyntax() {
        return "/kill <entityId> [dimension]";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        int entityId = parseI(args, 1);
        String dimensionName = parseString(args, 2, player == null ? "overworld" : player.entityDimension);
        ServerDimension dimension = ServerWorld.get().getDimension(dimensionName);

        if(dimension == null) {
            sendToSender("Invalid dimension name '" + dimensionName + "'", player);
            return;
        }

        ServerEntity entity = dimension.getEntityManager().getEntityById(entityId);

        if(entity == null) {
            sendToSender("Invalid entityId '" + entityId + "' (entity doesn't exist)", player);
            return;
        }

        if(entity.getEntityType() == ServerEntityType.DYNAMIC_3D_TILE) {
            sendToSender("Illegal entityType '" + entity.getEntityType().name() + "'", player);
            return;
        }

        entity.killEntityWithPacket(EntityRemovalReason.COMMAND);
        sendToSender("Killed entity " + entity.getEntityType().name() + " with id " + entityId, player);
    }

}