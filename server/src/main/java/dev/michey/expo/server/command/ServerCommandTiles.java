package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

public class ServerCommandTiles extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/tiles";
    }

    @Override
    public String getCommandDescription() {
        return "Invalidates all tiles";
    }

    @Override
    public String getCommandSyntax() {
        return "/tiles";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        if(player == null) {
            sendToSender("You must be a player to do this!", null);
            return;
        }

        ServerChunk[] chunks = player.getChunkGrid().getChunksInPlayerRange(player);

        for(ServerChunk chunk : chunks) {
            chunk.generate(false);
            ServerPackets.p11ChunkData(chunk.chunkX, chunk.chunkY, chunk.tiles, PacketReceiver.player(player));
        }

        sendToSender("Updated chunks in view", player);
    }

}
