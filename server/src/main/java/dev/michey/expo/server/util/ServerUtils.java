package dev.michey.expo.server.util;

import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.util.Location;

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

}
