package dev.michey.expo.server.util;

import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.arch.ExpoServerDedicated;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Location;

import java.util.Locale;

public class ServerUtils {

    /*
     *          90°
     *          I
     *          I
     *  180° -------   0°
     *          I
     *          I
     *         270°
     */
    public static boolean isInArc(float originX, float originY, float pointX, float pointY, float radius, float startAngle, float endAngle, float direction, float span) {
        if(!isInCircle(originX, originY, pointX, pointY, radius)) return false;
        double angle = Math.toDegrees(com.badlogic.gdx.math.MathUtils.atan2(pointY - originY, pointX - originX));

        if(angle < 0d) {
            angle += 360d;
        }

        if(direction == -1) {
            // Left punch direction.
            return angle >= startAngle && angle <= endAngle;
        } else {
            // Right punch direction.
            float diff = startAngle - endAngle;

            if(diff > 0) {
                return angle <= startAngle && angle >= endAngle;
            } else {
                return angle <= startAngle || angle >= endAngle;
            }
        }
    }

    public static boolean isInCircle(float originX, float originY, float checkX, float checkY, float radius) {
        float disSquared = (checkX - originX) * (checkX - originX) + (checkY - originY) * (checkY - originY);
        return disSquared <= radius * radius;
    }

    public static boolean rectIsInArc(float originX, float originY, float rectX, float rectY, float rectWidth, float rectHeight, float arcRadius, float startAngle, float endAngle, float direction, float span) {
        boolean bl = isInArc(originX, originY, rectX, rectY, arcRadius, startAngle, endAngle, direction, span);
        boolean br = isInArc(originX, originY, rectX + rectWidth, rectY, arcRadius, startAngle, endAngle, direction, span);
        boolean tl = isInArc(originX, originY, rectX, rectY + rectHeight, arcRadius, startAngle, endAngle, direction, span);
        boolean tr = isInArc(originX, originY, rectX + rectWidth, rectY + rectHeight, arcRadius, startAngle, endAngle, direction, span);
        return bl || br || tl || tr;
    }

    public static Location[] getNeighbourTiles(String dimension, float x, float y) {
        Location[] array = new Location[8];

        array[0] = new Location(dimension, x - 16, y - 16).toDetailedLocation();
        array[1] = new Location(dimension, x, y - 16).toDetailedLocation();
        array[2] = new Location(dimension, x + 16, y - 16).toDetailedLocation();
        array[3] = new Location(dimension, x + 16, y).toDetailedLocation();
        array[4] = new Location(dimension, x + 16, y + 16).toDetailedLocation();
        array[5] = new Location(dimension, x, y + 16).toDetailedLocation();
        array[6] = new Location(dimension, x - 16, y + 16).toDetailedLocation();
        array[7] = new Location(dimension, x - 16, y).toDetailedLocation();

        return array;
    }

    public static void dumpPerformanceMetrics(String prefix, String[] identifier, long[] timestamps) {
        StringBuilder builder = new StringBuilder();
        int useTickRate = ExpoServerBase.get().isLocalServer() ? ExpoShared.DEFAULT_LOCAL_TICK_RATE : ExpoServerDedicated.get().getTicksPerSecond();
        builder.append(prefix).append('\t').append("TPS=").append(useTickRate).append('\t').append('\t');

        dumpSingleMetric(builder, timestamps[timestamps.length - 1] - timestamps[0], "TOTAL");
        for(int i = 0; i < identifier.length; i++) {
            String n = identifier[i];
            long start = timestamps[i];
            long end = timestamps[i + 1];
            long diff = end - start;
            dumpSingleMetric(builder, diff, n);
        }

        ExpoLogger.log(builder.toString());
    }

    private static void dumpSingleMetric(StringBuilder builder, long time, String identifier) {
        double percentage = toPercentage(time);
        String colorCharacter = toColorCharacter(percentage);
        String formatted = String.format(Locale.US, "%.2f", percentage) + "%";

        builder.append(identifier).append("=").append(colorCharacter);
        builder.append(formatted).append(" ");
        builder.append('\t').append(ExpoShared.RESET);
    }

    private static double toPercentage(long time) {
        int useTickRate = ExpoServerBase.get().isLocalServer() ?
                ExpoShared.DEFAULT_LOCAL_TICK_RATE :
                ExpoServerDedicated.get().getTicksPerSecond();
        return (time / 1_000_000d * useTickRate);
    }

    private static String toColorCharacter(double percentage) {
        if(percentage < 20) return ExpoShared.GREEN_BRIGHT;
        if(percentage < 40) return ExpoShared.GREEN;
        if(percentage < 60) return ExpoShared.YELLOW_BRIGHT;
        if(percentage < 80) return ExpoShared.YELLOW;
        if(percentage < 100) return ExpoShared.RED_BRIGHT;
        return ExpoShared.RED;
    }

}
