package dev.michey.expo.render.ui.menu;

import dev.michey.expo.render.RenderContext;

import java.util.HashMap;

public abstract class MenuGroup {

    private final HashMap<String, Object> propertyMap;

    public MenuGroup() {
        propertyMap = new HashMap<>();
    }

    public void setProperty(String key, Object value) {
        propertyMap.put(key, value);
    }

    public float getPropertyAsFloat(String key) {
        return (float) propertyMap.get(key);
    }

    public int getPropertyAsInt(String key) {
        return (int) propertyMap.get(key);
    }

    public abstract void update(RenderContext r);
    public abstract void draw(RenderContext r);
    public abstract void handleInput(boolean left, boolean right, boolean middle);

}
