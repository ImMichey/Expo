package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.util.ExpoShared;

public class ServerCommandSpawn extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/spawn";
    }

    @Override
    public String getCommandDescription() {
        return "Spawns an entity";
    }

    @Override
    public String getCommandSyntax() {
        return "/spawn <typeId/typeName> [<x> <y>] [static]";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        var preResult = ExpoShared.asInt(args[1]);
        ServerEntity spawned;

        if(preResult.value) {
            int typeId = preResult.key;

            if(forbiddenSpawn(typeId)) {
                sendToSender("Forbidden entity type id '" + typeId + "'", player);
                return;
            }

            spawned = ServerEntityType.typeToEntity(typeId);

            if(spawned == null) {
                sendToSender("Invalid entity type id '" + typeId + "'", player);
                return;
            }
        } else {
            spawned = ServerEntityType.nameToEntity(args[1].toUpperCase());

            if(spawned == null) {
                sendToSender("Invalid entity name '" + args[1].toUpperCase() + "'", player);
                return;
            }
        }

        if(args.length >= 4) {
            float x = parseF(args, 2);
            float y = parseF(args, 3);

            spawned.posX = x;
            spawned.posY = y;

            if(args.length == 5) {
                boolean staticEntity = parseB(args, 4);

                if(staticEntity) {
                    spawned.setStaticEntity();
                }
            }
        } else {
            if(ServerPlayer.getLocalPlayer() != null) {
                spawned.posX = ServerPlayer.getLocalPlayer().posX;
                spawned.posY = ServerPlayer.getLocalPlayer().posY;
            }
        }

        ServerWorld.get().registerServerEntity(ExpoShared.DIMENSION_OVERWORLD, spawned);
        sendToSender("Spawned entity " + spawned.getEntityType() + " at position " + spawned.posX + ", " + spawned.posY + (spawned.staticPosition ? " as static entity" : ""), player);
    }

    private boolean forbiddenSpawn(int id) {
        return switch (id) {
            case 0, 1, 7, 16, 18  -> true;
            default -> false;
        };
    }

}
