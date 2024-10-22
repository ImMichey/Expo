package dev.michey.expo.io;

import dev.michey.expo.log.ExpoLogger;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class ExpoResourceFile {

    private final String filePath;
    private final String resourcePath;
    protected final File file;

    private byte[] data;

    public ExpoResourceFile(String path) {
        this(path, null);
    }

    /**
     * This class helps to dynamically load internal resource files and store them locally in the run directory,
     * so they don't need to be added externally after exporting.
     */
    public ExpoResourceFile(String path, String resourcePath) {
        this.filePath = path;
        this.resourcePath = resourcePath;
        file = new File(ExpoLogger.getLocalPath() + File.separator + path);
    }

    /**
     * This loads the desired file contents from the resource path into the data byte array.
     * This should be done once at initialization to load its default values.
     */
    public void loadConfigFromResources() {
        if(resourcePath == null) return;
        InputStream in = ExpoResourceFile.class.getClassLoader().getResourceAsStream(resourcePath);

        if(in == null) {
            ExpoLogger.logerr("Couldn't load file '" + filePath + "' as it probably doesn't exist.");
            return;
        }

        try {
            data = IOUtils.toByteArray(in);
        } catch (IOException e) {
            ExpoLogger.logerr("Error while loading file '" + filePath + "' from resource path:");
            e.printStackTrace();
        }
    }

    /**
     * Saves the data stored in memory to its respective file and returns whether it was required or not.
     */
    public void ensureFileExistence() {
        if(!file.exists()) {
            saveToDisk();
        }
    }

    public void saveToDisk() {
        file.getParentFile().mkdirs();

        try {
            Files.write(file.toPath(), data, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            ExpoLogger.logerr("Failed to save ExpoResourceFile '" + filePath + "' to disk:");
            e.printStackTrace();
        }
    }

    // ===== Static helpers =====

    public static boolean write(File f, String str) {
        try {
            Files.writeString(f.toPath(), str, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
