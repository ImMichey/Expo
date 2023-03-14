package dev.michey.expo.server.main.arch;

import dev.michey.expo.command.abstraction.AbstractCommand;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

import static dev.michey.expo.log.ExpoLogger.log;

public abstract class AbstractServerCommand extends AbstractCommand implements ExecutablePlayer {

    public void sendToSender(String message, ServerPlayer sender) {
        if(sender == null) {
            // From console.
            log(message);
        } else {
            ServerPackets.p25ChatMessage("SERVER", message, PacketReceiver.player(sender));
        }
    }

}
