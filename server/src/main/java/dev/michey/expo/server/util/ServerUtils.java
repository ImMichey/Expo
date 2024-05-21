package dev.michey.expo.server.util;

import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.arch.ExpoServerDedicated;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Location;

import java.util.LinkedList;
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

    record PerformanceMetric(String prefix, String[] identifier, long[] timestamps, int tickRate) {

    }

    private static final LinkedList<PerformanceMetric> metrics = new LinkedList<>();
    private static final Object lock = new Object();

    public static void recordPerformanceMetric(String prefix, String[] identifier, long[] timestamps, int tickRate) {
        synchronized (lock) {
            metrics.add(new PerformanceMetric(prefix, identifier, timestamps, tickRate));
        }
    }

    public static void recordPerformanceMetric(String prefix, String[] identifier, long[] timestamps) {
        synchronized (lock) {
            metrics.add(new PerformanceMetric(prefix, identifier, timestamps, ExpoServerBase.get().isLocalServer() ? ExpoShared.DEFAULT_LOCAL_TICK_RATE : ExpoServerDedicated.get().getTicksPerSecond()));
        }
    }

    public static void dumpPerformanceMetrics() {
        synchronized (lock) {
            for(PerformanceMetric metric : metrics) {
                StringBuilder builder = new StringBuilder();
                builder.append(metric.prefix).append('\t').append("TPS=").append(metric.tickRate).append('\t').append('\t');

                dumpSingleMetric(builder, metric.timestamps[metric.timestamps.length - 1] - metric.timestamps[0], "TOTAL", metric.tickRate);
                for(int i = 0; i < metric.identifier.length; i++) {
                    String n = metric.identifier[i];
                    long start = metric.timestamps[i];
                    long end = metric.timestamps[i + 1];
                    long diff = end - start;
                    dumpSingleMetric(builder, diff, n, metric.tickRate);
                }

                ExpoLogger.log(builder.toString());
            }

            metrics.clear();
        }
    }

    private static void dumpSingleMetric(StringBuilder builder, long time, String identifier, int tickRate) {
        double percentage = toPercentage(time, tickRate);
        String colorCharacter = toColorCharacter(percentage);
        String formatted = String.format(Locale.US, "%.2f", percentage) + "%";

        builder.append(identifier).append("=").append(colorCharacter);
        builder.append(formatted).append(" ");
        builder.append('\t').append(ExpoShared.RESET);
    }

    private static double toPercentage(long time, int tickRate) {
        return (time / 1_000_000d * tickRate);
    }

    private static String toColorCharacter(double percentage) {
        if(percentage < 20) return ExpoShared.GREEN_BRIGHT;
        if(percentage < 40) return ExpoShared.GREEN;
        if(percentage < 60) return ExpoShared.YELLOW_BRIGHT;
        if(percentage < 80) return ExpoShared.YELLOW;
        if(percentage < 100) return ExpoShared.RED_BRIGHT;
        return ExpoShared.RED;
    }

    private final static Vector2[] BASE_CURVE = new Vector2[] {
            new Vector2(0, 0),
            new Vector2(0, 1f),
            new Vector2(1f, 0)
    };

    public static Vector2[] toSmoothCurve(Vector2 start, Vector2 end, int precision, float curveHeight) {
        var bezier = new Bezier<>(BASE_CURVE);
        Vector2[] pathCache = new Vector2[precision];

        for(int i = 0; i < precision; i++) {
            pathCache[i] = new Vector2();
            float x = i / (float) precision;
            Vector2 out = bezier.valueAt(pathCache[i], x);

            float progressX = (end.x - start.x) * x;
            float progressY = (end.y - start.y) * x;
            pathCache[i].add(start.x + progressX, start.y + progressY + out.y * curveHeight);
        }

        return pathCache;
    }

}
