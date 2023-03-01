package dev.michey.expo.console.command;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.command.CommandSyntaxException;

public class Client_Volume extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/cl_volume";
    }

    @Override
    public String getCommandDescription() {
        return "Sets the client's game master volume";
    }

    @Override
    public String getCommandSyntax() {
        return "/cl_volume";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        float volume = MathUtils.clamp(parseF(args, 1), 0.0f, 1.0f);
        AudioEngine.get().setMasterVolume(volume);
        success("Set game master volume to [CYAN]" + volume);
    }

}
