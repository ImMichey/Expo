package dev.michey.expo.server.fs.whitelist;

import dev.michey.expo.log.ExpoLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerWhitelist {

    /** Singleton */
    private static ServerWhitelist INSTANCE;

    /** Disk file name */
    private final String WHITELIST_FILE = "whitelist.txt";
    private File file;

    /** Whitelisted player container */
    private ArrayList<String> whitelistedPlayers;

    public boolean load() {
        log("Creating ServerWhitelist");
        file = new File(ExpoLogger.getLocalPath() + File.separator + WHITELIST_FILE);

        if(!file.exists()) {
            log(WHITELIST_FILE + " file does not exist, creating one");
            boolean created = false;

            try {
                created = file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(!created) {
                log("Failed to create " + WHITELIST_FILE + " file");
                return false;
            }

            whitelistedPlayers = new ArrayList<>();
        } else {
            log(WHITELIST_FILE + " file exists, attempting to read from disk");
            ArrayList<String> allLines = null;

            try {
                allLines = (ArrayList<String>) Files.readAllLines(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(allLines == null) {
                log("Failed to read from " + WHITELIST_FILE + " file");
                return false;
            }

            log("Read " + allLines.size() + " entr" + (allLines.size() == 1 ? "y" : "ies") + " from " + WHITELIST_FILE + " file:");
            for(String name : allLines) {
                log("    -> " + name);
            }
            whitelistedPlayers = allLines;
        }

        INSTANCE = this;
        return true;
    }

    public void addWhitelistedPlayer(String username) {
        boolean requiresChanges = !whitelistedPlayers.contains(username);

        if(requiresChanges) {
            log("Adding whitelisted player: " + username);
            whitelistedPlayers.add(username);

            // Write to file in separate thread to not block main thread
            new Thread("Whitelist-Add-Thread") {

                @Override
                public void run() {
                    log("Writing update to " + WHITELIST_FILE);

                    try {
                        Files.writeString(file.toPath(), username + System.lineSeparator(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        log("Failed to write whitelist update to " + WHITELIST_FILE);
                        e.printStackTrace();
                    }
                }

            }.start();
        }
    }

    public void removeWhitelistedPlayer(String username) {
        boolean requiresChanges = whitelistedPlayers.contains(username);

        if(requiresChanges) {
            log("Removing whitelisted player: " + username);
            whitelistedPlayers.remove(username);

            // Write to file in separate thread to not block main thread
            new Thread("Whitelist-Remove-Thread") {

                @Override
                public void run() {
                    log("Writing update to " + WHITELIST_FILE);
                    String whitelistString = constructWhitelistString();

                    try {
                        Files.writeString(file.toPath(), whitelistString, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        log("Failed to write whitelist update to " + WHITELIST_FILE);
                        e.printStackTrace();
                    }
                }

            }.start();
        }
    }

    public boolean isPlayerWhitelisted(String username) {
        return whitelistedPlayers.contains(username);
    }

    public boolean isPlayerWhitelisted(long steamId) {
        return whitelistedPlayers.contains(String.valueOf(steamId));
    }

    private String constructWhitelistString() {
        StringBuilder builder = new StringBuilder();

        for(String user : whitelistedPlayers) {
            builder.append(user);
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    public static ServerWhitelist get() {
        return INSTANCE;
    }

}
