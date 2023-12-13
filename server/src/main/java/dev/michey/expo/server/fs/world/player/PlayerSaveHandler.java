package dev.michey.expo.server.fs.world.player;

import dev.michey.expo.io.ExpoConfig;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class PlayerSaveHandler {

    private final String folderPath;
    private final ConcurrentHashMap<String, PlayerSaveFile> saveFileCache;

    public PlayerSaveHandler(String folderPath) {
        this.folderPath = folderPath;
        saveFileCache = new ConcurrentHashMap<>();
    }

    /** Loads and retrieves the player save file and returns it as a class. Should be used asynchronous. */
    public PlayerSaveFile loadAndGetPlayerFile(String username, long steamId) {
        String useSaveString = steamId == -1 ? username : String.valueOf(steamId);
        log("Loading PlayerSaveFile for " + useSaveString);
        PlayerSaveFile psf = saveFileCache.get(useSaveString);
        if(psf != null) return psf; // retrieve from cached map

        String filePath = folderPath + File.separator + useSaveString + ".json";
        ExpoConfig playerSave = new ExpoConfig(filePath, PlayerSaveFile.DEFAULT_PROPERTIES);
        playerSave.load(); // load ExpoConfig

        PlayerSaveFile newPsf = new PlayerSaveFile(playerSave); // initialize PlayerSaveFile
        saveFileCache.put(useSaveString, newPsf);

        return newPsf;
    }

    public void saveAll() {
        for(PlayerSaveFile psf : saveFileCache.values()) {
            psf.getHandler().onTermination();
        }
    }

}
