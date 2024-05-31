package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class ServerCommandDebug extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/debug";
    }

    @Override
    public String getCommandDescription() {
        return "Debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/debug";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        if(player != null) {
            var items = player.getDimension().getEntityManager().getEntitiesOf(ServerEntityType.ITEM);

            for(ServerEntity se : items) {
                ServerItem item = (ServerItem) se;
                item.originX = item.posX;
                item.originY = item.posY;
                item.dstX = 100f;
                item.dstY = 100f;
                item.dstDelta = 0;
            }

            sendToSender("Done.", player);
        }
    }

}
