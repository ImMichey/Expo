package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerCommandTime extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/time";
    }

    @Override
    public String getCommandDescription() {
        return "Sets the main dimension's time";
    }

    @Override
    public String getCommandSyntax() {
        return "/time <value>";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        float time = parseF(args, 1);

        if(time < 0) {
            sendToSender("Invalid time (must be larger than 0).", player);
            return;
        }

        ServerDimension dim = ServerWorld.get().getMainDimension();
        dim.dimensionTime = time;

        // Update.
        ServerPackets.p14WorldUpdate(time, dim.dimensionWeather.WEATHER_ID, dim.dimensionWeatherStrength, PacketReceiver.dimension(dim));

        sendToSender("Set main dimension time to " + time, player);
    }

}