package dev.michey.expo.util;

public class ExpoTime {

    /** Ingame world time */
    public static final int WORLD_MINUTE_DURATION = 1;
    public static final int WORLD_HOUR_DURATION = WORLD_MINUTE_DURATION * 60;
    public static final int WORLD_DAY_DURATION = WORLD_HOUR_DURATION * 24;
    public static final int WORLD_CYCLE_DURATION = WORLD_DAY_DURATION;

    public static final int SUNRISE     = worldDurationHours(6);
    public static final int DAY         = worldDurationHours(8);
    public static final int SUNSET      = worldDurationHours(20);
    public static final int NIGHT       = worldDurationHours(22);
    public static final int MIDNIGHT    = worldDurationHours(2);

    public static int worldDurationHours(int hours) {
        return hours * WORLD_HOUR_DURATION;
    }

    public static int worldDuration(int days, int hours, int minutes) {
        return days * WORLD_DAY_DURATION + hours * WORLD_HOUR_DURATION + minutes * WORLD_MINUTE_DURATION;
    }

    public static boolean isNight(float time) {
        return time >= NIGHT || time < SUNRISE;
    }

    public static boolean isDay(float time) {
        return time >= DAY && time < SUNSET;
    }

    public static String worldTimeString(float worldTime) {
        int hours = (int) (worldTime / WORLD_HOUR_DURATION);
        int minutes = (int) (worldTime - worldDurationHours(hours));

        String str = hours < 10 ? "0" : "";
        str += hours + ":";
        str += minutes < 10 ? "0" : "";
        str += minutes;

        return str;
    }

    /** General purpose time */
    public static class RealWorld {

        public static long hours(int hours) {
            return 1000L * 60L * 60L * hours;
        }

        public static long minutes(int minutes) {
            return 1000L * 60L * minutes;
        }

        public static long seconds(int seconds) {
            return 1000L * seconds;
        }

    }

}