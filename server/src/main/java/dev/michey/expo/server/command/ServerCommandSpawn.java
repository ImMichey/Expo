package dev.michey.expo.server.command;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.GenerationUtils;
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
        return "/spawn <typeId/typeName> [<x> <y>] [spreadDst]";
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

        ServerPlayer usePlayer = ServerPlayer.getLocalPlayer() == null ? player : ServerPlayer.getLocalPlayer();

        if(usePlayer != null) {
            x = usePlayer.posX;
            y = usePlayer.posY;
        }

        if(args.length >= 3) {
            if(args[2].startsWith("~") && usePlayer != null) {
                if(args[2].equals("~")) {
                    x = usePlayer.posX;
                } else {
                    x = parseF(args[2].substring(1), 2) + usePlayer.posX;
                }
            } else {
                x = parseF(args, 2);
            }
        }
        if(args.length >= 4) {
            if(args[3].startsWith("~") && usePlayer != null) {
                if(args[3].equals("~")) {
                    y = usePlayer.posY;
                } else {
                    y = parseF(args[3].substring(1), 3) + usePlayer.posY;
                }
            } else {
                y = parseF(args, 3);
            }
        }

        spawned.posX = x;
        spawned.posY = y;

        if(args.length == 5) {
            float spreadDst = parseF(args, 4);
            Vector2 spread = GenerationUtils.circularRandom(spreadDst);
            spawned.posX += spread.x;
            spawned.posY += spread.y;
        }

        ServerWorld.get().registerServerEntity(ExpoShared.DIMENSION_OVERWORLD, spawned);
        if(!ignoreLogging) sendToSender("Spawned entity " + spawned.getEntityType() + " at position " + spawned.posX + ", " + spawned.posY, player);
    }

    private boolean forbiddenSpawn(int id) {
        return switch (id) {
            case 0, 1, 7, 16, 18  -> true;
            default -> false;
        };
    }

}
