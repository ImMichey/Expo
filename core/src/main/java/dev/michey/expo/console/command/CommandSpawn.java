package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.command.ServerCommandSpawn;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.util.ExpoShared;

public class CommandSpawn extends AbstractConsoleCommand {

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
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            error("You are not ingame.");
            return;
        }

        if(Expo.get().isMultiplayer()) {
            error("Use the ingame chat for this command.");
            return;
        }

        if(args.length <= 1) {
            error(getCommandSyntax());
            return;
        }

        var preResult = ExpoShared.asInt(args[1]);
        ServerEntity spawned;

        if(preResult.value) {
            int typeId = preResult.key;

            if(ServerCommandSpawn.forbiddenSpawn(typeId)) {
                error("Forbidden entity type id '" + typeId + "'");
                return;
            }

            spawned = ServerEntityType.typeToEntity(typeId);

            if(spawned == null) {
                error("Invalid entity type id '" + typeId + "'");
                return;
            }
        } else {
            String typeName = args[1].toUpperCase();

            try {
                ServerEntityType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                error("Invalid entity name '" + typeName + "'");
                return;
            }

            if(ServerCommandSpawn.forbiddenSpawn(typeName)) {
                error("Forbidden entity type '" + typeName + "'");
                return;
            }

            spawned = ServerEntityType.nameToEntity(typeName);

            if(spawned == null) {
                // This should never happen anymore after code additions of 11.05.2024
                error("Invalid entity name '" + typeName + "'");
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
        success("Spawned entity " + spawned.getEntityType() + " at position " + spawned.posX + ", " + spawned.posY + (spawned.staticPosition ? " as static entity" : ""));
    }

}
