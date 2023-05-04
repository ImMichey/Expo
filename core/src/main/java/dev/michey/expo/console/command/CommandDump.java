package dev.michey.expo.console.command;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ItemSheet;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class CommandDump extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/dump";
    }

    @Override
    public String getCommandDescription() {
        return "Dumps internal data";
    }

    @Override
    public String getCommandSyntax() {
        return "/dump";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            error("You are not ingame.");
            return;
        }

        var entities = ClientEntityType.values();
        success("===Entity Dump===");
        for(ClientEntityType cet : entities) {
            message("- " + cet.name() + " " + cet.ENTITY_ID);
        }

        var items = ItemMapper.get().getItemMappings();
        List<ItemMapping> list = new LinkedList<>(items);
        list.sort(Comparator.comparingInt(o -> o.id));
        success("===Item Dump===");

        for(ItemMapping mapping : list) {
            message("- " + mapping.id + " " + mapping.displayName);
        }
    }

}