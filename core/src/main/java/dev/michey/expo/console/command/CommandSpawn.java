package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
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
        return "/spawn <typeId> [<x> <y>] [static]";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            error("You are not ingame.");
            return;
        }

        int typeId = parseI(args, 1);

        if(typeId == ServerEntityType.PLAYER.ENTITY_ID) {
            error("Invalid entity type id '" + typeId + "' (you cannot spawn player entities)");
            return;
        }

        ServerEntity spawned = ServerEntityType.typeToEntity(typeId);

        if(spawned == null) {
            error("Invalid entity type id '" + typeId + "'");
            return;
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
