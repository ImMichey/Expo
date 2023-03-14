package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.render.RenderContext;

public class CommandZoom extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/zoom";
    }

    @Override
    public String getCommandDescription() {
        return "Adjusts the ingame camera zoom";
    }

    @Override
    public String getCommandSyntax() {
        return "/zoom <amount>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(Expo.get().isPlaying()) {
            float amount = parseF(args, 1);
            RenderContext.get().expoCamera.camera.zoom = amount;
            success("Set camera zoom to [CYAN]" + amount);
        } else {
            error("You are not ingame.");
        }
    }

}
