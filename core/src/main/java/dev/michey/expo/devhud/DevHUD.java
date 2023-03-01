package dev.michey.expo.devhud;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.render.RenderContext;

import java.util.TreeMap;

public class DevHUD {

    private int offsetY;

    private TreeMap<String, Long> injectedLines;

    public DevHUD() {
        injectedLines = new TreeMap<>();
    }

    public void draw() {
        if(!RenderContext.get().drawDebugHUD) return;

        RenderContext.get().hudBatch.begin();

        line("[GRAY]FPS: [WHITE]" + Gdx.graphics.getFramesPerSecond());

        /*
        for(String s : injectedLines.keySet()) {
            if(injectedLines.get(s) == RenderContext.get().frameId) {
                line(s);
            }
        }
        injectedLines.clear();
        */

        RenderContext.get().hudBatch.end();
        offsetY = 0;
    }

    private void line(String s) {
        RenderContext.get().m5x7_base.draw(RenderContext.get().hudBatch, s, 16, Gdx.graphics.getHeight() - 16 - offsetY);
        offsetY += 16;
    }

    public void injectLine(String s) {
        injectedLines.put(s, RenderContext.get().frameId);
    }

    // global singleton
    private static DevHUD INSTANCE;

    public static DevHUD get() {
        if(INSTANCE == null) {
            INSTANCE = new DevHUD();
        }

        return INSTANCE;
    }

}

