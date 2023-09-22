package dev.michey.expo.console.command;

import dev.michey.expo.command.util.CommandSyntaxException;

public class CommandGc extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/gc";
    }

    @Override
    public String getCommandDescription() {
        return "Runs the ZGC";
    }

    @Override
    public String getCommandSyntax() {
        return "/gc";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        System.gc();
        success("Done.");
    }

}
