package dev.michey.expo.io;

public class ExpoFile {

    public FileType fileType;
    public String name;
    public Object payload; // only supported types: String & JSONObject
    public String path; // can be null

    public ExpoFile(FileType fileType, String name) {
        this.fileType = fileType;
        this.name = name;
    }

    public ExpoFile(FileType fileType, String name, Object payload) {
        this(fileType, name);
        this.payload = payload;
    }

    public ExpoFile(FileType fileType, String name, Object payload, String path) {
        this(fileType, name, payload);
        this.path = path;
    }

    public enum FileType {
        FOLDER, FILE, CONFIG
    }

}