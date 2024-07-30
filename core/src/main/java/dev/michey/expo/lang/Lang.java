package dev.michey.expo.lang;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.render.ui.notification.UINotificationPiece;

import java.util.HashMap;
import java.util.LinkedList;

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

        String[] fullString = fh.readString("UTF-8").split("\r\n");

        int totalLines = 0;
        int validLines = 0;
        HashMap<String, String> translations = new HashMap<>();

        for(String line : fullString) {
            if(line.isEmpty()) continue;
            if(line.startsWith("#")) continue;
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

    public static UINotificationPiece[] ntp(String key, Object... tokens) {
        String base = str(key, tokens);
        LinkedList<int[]> bounds = new LinkedList<>();

        int lastIndex = 0;

        while(true) {
            int index = base.indexOf("[piece,", lastIndex);

            if(index == -1) {
                break;
            }

            lastIndex = index + 1;
            bounds.add(new int[] {index, index + 14}); // [piece,HEXCLR]
        }

        LinkedList<UINotificationPiece> pieces = new LinkedList<>();

        for(int i = 0 ; i < bounds.size(); i++) {
            int[] pieceBoundaries = bounds.get(i);

            String str;
            String hexColor = base.substring(pieceBoundaries[0] + 7, pieceBoundaries[0] + 13);

            if(i == bounds.size() - 1) {
                str = base.substring(pieceBoundaries[1]);
            } else {
                str = base.substring(pieceBoundaries[1], bounds.get(i + 1)[0]);
            }

            pieces.add(new UINotificationPiece(str, Color.valueOf(hexColor)));
        }

        return pieces.toArray(new UINotificationPiece[0]);
    }

    public static String str(String key) {
        Lang l = Lang.get();

        if(l.activeLangCode == null) {
            return "<INVALID LANG>";
        }

        String value = l.translationDictionary.get(l.activeLangCode).get(key);

        if(value == null && !l.activeLangCode.equals("en")) {
            // Grab from default language "en"
            value = l.translationDictionary.get("en").get(key);
        }

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
