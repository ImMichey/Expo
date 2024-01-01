package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;

public class CommandPack extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/pack";
    }

    @Override
    public String getCommandDescription() {
        return "Runs the Tile Merger";
    }

    @Override
    public String getCommandSyntax() {
        return "/pack";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        Expo.get().sliceAndPatch();
        success("Done.");
    }

}
