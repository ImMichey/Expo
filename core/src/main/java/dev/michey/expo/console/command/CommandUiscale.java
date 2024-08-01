package dev.michey.expo.console.command;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.screen.MenuScreen;
import dev.michey.expo.util.GameSettings;

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
        int amount = MathUtils.clamp(parseI(args, 1), 1, 5);
        GameSettings.get().uiScale = amount;
        RenderContext.get().updatePreferredFonts(amount);
        if(ExpoClientContainer.get() != null) ExpoClientContainer.get().getPlayerUI().changeUiScale();
        MenuScreen.get().getActiveGroup().update(RenderContext.get());
        success("UI scale is now [CYAN]" + amount);
    }

}