package dev.michey.expo.console.command;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.logic.container.ExpoClientContainer;

public class CommandUiscale extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/uiscale";
    }

    @Override
    public String getCommandDescription() {
        return "Scales the game UI";
    }

    @Override
    public String getCommandSyntax() {
        return "/uiscale <amount>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            error("You are not ingame.");
            return;
        }

        int amount = MathUtils.clamp(parseI(args, 1), 1, 5);
        ExpoClientContainer.get().getPlayerUI().changeUiScale(amount);
        success("UI scale is now [CYAN]" + amount);
    }

}