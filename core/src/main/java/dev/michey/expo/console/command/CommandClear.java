package dev.michey.expo.console.command;

import dev.michey.expo.console.GameConsole;

public class CommandClear extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/clear";
    }

    @Override
    public String getCommandDescription() {
        return "Clears the console message history";
    }

    @Override
    public String getCommandSyntax() {
        return "/clear";
    }

    @Override
    public void executeCommand(String[] args) {
        GameConsole.get().clearHistory();
    }

}
