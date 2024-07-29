package dev.michey.expo.jsonextractor;

import dev.michey.expo.log.ExpoLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class JsonExtractor {

    public static void main(String[] args) {
        File f = new File(ExpoLogger.getLocalPath() + File.separator + "assets_shared" + File.separator + "items.json");
        File writeTo = new File(ExpoLogger.getLocalPath() + File.separator + "json-extractor" + File.separator + "output.txt");

        try {
            writeTo.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            JSONObject object = new JSONObject(new String(Files.readAllBytes(f.toPath())));
            JSONArray database = object.getJSONArray("database");

            for(int i = 0; i < database.length(); i++) {
                JSONObject singleObject = database.getJSONObject(i);
                String str = "item." + singleObject.getString("identifier").substring(5) + "=" + singleObject.getString("displayName") + System.lineSeparator();
                Files.write(writeTo.toPath(), str.getBytes(), StandardOpenOption.APPEND);

                // modify existing .json if needed
                singleObject.remove("displayName");
            }

            ExpoLogger.log(object.toString(4));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
