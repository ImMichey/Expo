package dev.michey.expo.console.command;

import dev.michey.expo.command.CommandSyntaxException;
import dev.michey.expo.console.GameConsole;

public class CommandResize extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/resize";
    }

    @Override
    public String getCommandDescription() {
        return "Resizes the console window bounds";
    }

    @Override
    public String getCommandSyntax() {
        return "/resize <width> <height>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        int width = parseI(args, 1);
        int height = parseI(args, 2);

        if(width < 200) {
            error("Console width has to be at least 200 pixels.");
            return;
        }

        if(height < 200) {
            error("Console height has to be at least 200 pixels.");
            return;
        }

        GameConsole.get().resize(width, height);
        success("Console resized to [" + width + "x" + height + "]");
    }

}
