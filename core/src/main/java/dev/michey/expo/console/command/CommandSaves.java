package dev.michey.expo.console.command;

import dev.michey.expo.server.fs.world.WorldSaveFile;

import java.io.File;
import java.text.SimpleDateFormat;

public class CommandSaves extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/saves";
    }

    @Override
    public String getCommandDescription() {
        return "Displays the saves file structure";
    }

    @Override
    public String getCommandSyntax() {
        return "/saves";
    }

    @Override
    public void executeCommand(String[] args) {
        File savesFolder = new File(WorldSaveFile.getPathSaveFolder());
        File[] folders = savesFolder.listFiles();

        if(folders == null) {
            error("Saves folder doesn't exist.");
        } else {
            success("Listing all existing saves below:");
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            long now = System.currentTimeMillis();

            for(File folder : folders) {
                long diff = now - folder.lastModified();
                String agoString;

                if(diff > 1000) {
                    long minute = (diff / (1000 * 60)) % 60;
                    long hour = (diff / (1000 * 60 * 60)) % 24;
                    long days = (diff / (1000 * 60 * 60 * 24));
                    long second = (diff / 1000) % 60;

                    String daysString = days > 0 ? days + " day" + (days == 1 ? " " : "s ") : "";
                    String hoursString = hour > 0 ? hour + " hour" + (hour == 1 ? " " : "s ") : "";
                    String minutesString = minute > 0 ? minute + " minute" + (minute == 1 ? " " : "s ") : "";
                    String secondString = second > 0 ? second + " second" + (second == 1 ? " " : "s ") : "";

                    agoString = daysString + hoursString + minutesString + secondString + "ago";
                } else {
                    agoString = "just now";
                }

                message("  [YELLOW]" + folder.getName() + " [GRAY]- [ORANGE]" + sdf.format(folder.lastModified()) + " [GRAY]- [ORANGE](" + agoString + ")");
            }
        }
    }

}
