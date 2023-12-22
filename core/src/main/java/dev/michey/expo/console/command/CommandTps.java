package dev.michey.expo.console.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.util.ExpoShared;

public class CommandTps extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/tps";
    }

    @Override
    public String getCommandDescription() {
        return "Sets the local server world tps limit";
    }

    @Override
    public String getCommandSyntax() {
        return "/tps <amount>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        int amount = parseI(args, 1);
        ExpoShared.DEFAULT_LOCAL_TICK_RATE = amount;
    }

}