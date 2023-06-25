package dev.michey.expo.server.main.arch;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public interface ExecutablePlayer {

    void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException;

}
