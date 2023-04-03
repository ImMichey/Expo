package dev.michey.expo.console.command;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.Expo;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.util.ClientStatic;

public class CommandUsername extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/username";
    }

    @Override
    public String getCommandDescription() {
        return "Sets the player's username";
    }

    @Override
    public String getCommandSyntax() {
        return "/username <name>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            String username = parseString(args, 1);

            if(username.length() < 3) {
                error("The username [CYAN]" + username + " [RED]is too short.");
                return;
            }

            if(username.equals("<random>")) {
                ClientStatic.PLAYER_USERNAME = "debug-" + MathUtils.random(0, 10000);
            } else {
                ClientStatic.PLAYER_USERNAME = username;
            }

            success("Set client username to [CYAN]" + username);
        } else {
            error("You are already ingame.");
        }
    }

}