package dev.michey.expo.server.main.logic.world.gen;

import java.awt.geom.Point2D;

public class Point extends Point2D
{
    public float x, y;
    public float absoluteX, absoluteY;

    public Point()
    {
        x = 0;
        y = 0;
    }

    public Point(double X, double Y)
    {
        x = (float)X;
        y = (float)Y;
    }

    @Override
    public double getX()
    {
        return x;
    }

    @Override
    public double getY()
    {
        return y;
    }

    @Override
    public void setLocation(double arg0, double arg1)
    {
        x = (float)arg0;
        y = (float)arg1;
    }

    public double getLength()
    {
        return this.distance(0, 0);
    }

    public static double distance(Point p0, Point p1)
    {
        return new Point(p0.x, p0.y).distance(p1);
    }

    public static Point interpolate(Point p0, Point p1, double amt)
    {
        Point p = p0.plus((p1.minus(p0).cross(amt)));
        return p;
    }

    public Point minus(Point p0)
    {
        Point out = new Point();
        out.x = this.x - p0.x;
        out.y = this.y - p0.y;
        return out;
    }

    public Point plus(Point p0)
    {
        Point out = new Point();
        out.x = this.x + p0.x;
        out.y = this.y + p0.y;
        return out;
    }

    public Point cross(double d)
    {
        this.x *= d;
        this.y *= d;
        return this;
    }

}