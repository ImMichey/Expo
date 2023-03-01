package dev.michey.expo.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExpoLogger {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss.SSS");
    private static final StringBuilder builder = new StringBuilder();

    private static final Object LOG_LOCK = new Object();

    public static void log(String s) {
        builder.append('[');
        builder.append(sdf.format(new Date()));
        builder.append(']');
        builder.append(' ');
        builder.append(s);
        synchronized (LOG_LOCK) {
            System.out.println(builder);
        }
        builder.setLength(0);
    }

    public static void logc(String s) {
        builder.append('[');
        builder.append(sdf.format(new Date()));
        builder.append('*');
        builder.append(']');
        builder.append(' ');
        builder.append(s);
        synchronized (LOG_LOCK) {
            System.out.println(builder);
        }
        builder.setLength(0);
    }

    /** Enables logging to console & file simultaneously */
    public static void enableDualLogging(String logFolderName) {
        log("Enabling dual logging (folder: " + logFolderName + ")");

        // Current execution path
        String currentPath = getLocalPath();

        TreeOutputStream tos = new TreeOutputStream();
        tos.addStream(System.out); // add console to tree

        String logFolderPath = currentPath + File.separator + logFolderName;
        new File(logFolderPath).mkdir(); // creates the log folder

        String logFileName = "log-" + System.currentTimeMillis() + ".txt";
        File logFile = new File(logFolderPath + File.separator + logFileName);
        boolean logFileCreationSuccessful = false;

        try {
            logFileCreationSuccessful = logFile.createNewFile(); // creates the log file within the log folder
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(logFileCreationSuccessful) {
            try {
                tos.addStream(new PrintStream(logFile)); // add file to tree

                PrintStream finalStream = new PrintStream(tos);
                System.setOut(finalStream);
                System.setErr(finalStream); // to catch errors on file as well

                log("Logging now to file: " + logFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            log("Could not enable dual logging (file could not be created)");
        }
    }

    public static String getLocalPath() {
        return Paths.get(".").toAbsolutePath().normalize().toString();
    }

}
