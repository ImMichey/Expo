package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.logic.entity.ClientPlayer;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;

public class CommandReload extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/reload";
    }

    @Override
    public String getCommandDescription() {
        return "Reloads the internal Item Mappings";
    }

    @Override
    public String getCommandSyntax() {
        return "/reload";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        new ItemMapper(true, true);
        Expo.get().loadItemMapperTextures();
        success("Reloaded internal item mappings");

        if(ClientPlayer.getLocalPlayer() != null) ClientPlayer.getLocalPlayer().updateHoldingItemSprite();
    }

}
