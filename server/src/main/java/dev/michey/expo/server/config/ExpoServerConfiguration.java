package dev.michey.expo.server.config;

import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoServerConfiguration {

    /** Disk file name */
    private static final String CONFIG_FILE = "serverConfig.json";

    /** Configuration properties */
    private String serverName = "default-server";
    private boolean consoleInput = true;
    private int maxPlayers = 10;
    private int serverPort = ExpoShared.DEFAULT_EXPO_SERVER_PORT;
    private int writeBufferSize = ExpoShared.DEFAULT_WRITE_BUFFER_SIZE;
    private int objectBufferSize = ExpoShared.DEFAULT_OBJECT_BUFFER_SIZE;
    private int serverTps = ExpoShared.DEFAULT_DEDICATED_TICK_RATE;
    private boolean enableWhitelist = false;
    private String worldName = "default-world";
    private int unloadChunksAfter = 5000;
    private int saveChunksAfter = 180000;
    private String password = "";
    private boolean authPlayers = true;
    private String steamWebApiKey = "";
    private int maxPlayerViewDistanceX = 11;
    private int maxPlayerViewDistanceY = 11;
    private boolean trackPerformance = false;

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
                    case "password" -> password = fileAsJson.getString("password");
                    case "authPlayers" -> authPlayers = fileAsJson.getBoolean("authPlayers");
                    case "steamWebApiKey" -> steamWebApiKey = fileAsJson.getString("steamWebApiKey");
                    case "maxPlayerViewDistanceX" -> maxPlayerViewDistanceX = fileAsJson.getInt("maxPlayerViewDistanceX");
                    case "maxPlayerViewDistanceY" -> maxPlayerViewDistanceY = fileAsJson.getInt("maxPlayerViewDistanceY");
                    case "trackPerformance" -> trackPerformance = fileAsJson.getBoolean("trackPerformance");
                }
            }

            log("Verifying keys from config file");
            {
                // VIEW DISTANCE CHECK
                if(maxPlayerViewDistanceX < 3) {
                    maxPlayerViewDistanceX = 3;
                    log("Setting maxPlayerViewDistanceX to '3' as it's the required minimum view distance.");
                }
                if(maxPlayerViewDistanceX % 2 == 0) {
                    maxPlayerViewDistanceX++;
                    log("Setting maxPlayerViewDistanceX to '" + maxPlayerViewDistanceX + "' as the parsed number was not an odd number.");
                }

                if(maxPlayerViewDistanceY < 3) {
                    maxPlayerViewDistanceY = 3;
                    log("Setting maxPlayerViewDistanceY to '3' as it's the required minimum view distance.");
                }
                if(maxPlayerViewDistanceY % 2 == 0) {
                    maxPlayerViewDistanceY++;
                    log("Setting maxPlayerViewDistanceY to '" + maxPlayerViewDistanceY + "' as the parsed number was not an odd number.");
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
                .put("password", password)
                .put("authPlayers", authPlayers)
                .put("steamWebApiKey", steamWebApiKey)
                .put("maxPlayerViewDistanceX", maxPlayerViewDistanceX)
                .put("maxPlayerViewDistanceY", maxPlayerViewDistanceY)
                .put("trackPerformance", trackPerformance)
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

    public String getPassword() {
        return password;
    }

    public boolean isAuthPlayersEnabled() {
        return authPlayers;
    }

    public String getSteamWebApiKey() {
        return steamWebApiKey;
    }

    public int getMaxPlayerViewDistanceX() {
        return maxPlayerViewDistanceX;
    }

    public int getMaxPlayerViewDistanceY() {
        return maxPlayerViewDistanceY;
    }

    public boolean isTrackPerformance() {
        return trackPerformance;
    }

    public static ExpoServerConfiguration get() {
        return INSTANCE;
    }

}
