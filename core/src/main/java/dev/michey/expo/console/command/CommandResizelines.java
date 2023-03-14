package dev.michey.expo.console.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.console.GameConsole;

public class CommandResizelines extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/resizelines";
    }

    @Override
    public String getCommandDescription() {
        return "Resizes the console window bounds";
    }

    @Override
    public String getCommandSyntax() {
        return "/resizelines <lines>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        int lines = parseI(args, 1);

        if(lines < 5) {
            error("Console lines has to be at least 5 lines.");
            return;
        }

        int[] size = GameConsole.get().resizeLines(lines);
        success("Console resized to [" + size[0] + "x" + size[1] + "]");
    }

}