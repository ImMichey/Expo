package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.util.EntityRemovalReason;

import java.util.List;

public class ServerCommandKillall extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/killall";
    }

    @Override
    public String getCommandDescription() {
        return "Kills all entities of specified type";
    }

    @Override
    public String getCommandSyntax() {
        return "/killall <entityType> [dimension]";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        String entityType = parseString(args, 1).toUpperCase();
        ServerEntityType type;

        try {
            type = ServerEntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            sendToSender("Invalid entityType '" + entityType + "' (entityType doesn't exist)", player);
            return;
        }

        if(type == ServerEntityType.DYNAMIC_3D_TILE) {
            sendToSender("Illegal entityType '" + entityType + "'", player);
            return;
        }

        String dimensionName = parseString(args, 2, player == null ? "overworld" : player.entityDimension);
        ServerDimension dimension = ServerWorld.get().getDimension(dimensionName);

        if(dimension == null) {
            sendToSender("Invalid dimension name '" + dimensionName + "'", player);
            return;
        }

        List<ServerEntity> entities = ServerWorld.get().getDimension(dimensionName).getEntityManager().getEntitiesOf(type);

        for(ServerEntity entity : entities) {
            entity.killEntityWithPacket(EntityRemovalReason.COMMAND);
        }

        int s = entities.size();
        sendToSender("Killed " + s + " entit" + (s == 1 ? "y" : "ies") + " of type '" + entityType + "'", player);
    }

}