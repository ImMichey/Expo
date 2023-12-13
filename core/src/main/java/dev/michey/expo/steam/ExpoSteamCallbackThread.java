package dev.michey.expo.steam;

import com.codedisaster.steamworks.SteamAPI;

public class ExpoSteamCallbackThread extends Thread {

    private static final int UPDATES_PER_SECOND = 60;
    private static final long SLEEP_PER_CALLBACK = 1000L / UPDATES_PER_SECOND;

    @Override
    public void run() {
        while(!isInterrupted()) {
            SteamAPI.runCallbacks();

            try {
                Thread.sleep(SLEEP_PER_CALLBACK);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
