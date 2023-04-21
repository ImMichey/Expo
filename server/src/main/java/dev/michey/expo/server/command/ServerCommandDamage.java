package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

public class ServerCommandDamage extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/damage";
    }

    @Override
    public String getCommandDescription() {
        return "Debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/damage";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        if(player != null) {
            int dmg = parseI(args, 1);

            player.damagePlayer(dmg);
            ServerPackets.p23PlayerLifeUpdate(player.health, player.hunger, PacketReceiver.player(player));
            sendToSender("You received " + dmg + " damage ", player);
        }
    }

}
