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
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        if(args.length <= 1) {
            if(!ignoreLogging) sendToSender(getCommandSyntax(), player);
            return;
        }

        var preResult = ExpoShared.asInt(args[1]);
        ServerEntity spawned;

        if(preResult.value) {
            int typeId = preResult.key;

            if(forbiddenSpawn(typeId)) {
                if(!ignoreLogging) sendToSender("Forbidden entity type id '" + typeId + "'", player);
                return;
            }

            spawned = ServerEntityType.typeToEntity(typeId);

            if(spawned == null) {
                if(!ignoreLogging) sendToSender("Invalid entity type id '" + typeId + "'", player);
                return;
            }
        } else {
            spawned = ServerEntityType.nameToEntity(args[1].toUpperCase());

            if(spawned == null) {
                if(!ignoreLogging) sendToSender("Invalid entity name '" + args[1].toUpperCase() + "'", player);
                return;
            }
        }

        float x = 0, y = 0;
        boolean ix = false, iy = false;

        if(ServerPlayer.getLocalPlayer() != null) {
            spawned.posX = ServerPlayer.getLocalPlayer().posX;
            spawned.posY = ServerPlayer.getLocalPlayer().posY;
        } else if(player != null) {
            spawned.posX = player.posX;
            spawned.posY = player.posY;
        }

        if(args.length >= 3) {
            if(args[2].startsWith("~") && ServerPlayer.getLocalPlayer() != null) {
                x = parseF(args[2].substring(1), 2) + ServerPlayer.getLocalPlayer().posX;
            } else {
                x = parseF(args, 2);
            }
            ix = true;
        }
        if(args.length >= 4) {
            if(args[3].startsWith("~") && ServerPlayer.getLocalPlayer() != null) {
                y = parseF(args[3].substring(1), 3) + ServerPlayer.getLocalPlayer().posY;
            } else {
                y = parseF(args, 3);
            }
            iy = true;
        }

        if(!ix) {
            if(ServerPlayer.getLocalPlayer() != null) {
                x = ServerPlayer.getLocalPlayer().posX;
            } else if(player != null) {
                x = player.posX;
            }
        }
        if(!iy) {
            if(ServerPlayer.getLocalPlayer() != null) {
                y = ServerPlayer.getLocalPlayer().posY;
            } else if(player != null) {
                y = player.posY;
            }
        }

        spawned.posX = x;
        spawned.posY = y;

        if(args.length == 5) {
            boolean staticEntity = parseB(args, 4);

            if(staticEntity) {
                spawned.setStaticEntity();
            }
        }

        ServerWorld.get().registerServerEntity(ExpoShared.DIMENSION_OVERWORLD, spawned);
        if(!ignoreLogging) sendToSender("Spawned entity " + spawned.getEntityType() + " at position " + spawned.posX + ", " + spawned.posY + (spawned.staticPosition ? " as static entity" : ""), player);
    }

    private boolean forbiddenSpawn(int id) {
        return switch (id) {
            case 0, 1, 7, 16, 18  -> true;
            default -> false;
        };
    }

}
