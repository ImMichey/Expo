package dev.michey.expo.render.ui.menu;

import dev.michey.expo.render.RenderContext;

public abstract class MenuComponent {

    public abstract void update(RenderContext r);
    public abstract void draw(RenderContext r);

    public abstract void onClick(boolean left, boolean middle, boolean right);

}
