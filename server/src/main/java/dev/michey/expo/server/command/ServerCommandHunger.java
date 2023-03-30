package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.weather.Weather;

public class ServerCommandHunger extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/hunger";
    }

    @Override
    public String getCommandDescription() {
        return "Sets the player hunger level to 10";
    }

    @Override
    public String getCommandSyntax() {
        return "/hunger";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        player.hunger = 10.0f;
        ServerPackets.p23PlayerLifeUpdate(player.health, player.hunger, PacketReceiver.player(player));

        sendToSender("Set hunger to 10", player);
    }

}