package dev.michey.expo.command;

import java.util.*;

import static dev.michey.expo.log.ExpoLogger.log;

public class CommandResolver {

    private HashMap<String, AbstractCommand> commandMap;

    public CommandResolver() {
        commandMap = new HashMap<>();
    }

    public void addCommand(AbstractCommand command) {
        log("Registering command: " + command.getCommandName());
        command.setResolver(this);
        commandMap.put(command.getCommandName().toLowerCase(), command);
    }

    public Collection<AbstractCommand> getCommandsSorted() {
        List<AbstractCommand> list = new LinkedList<>(getCommands());
        list.sort(Comparator.comparing(AbstractCommand::getCommandName));
        return list;
    }

    public void resolveCommand(String fullLine) {
        String[] split = fullLine.split(" ");

        if(split.length == 0) {
            log("Invalid input: '" + fullLine + "'");
            return;
        }

        String firstArg = split[0].toLowerCase();
        AbstractCommand mapped = commandMap.get(firstArg);

        if(mapped != null) {
            log("Executing command '" + fullLine + "'");

            try {
                mapped.executeCommand(split);
            } catch (CommandSyntaxException e) {
                log("Syntax parse exception: " + e.getMessage());

                if(e.reason == CommandExceptionReason.OUT_OF_BOUNDS) {
                    log("Use: " + mapped.getCommandSyntax());
                }
            }
        } else {
            log("Command '" + firstArg + "' does not exist in command map");
        }
    }

    public Collection<AbstractCommand> getCommands() {
        return commandMap.values();
    }

    public HashMap<String, AbstractCommand> getCommandMap() {
        return commandMap;
    }

}
