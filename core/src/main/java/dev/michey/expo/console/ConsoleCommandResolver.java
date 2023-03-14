package dev.michey.expo.console;

import dev.michey.expo.command.util.CommandExceptionReason;
import dev.michey.expo.command.CommandResolver;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.console.command.AbstractConsoleCommand;

import static dev.michey.expo.log.ExpoLogger.logc;

public class ConsoleCommandResolver extends CommandResolver {

    @Override
    public void resolveCommand(String fullLine) {
        String lower = fullLine.toLowerCase();
        String[] split = lower.split(" ");
        AbstractConsoleCommand mapped = (AbstractConsoleCommand) getCommandMap().get(split[0]);

        if(mapped != null) {
            logc("Executing console command '" + fullLine + "'");

            try {
                if(!mapped.isLocked()) {
                    mapped.executeCommand(split);
                } else {
                    GameConsole.get().addSystemErrorMessage("Command " + split[0] + " is currently still executing.");
                }
            } catch (CommandSyntaxException e) {
                String msg = "Syntax parse exception: " + e.getMessage();
                GameConsole.get().addSystemErrorMessage(msg);

                if(e.reason == CommandExceptionReason.OUT_OF_BOUNDS) {
                    msg = "Use: " + mapped.getCommandSyntax();
                    GameConsole.get().addSystemErrorMessage(msg);
                }
            }
        } else {
            String msg = "The command '" + split[0] + "' does not exist.";
            GameConsole.get().addSystemErrorMessage(msg);
        }
    }

}
