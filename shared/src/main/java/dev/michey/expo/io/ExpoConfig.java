package dev.michey.expo.io;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoConfig {

    /** File structure */
    private final String filePath;
    private final File file;

    /** Config data */
    public final HashMap<String, Object> properties;
    public boolean fileJustCreated;
    private boolean markedForSave;

    public ExpoConfig(String filePath, HashMap<String, Object> defaultProperties) {
        this.filePath = filePath;
        this.properties = new HashMap<>();
        this.properties.putAll(defaultProperties);
        file = new File(filePath);
    }

    public void load() {
        log("Loading config file " + filePath);

        if(!file.exists()) {
            log("Config file does not exist yet, creating + saving default properties");
            fileJustCreated = true;

            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            save();
        } else {
            log("Config file does exist, attempting to read already existing properties");
            String readString = null;
            boolean finalUpdate = false;

            try {
                readString = Files.readString(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(readString != null && readString.length() >= 2) { // {} is bare minimum
                JSONObject toJson = null;

                try {
                    toJson = new JSONObject(readString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if(toJson != null) {
                    boolean requiresUpdate = false;

                    for(String key : properties.keySet()) {
                        if(toJson.has(key)) {
                            properties.put(key, toJson.get(key));
                        } else {
                            requiresUpdate = true;
                        }
                    }

                    if(requiresUpdate) {
                        log("Existing config file is incomplete, writing missing values to file.");
                        save();
                    } else {
                        log("Successfully loaded config file without any errors/required completions.");
                    }
                } else {
                    finalUpdate = true;
                }
            } else {
                finalUpdate = true;
            }

            if(finalUpdate) {
                log("Existing config file is invalid/faulty, writing default values.");
                save();
            }
        }
    }

    /** Saves the cached properties to the disk file. Returns whether the operation was successful or not. */
    public boolean save() {
        boolean successfullySaved = false;

        try {
            Files.writeString(file.toPath(), propertiesToJsonString(), StandardOpenOption.TRUNCATE_EXISTING);
            successfullySaved = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return successfullySaved;
    }

    /** Called when the config file is being deleted from the internal program cache. */
    public void onTermination() {
        if(markedForSave) {
            save();
            markedForSave = false;
        }
    }

    private String propertiesToJsonString() {
        JSONObject object = new JSONObject();

        for(Map.Entry<String, Object> key : properties.entrySet()) {
            object.put(key.getKey(), key.getValue());
        }

        return object.toString(4);
    }

    /** Cache a new updated value. */
    public void update(String k, Object store) {
        properties.put(k, store);
        markedForSave = true;
    }

    /** Retrieve a config value. */
    public Object get(String k) {
        return properties.get(k);
    }

}
