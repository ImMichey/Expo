package dev.michey.expo.server.config;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoServerConfiguration {

    /** Disk file name */
    private final String CONFIG_FILE = "serverConfig.json";

    /** Configuration properties */
    private String serverName = "default-server";
    private boolean consoleInput = true;
    private int maxPlayers = 10;
    private int serverPort = ExpoShared.DEFAULT_EXPO_SERVER_PORT;
    private int writeBufferSize = 32768;
    private int objectBufferSize = 8192;
    private int serverTps = ExpoShared.DEFAULT_SERVER_TICK_RATE;
    private boolean enableWhitelist = false;
    private String worldName = "default-world";
    private int unloadChunksAfter = 5000;
    private int saveChunksAfter = 180000;

    /** Singleton */
    private static ExpoServerConfiguration INSTANCE;

    public boolean loadContents() {
        log("Creating ExpoServerConfiguration");
        File configFile = new File(ExpoLogger.getLocalPath() + File.separator + CONFIG_FILE);

        if(!configFile.exists()) {
            log(CONFIG_FILE + " file does not exist, creating one with defaults");
            boolean created = false;

            try {
                created = configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(!created) {
                log("Failed to create " + CONFIG_FILE + " file");
                return false;
            }

            boolean written = false;

            try {
                Files.writeString(configFile.toPath(), jsonAsString(configAsJson()), StandardOpenOption.WRITE);
                written = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(!written) {
                log("Failed to write to " + CONFIG_FILE + " file");
                return false;
            }
        } else {
            log(CONFIG_FILE + " file exists, attempting to read from disk");
            String fileAsString = null;

            try {
                fileAsString = Files.readString(configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(fileAsString == null) {
                log("Failed to read from " + CONFIG_FILE + " file");
                return false;
            }

            JSONObject fileAsJson = null;

            try {
                fileAsJson = new JSONObject(fileAsString);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(fileAsJson == null) {
                log("Failed to parse config file to json");
                return false;
            }

            boolean requiresUpdate = configAsJson().length() != fileAsJson.length(); // If key length differs

            if(!requiresUpdate) {
                JSONObject defaultJson = configAsJson();

                for(String key : defaultJson.keySet()) {
                    if(!fileAsJson.has(key)) {
                        requiresUpdate = true;
                        break;
                    }
                }
            }

            log("Reading keys from config file");
            for(String key : fileAsJson.keySet()) {
                log("\t-> " + key + ": " + fileAsJson.get(key));
                switch(key) {
                    case "serverName" -> serverName = fileAsJson.getString("serverName");
                    case "consoleInput" -> consoleInput = fileAsJson.getBoolean("consoleInput");
                    case "maxPlayers" -> maxPlayers = fileAsJson.getInt("maxPlayers");
                    case "serverPort" -> serverPort = fileAsJson.getInt("serverPort");
                    case "writeBufferSize" -> writeBufferSize = fileAsJson.getInt("writeBufferSize");
                    case "objectBufferSize" -> objectBufferSize = fileAsJson.getInt("objectBufferSize");
                    case "serverTps" -> serverTps = fileAsJson.getInt("serverTps");
                    case "enableWhitelist" -> enableWhitelist = fileAsJson.getBoolean("enableWhitelist");
                    case "worldName" -> worldName = fileAsJson.getString("worldName");
                    case "unloadChunksAfter" -> unloadChunksAfter = fileAsJson.getInt("unloadChunksAfter");
                    case "saveChunksAfter" -> saveChunksAfter = fileAsJson.getInt("saveChunksAfter");
                }
            }

            if(requiresUpdate) {
                log("Config file requires update, writing to disk");

                try {
                    Files.writeString(configFile.toPath(), jsonAsString(configAsJson()), StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                    log("Failed to write updated config to " + CONFIG_FILE + " file");
                    return false;
                }
            }
        }

        INSTANCE = this;
        return true;
    }

    private String jsonAsString(JSONObject json) {
        return json.toString(4);
    }

    private JSONObject configAsJson() {
        return new JSONObject()
                .put("serverName", serverName)
                .put("consoleInput", consoleInput)
                .put("maxPlayers", maxPlayers)
                .put("serverPort", serverPort)
                .put("writeBufferSize", writeBufferSize)
                .put("objectBufferSize", objectBufferSize)
                .put("serverTps", serverTps)
                .put("enableWhitelist", enableWhitelist)
                .put("worldName", worldName)
                .put("unloadChunksAfter", unloadChunksAfter)
                .put("saveChunksAfter", saveChunksAfter)
                ;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getServerName() {
        return serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean isConsoleInput() {
        return consoleInput;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public int getObjectBufferSize() {
        return objectBufferSize;
    }

    public int getServerTps() {
        return serverTps;
    }

    public boolean isWhitelistEnabled() {
        return enableWhitelist;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getUnloadChunksAfter() {
        return unloadChunksAfter;
    }

    public int getSaveChunksAfter() {
        return saveChunksAfter;
    }

    public static ExpoServerConfiguration get() {
        return INSTANCE;
    }

}
