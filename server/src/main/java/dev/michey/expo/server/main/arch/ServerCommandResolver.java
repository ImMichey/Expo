package dev.michey.expo.server.main.arch;

import dev.michey.expo.command.abstraction.AbstractCommand;
import dev.michey.expo.command.util.CommandExceptionReason;
import dev.michey.expo.command.CommandResolver;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerCommandResolver extends CommandResolver {

    public void resolveCommand(String fullLine, ServerPlayer sender) {
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
                ((ExecutablePlayer) mapped).executeCommand(split, sender);
            } catch (CommandSyntaxException e) {
                sendToSender("Syntax parse exception: " + e.getMessage(), sender);

                if(e.reason == CommandExceptionReason.OUT_OF_BOUNDS) {
                    sendToSender("Use: " + mapped.getCommandSyntax(), sender);
                }
            }
        } else {
            sendToSender("Command '" + firstArg + "' does not exist in command map", sender);
        }
    }

    public void sendToSender(String message, ServerPlayer sender) {
        if(sender == null) {
            // From console.
            log(message);
        } else {
            ServerPackets.p25ChatMessage("SERVER", message, PacketReceiver.player(sender));
        }
    }

}
