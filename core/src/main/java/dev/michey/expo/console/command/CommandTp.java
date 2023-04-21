package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

public class CommandTp extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/tp";
    }

    @Override
    public String getCommandDescription() {
        return "Teleports the player to desired location";
    }

    @Override
    public String getCommandSyntax() {
        return "/tp <x> <y>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(Expo.get().isPlaying()) {
            float x = parseF(args, 1);
            float y = parseF(args, 2);

            ServerPlayer l = ServerPlayer.getLocalPlayer();

            if(l != null) {
                l.posX = x;
                l.posY = y;
                ServerPackets.p6EntityPosition(l.entityId, l.posX, l.posY, PacketReceiver.whoCanSee(l));
                success("Teleported player to " + x + ", " + y);
            }
        } else {
            error("You are not ingame.");
        }
    }

}
