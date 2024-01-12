package dev.michey.expo.console.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.util.ExpoShared;

public class CommandTrack extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/track";
    }

    @Override
    public String getCommandDescription() {
        return "Toggles the performance profiler";
    }

    @Override
    public String getCommandSyntax() {
        return "/track";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        ExpoShared.TRACK_PERFORMANCE = !ExpoShared.TRACK_PERFORMANCE;
        success("Done.");
    }

}
