package dev.michey.expo.util;

import org.json.JSONArray;

public class JsonConverter {

    public static String[] pullStrings(JSONArray array) {
        String[] strings = new String[array.length()];
        for(int i = 0; i < array.length(); i++) strings[i] = array.getString(i);
        return strings;
    }

}
