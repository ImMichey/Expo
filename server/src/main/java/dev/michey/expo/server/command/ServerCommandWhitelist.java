package dev.michey.expo.server.command;

import dev.michey.expo.command.AbstractCommand;
import dev.michey.expo.command.CommandSyntaxException;
import dev.michey.expo.server.fs.whitelist.ServerWhitelist;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerCommandWhitelist extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "/whitelist";
    }

    @Override
    public String getCommandDescription() {
        return "Adds/removes a player to/from the whitelist";
    }

    @Override
    public String getCommandSyntax() {
        return "/whitelist <add/remove> <username>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(ServerWhitelist.get() == null) {
            log("Server whitelist is disabled. Enable whitelist in config and restart the server.");
            return;
        }

        String mode = parseString(args, 1).toLowerCase();
        String username = parseString(args, 2);
        requireString(mode, "add", "remove");

        if(mode.equals("add")) {
            ServerWhitelist.get().addWhitelistedPlayer(username);
        } else {
            ServerWhitelist.get().removeWhitelistedPlayer(username);
        }
    }

}