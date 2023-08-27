package dev.michey.expo.io;

import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.util.Pair;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoFileCreator {

    public static Pair<Boolean, Object[]> createFileStructure(String basePath, ExpoFile... expoFiles) {
        log("Creating file structure for " + expoFiles.length + " file(s) with base path: " + basePath);
        int iterate = expoFiles.length;
        Object[] objects = new Object[iterate];
        int successful = 0;

        for(int i = 0; i < expoFiles.length; i++) {
            ExpoFile ef = expoFiles[i];
            int oldSuccessful = successful;
            File f = new File(ef.path == null ? (((basePath == null || basePath.isEmpty()) ? "" : (basePath + File.separator)) + ef.name) : ef.path);
            log("    " + f.getAbsolutePath());

            if(ef.fileType == ExpoFile.FileType.FOLDER) {
                f.mkdir();
            } else {
                boolean _c = false;

                try {
                    _c = f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // file was newly created + has a payload to write from
                if(_c && ef.payload != null) {
                    String writeFrom = null;

                    if(ef.payload instanceof String s) {
                        writeFrom = s;
                    } else if(ef.payload instanceof JSONObject j) {
                        writeFrom = j.toString(4);
                    }

                    if(writeFrom != null) {
                        try {
                            Files.writeString(f.toPath(), writeFrom, StandardOpenOption.CREATE);
                        } catch (IOException e) {
                            e.printStackTrace();
                            successful--;
                        }
                    } else {
                        successful--;
                    }
                } else {
                    // file is old, check if it's a config type
                    if(ef.fileType == ExpoFile.FileType.CONFIG) {
                        // compare differences and update if needed
                        if(ef.payload instanceof JSONObject j) {
                            JSONObject updatedConfig = new JSONObject();
                            JSONObject oldConfig = null;

                            try {
                                oldConfig = new JSONObject(Files.readString(f.toPath()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // old config is invalid/cannot be read, copy defaults
                            if(oldConfig == null) {
                                try {
                                    Files.writeString(f.toPath(), j.toString(4), StandardOpenOption.CREATE);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    successful--;
                                }
                            } else {
                                boolean requiresChanges = false;

                                for(String key : j.keySet()) {
                                    Object o = j.get(key);

                                    if(oldConfig.has(key)) {
                                        Object _check = oldConfig.get(key);

                                        if(_check instanceof JSONObject _checkObj) {
                                            JSONObject compareWith = (JSONObject) o;

                                            for(String _key : compareWith.keySet()) {
                                                if(_checkObj.has(_key)) {
                                                    compareWith.put(_key, _checkObj.get(_key));
                                                } else {
                                                    requiresChanges = true;
                                                }
                                            }
                                        } else {
                                            o = oldConfig.get(key);
                                        }
                                    } else {
                                        requiresChanges = true;
                                    }

                                    updatedConfig.put(key, o);
                                }

                                objects[i] = updatedConfig;

                                if(requiresChanges) {
                                    try {
                                        Files.writeString(f.toPath(), updatedConfig.toString(4), StandardOpenOption.TRUNCATE_EXISTING);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        successful--;
                                    }
                                }
                            }
                        } else {
                            successful--;
                        }
                    }
                }
            }

            if(f.exists()) successful++;

            if(oldSuccessful == successful) {
                log("      ...failed operation");
            }
        }

        return new Pair<>(iterate == successful, objects);
    }

    public static Pair<Boolean, Object[]> createFileStructure(ExpoFile... expoFiles) {
        return createFileStructure(ExpoLogger.getLocalPath(), expoFiles);
    }

}
