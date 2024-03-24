package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.weather.Weather;

public class ServerCommandRain extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/rain";
    }

    @Override
    public String getCommandDescription() {
        return "Sets the main dimension's weather to rain";
    }

    @Override
    public String getCommandSyntax() {
        return "/rain";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        ServerDimension dim = ServerWorld.get().getMainDimension();
        dim.dimensionWeather = Weather.RAIN;
        dim.dimensionWeatherDuration = Weather.RAIN.generateWeatherDuration();
        dim.dimensionWeatherStrength = Weather.RAIN.generateWeatherStrength();

        // Update.
        ServerPackets.p14WorldUpdate(dim.getDimensionName(), dim.dimensionTime, dim.dimensionWeather.WEATHER_ID, dim.dimensionWeatherStrength, PacketReceiver.dimension(dim));

        sendToSender("Set main dimension weather to RAIN", player);
    }

}