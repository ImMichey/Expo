package dev.michey.expo.console.command;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.command.CommandSyntaxException;

public class CommandFps extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/fps";
    }

    @Override
    public String getCommandDescription() {
        return "Caps the game FPS to the specified amount";
    }

    @Override
    public String getCommandSyntax() {
        return "/fps <amount>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        int amount = parseI(args, 1);
        Gdx.graphics.setForegroundFPS(amount);
        success("Capped game FPS to [CYAN]" + amount);
    }

}