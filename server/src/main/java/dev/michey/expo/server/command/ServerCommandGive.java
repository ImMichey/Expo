package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.ItemMetadata;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.util.ExpoShared;

public class ServerCommandGive extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/give";
    }

    @Override
    public String getCommandDescription() {
        return "Gives you an item";
    }

    @Override
    public String getCommandSyntax() {
        return "/give <itemId/itemName> [amount]";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player, boolean ignoreLogging) throws CommandSyntaxException {
        if(args.length <= 1) {
            sendToSender(getCommandSyntax(), player);
            return;
        }

        var idResult = ExpoShared.asInt(args[1]);
        int amount = 1;

        if(args.length == 3) {
            var amountResult = ExpoShared.asInt(args[2]);

            if(amountResult.value) {
                amount = amountResult.key;

                if(amount <= 0) {
                    sendToSender("Invalid item amount '" + amount + "'", player);
                    return;
                }
            }
        }

        if(idResult.value) {
            // Valid number.
            ItemMapping mapping = ItemMapper.get().getMapping(idResult.key);

            if(mapping == null) {
                sendToSender("Invalid item id '" + args[1] + "'", player);
                return;
            }

            ServerInventoryItem item = new ServerInventoryItem(mapping.id, amount);
            var addResult = player.playerInventory.addItem(item);
            ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(addResult.changeResult, PacketReceiver.player(player));

            sendToSender("You received " + amount + "x " + mapping.displayName, player);
        } else {
            // Try name.
            String stripped = args[1].toLowerCase();
            ItemMapping mapping = ItemMapper.get().getMapping(stripped);

            if(mapping == null) {
                if(!stripped.startsWith("item_")) {
                    stripped = "item_" + stripped;
                    mapping = ItemMapper.get().getMapping(stripped);
                }

                if(mapping == null) {
                    sendToSender("Invalid item name '" + args[1].toLowerCase() + "'", player);
                    return;
                }
            }

            ServerInventoryItem item = new ServerInventoryItem(mapping.id, amount);
            var addResult = player.playerInventory.addItem(item);
            ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(addResult.changeResult, PacketReceiver.player(player));

            sendToSender("You received " + amount + "x " + mapping.displayName, player);
        }
    }

}