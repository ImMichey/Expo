package dev.michey.expo.console.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.log.ExpoLogger;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class CommandOpenlog extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/openlog";
    }

    @Override
    public String getCommandDescription() {
        return "Opens the game's current log file";
    }

    @Override
    public String getCommandSyntax() {
        return "/openlog";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        success("Opening... " + ExpoLogger.LOG_FILE_ABSOLUTE_PATH);

        try {
            Desktop.getDesktop().open(new File(ExpoLogger.LOG_FILE_ABSOLUTE_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
