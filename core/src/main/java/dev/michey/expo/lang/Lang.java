package dev.michey.expo.lang;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import dev.michey.expo.log.ExpoLogger;

import java.util.HashMap;

public class Lang {

    private static Lang instance;

    private final HashMap<String, HashMap<String, String>> translationDictionary;
    private String activeLangCode;

    public Lang() {
        translationDictionary = new HashMap<>();
        instance = this;
    }

    public void load(String langCode) {
        ExpoLogger.log("Loading translations for '" + langCode + "'");
        FileHandle fh = Gdx.files.internal("lang/" + langCode + ".txt");

        if(!fh.exists()) {
            ExpoLogger.logerr("Couldn't load translations for '" + langCode + "' as the file does not exist.");
            return;
        }

        String[] fullString = fh.readString().split("\r\n");

        int totalLines = 0;
        int validLines = 0;
        HashMap<String, String> translations = new HashMap<>();

        for(String line : fullString) {
            if(line.isEmpty()) continue;
            totalLines++;

            int substringChar = line.indexOf("=");

            if(substringChar != -1) {
                String key = line.substring(0, substringChar);
                String value = line.substring(substringChar + 1);
                translations.put(key, value);
                validLines++;
            }
        }

        if(validLines > 0) {
            ExpoLogger.log("Registered " + validLines + "/" + totalLines + " valid/total translations for lang code '" + langCode + "'.");
            translationDictionary.put(langCode, translations);
        } else {
            ExpoLogger.logerr("Couldn't register translations for lang code '" + langCode + "' as there are none valid.");
        }
    }

    public void setActiveLangCode(String langCode) {
        ExpoLogger.log("Setting active lang code to '" + langCode + "'");
        this.activeLangCode = langCode;
    }

    public static String str(String key) {
        Lang l = Lang.get();

        if(l.activeLangCode == null) {
            return "<INVALID LANG>";
        }

        String value = l.translationDictionary.get(l.activeLangCode).get(key);
        return value == null ? "<MISSING STRING>" : value;
    }

    public static String str(String key, Object... tokens) {
        String grab = str(key);
        return String.format(grab, tokens);
    }

    public static Lang get() {
        return instance;
    }

}
