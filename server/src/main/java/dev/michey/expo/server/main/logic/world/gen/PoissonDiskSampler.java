package dev.michey.expo.server.main.logic.world.gen;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.util.ExpoShared;

import java.util.LinkedList;
import java.util.List;

// Modified version of
// https://github.com/Deadrik/jMapGen/blob/master/src/za/co/luma/math/sampling/PoissonDiskSampler.java
public class PoissonDiskSampler
{
    private final static int DEFAULT_POINTS_TO_GENERATE = 30;
    private final int pointsToGenerate; // k in literature
    private final Point p0, p1;
    private final Point dimensions;
    private final double cellSize; // r / sqrt(n), for 2D: r / sqrt(2)
    private final double minDist; // r
    private final int gridWidth, gridHeight;

    /**
     * A safety measure - no more than this number of points are produced by the algorithm.
     */
    public final static int MAX_POINTS = 100000;

    /**
     * Construct a new PoissonDisk object, with a given domain and minimum distance between points.
     *
     * @param x0
     *            x-coordinate of bottom left corner of domain.
     * @param y0
     *            x-coordinate of bottom left corner of domain.
     * @param x1
     *            x-coordinate of bottom left corner of domain.
     * @param y1
     *            x-coordinate of bottom left corner of domain.
     */
    public PoissonDiskSampler(double x0, double y0, double x1, double y1, double minDist, int pointsToGenerate)
    {
        p0 = new Point(x0, y0);
        p1 = new Point(x1, y1);
        dimensions = new Point(x1 - x0, y1 - y0);

        this.minDist = minDist;
        this.pointsToGenerate = pointsToGenerate;
        cellSize = minDist / Math.sqrt(2);
        gridWidth = (int) (dimensions.x / cellSize) + 1;
        gridHeight = (int) (dimensions.y / cellSize) + 1;
    }
    /**
     * Construct a new PoissonDisk object, with a given domain and minimum distance between points.
     *
     * @param x0
     *            x-coordinate of bottom left corner of domain.
     * @param y0
     *            x-coordinate of bottom left corner of domain.
     * @param x1
     *            x-coordinate of bottom left corner of domain.
     * @param y1
     *            x-coordinate of bottom left corner of domain.
     */
    public PoissonDiskSampler(double x0, double y0, double x1, double y1, double minDist)
    {
        this(x0, y0, x1, y1, minDist, DEFAULT_POINTS_TO_GENERATE);
    }

    public PoissonDiskSampler(float minDist) {
        this(0, 0, ExpoShared.CHUNK_SIZE, ExpoShared.CHUNK_SIZE, minDist, DEFAULT_POINTS_TO_GENERATE);
    }
    /**
     * Generates a list of points following the Poisson distribution. No more than MAX_POINTS are produced.
     *
     * @return The sample set.
     */
    @SuppressWarnings("unchecked")
    public List<Point> sample(float absoluteX, float absoluteY)
    {
        List<Point> activeList = new LinkedList<>();
        List<Point> pointList = new LinkedList<>();
        List<Point>[][] grid = new List[gridWidth][gridHeight];

        for (int i = 0; i < gridWidth; i++)
        {
            for (int j = 0; j < gridHeight; j++)
            {
                grid[i][j] = new LinkedList<>();
            }
        }

        addFirstPoint(grid, activeList, pointList);

        while (!activeList.isEmpty() && (pointList.size() < MAX_POINTS))
        {
            int listIndex = MathUtils.random(activeList.size() - 1);

            Point point = activeList.get(listIndex);
            boolean found = false;

            for (int k = 0; k < pointsToGenerate; k++)
            {
                found |= addNextPoint(grid, activeList, pointList, point);
            }

            if (!found)
            {
                activeList.remove(listIndex);
            }
        }

        for(Point p : pointList) {
            p.absoluteX = absoluteX + p.x;
            p.absoluteY = absoluteY + p.y;
        }

        return pointList;
    }

    private boolean addNextPoint(List<Point>[][] grid, List<Point> activeList,
                                 List<Point> pointList, Point point)
    {
        boolean found = false;
        //double fraction = distribution.getDouble((int) point.x, (int) point.y);
        Point q = generateRandomAround(point, 1 * minDist);

        if ((q.x >= p0.x) && (q.x < p1.x) && (q.y > p0.y) && (q.y < p1.y))
        {
            Vector2DInt qIndex = pointDoubleToInt(q, p0, cellSize);

            boolean tooClose = false;

            for (int i = Math.max(0, qIndex.x - 2); (i < Math.min(gridWidth, qIndex.x + 3)) && !tooClose; i++)
            {
                for (int j = Math.max(0, qIndex.y - 2); (j < Math.min(gridHeight, qIndex.y + 3)) && !tooClose; j++)
                {
                    for (Point gridPoint : grid[i][j])
                    {
                        if (Point.distance(gridPoint, q) < minDist * 1)
                        {
                            tooClose = true;
                        }
                    }
                }
            }

            if (!tooClose)
            {
                found = true;
                activeList.add(q);
                pointList.add(q);
                grid[qIndex.x][qIndex.y].add(q);
            }
        }

        return found;
    }

    private void addFirstPoint(List<Point>[][] grid, List<Point> activeList,
                               List<Point> pointList)
    {
        double d = MathUtils.random();
        double xr = p0.x + dimensions.x * (d);

        d = MathUtils.random();
        double yr = p0.y + dimensions.y * (d);

        Point p = new Point(xr, yr);
        Vector2DInt index = pointDoubleToInt(p, p0, cellSize);

        grid[index.x][index.y].add(p);
        activeList.add(p);
        pointList.add(p);
    }

    /**
     * Converts a PointDouble to a PointInt that represents the index coordinates of the point in the background grid.
     */
    static Vector2DInt pointDoubleToInt(Point pointDouble, Point origin, double cellSize)
    {
        return new Vector2DInt((int) ((pointDouble.x - origin.x) / cellSize),
                (int) ((pointDouble.y - origin.y) / cellSize));
    }

    /**
     * Generates a random point in the analus around the given point. The analus has inner radius minimum distance and
     * outer radius twice that.
     *
     * @param centre
     *            The point around which the random point should be.
     * @return A new point, randomly selected.
     */
    static Point generateRandomAround(Point centre, double minDist)
    {
        double d = MathUtils.random();
        double radius = (minDist + minDist * (d));

        d = MathUtils.random();
        double angle = 2 * Math.PI * (d);

        double newX = radius * Math.sin(angle);
        double newY = radius * Math.cos(angle);

        return new Point(centre.x + newX, centre.y + newY);
    }
}