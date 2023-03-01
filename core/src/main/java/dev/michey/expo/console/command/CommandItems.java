package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.CommandSyntaxException;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.ServerPlayerInventory;

public class CommandItems extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/items";
    }

    @Override
    public String getCommandDescription() {
        return "Inventory item debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/items";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(Expo.get().isPlaying()) {
            ServerPlayer p = ServerPlayer.getLocalPlayer();

            p.playerInventory.fillRandom();
            success("Executed on server");
        } else {
            error("You are not ingame.");
        }
    }

}
