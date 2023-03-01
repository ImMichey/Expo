package dev.michey.expo.console.command;

import dev.michey.expo.command.CommandSyntaxException;
import dev.michey.expo.logic.container.ExpoClientContainer;

public class CommandCol extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/col";
    }

    @Override
    public String getCommandDescription() {
        return "Background color debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/col <r> <g> <b>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        float r = parseF(args, 1);
        float g = parseF(args, 2);
        float b = parseF(args, 3);

        ExpoClientContainer.get().getClientWorld().ambientLightingR = r;
        ExpoClientContainer.get().getClientWorld().ambientLightingG = g;
        ExpoClientContainer.get().getClientWorld().ambientLightingB = b;
        success("Random color [RED]" + r + " [GREEN]" + g + " [BLUE]" + b);
    }

}
