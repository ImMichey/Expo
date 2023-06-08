package dev.michey.expo.util;

import dev.michey.expo.io.ExpoFile;
import dev.michey.expo.io.ExpoFileCreator;
import dev.michey.expo.log.ExpoLogger;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static dev.michey.expo.log.ExpoLogger.log;

public class GameSettings {

    private static GameSettings INSTANCE;

    // Window variables
    public int preferredWidth = 1280;
    public int preferredHeight = 720;
    public boolean fullscreen = false;
    public boolean vsync = false;
    public int fpsCap = 500;

    // Debug variables
    public boolean enableDebugGL = false;
    public boolean enableDebugImGui = false;

    // Audio variables
    public float masterVolume = 1.0f;
    public float sfxVolume = 1.0f;
    public float ambientVolume = 1.0f;
    public float musicVolume = 1.0f;

    // UI variables
    public int uiScale = 2;

    // Graphic variables
    public boolean enableBlur = true;
    public boolean enableShadows = true;
    public boolean enableWater = true;
    public boolean enableParticles = true;
    public int zoomLevel = 0;

    public GameSettings() {
        var result = ExpoFileCreator.createFileStructure(new ExpoFile(ExpoFile.FileType.CONFIG, "settings.json", asJson()));

        if(!result.key) {
            log("Failed to create GameSettings file.");
            System.exit(0);
        } else {
            if(result.value[0] != null) {
                applyFromFile((JSONObject) result.value[0]);
                log("Applied GameSettings from file. " + preferredWidth + "x" + preferredHeight);
            }
        }

        INSTANCE = this;
    }

    public void flushUpdates() {
        ExpoLogger.log("Flushing GameSettings updates to file.");

        try {
            Files.writeString(new File(ExpoLogger.getLocalPath() + File.separator + "settings.json").toPath(), asJson().toString(4), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameSettings get() {
        return INSTANCE;
    }

    private JSONObject asJson() {
        JSONObject debugJson = new JSONObject()
                .put("enableDebugGL", enableDebugGL)
                .put("enableDebugImGui", enableDebugImGui)
                ;

        JSONObject audioJson = new JSONObject()
                .put("masterVolume", masterVolume)
                .put("sfxVolume", sfxVolume)
                .put("ambientVolume", ambientVolume)
                .put("musicVolume", musicVolume)
                ;

        JSONObject graphicJson = new JSONObject()
                .put("enableBlur", enableBlur)
                .put("enableShadows", enableShadows)
                .put("enableWater", enableWater)
                .put("enableParticles", enableParticles)
                .put("zoomLevel", zoomLevel)
                ;

        return new JSONObject()
                .put("preferredWidth", preferredWidth)
                .put("preferredHeight", preferredHeight)
                .put("fpsCap", fpsCap)
                .put("fullscreen", fullscreen)
                .put("vsync", vsync)
                .put("debug", debugJson)
                .put("audio", audioJson)
                .put("uiScale", uiScale)
                .put("graphics", graphicJson)
                ;
    }

    private void applyFromFile(JSONObject jsonSettings) {
        preferredWidth = jsonSettings.getInt("preferredWidth");
        preferredHeight = jsonSettings.getInt("preferredHeight");
        fpsCap = jsonSettings.getInt("fpsCap");
        fullscreen = jsonSettings.getBoolean("fullscreen");
        vsync = jsonSettings.getBoolean("vsync");

        JSONObject debugJson = jsonSettings.getJSONObject("debug");
        enableDebugGL = debugJson.getBoolean("enableDebugGL");
        enableDebugImGui = debugJson.getBoolean("enableDebugImGui");

        JSONObject audioJson = jsonSettings.getJSONObject("audio");
        masterVolume = audioJson.getFloat("masterVolume");
        sfxVolume = audioJson.getFloat("sfxVolume");
        ambientVolume = audioJson.getFloat("ambientVolume");
        musicVolume = audioJson.getFloat("musicVolume");

        uiScale = jsonSettings.getInt("uiScale");

        JSONObject graphicJson = jsonSettings.getJSONObject("graphics");
        enableBlur = graphicJson.getBoolean("enableBlur");
        enableShadows = graphicJson.getBoolean("enableShadows");
        enableWater = graphicJson.getBoolean("enableWater");
        enableParticles = graphicJson.getBoolean("enableParticles");
        zoomLevel = graphicJson.getInt("zoomLevel");
    }

    @Override
    public String toString() {
        return asJson().toString(4);
    }

}