package dev.michey.expo.console.command;

import dev.michey.expo.command.AbstractCommand;
import dev.michey.expo.console.GameConsole;

import java.util.Collection;

public class CommandHelp extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/help";
    }

    @Override
    public String getCommandDescription() {
        return "Prints out every available console command";
    }

    @Override
    public String getCommandSyntax() {
        return "/help";
    }

    @Override
    public void executeCommand(String[] args) {
        Collection<AbstractCommand> commands = GameConsole.get().getResolver().getCommandsSorted();

        empty();
        message("[GREEN]Every available console command is listed below:");

        for(AbstractCommand cmd : commands) {
            message("  [YELLOW]" + cmd.getCommandSyntax() + " [GRAY]- [ORANGE]" + cmd.getCommandDescription());
        }

        empty();
    }

}
