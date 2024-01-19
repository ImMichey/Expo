package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.util.PacketReceiver;

public class ServerCommandClear extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/clear";
    }

    @Override
    public String getCommandDescription() {
        return "Clears the inventory";
    }

    @Override
    public String getCommandSyntax() {
        return "/clear";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        if(player != null) {
            player.playerInventory.clear();
            player.heldItemPacket(PacketReceiver.whoCanSee(player));
            sendToSender("Cleared inventory.", player);
        }
    }

}
