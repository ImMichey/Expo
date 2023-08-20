package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import dev.michey.expo.log.ExpoLogger;

public class TileBatch extends SpriteBatch {

    private final Color COLOR = new Color();

    private static final float MIN_TEMP = 0.2847f;
    private static final float MAX_TEMP = 0.7058f;
    private static final float DIFF_TEMP = MAX_TEMP - MIN_TEMP;
    private static final float START_OFFSET = 0.55f;
    private static final float STRENGTH = 0.164f;

    private static final float USE_DEFAULT_RENDERER = 1.0f;
    private static final float USE_CUSTOM_RENDERER = 0.5f;

    private float getAsPacked(float grassColor, float ambientOcclusion) {
        if(grassColor == 0) {
            // No grass color.
            if(ambientOcclusion == 0) {
                // No ambient occlusion.
                COLOR.set(0.0f, 0.0f, USE_DEFAULT_RENDERER, 0.0f);
                return COLOR.toFloatBits();
            } else {
                // Ambient occlusion.
                COLOR.set(0.0f, ambientOcclusion, USE_CUSTOM_RENDERER, 1.0f);
                return COLOR.toFloatBits();
            }
        } else {
            // Grass color.
            float start = grassColor - MIN_TEMP - DIFF_TEMP * START_OFFSET;
            if(start < 0) start = 0;

            float adjustedAlpha = start * STRENGTH / DIFF_TEMP;

            COLOR.set(adjustedAlpha, ambientOcclusion, USE_CUSTOM_RENDERER, ambientOcclusion > 0 ? 1f : 0f);
            return COLOR.toFloatBits();
        }
    }

    public void drawTileColored(TextureRegion region, float x, float y, float width, float height, float color, float[] ambientOcclusion) {
        if (!drawing) throw new IllegalStateException("TileBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        Texture texture = region.texture;
        if (texture != lastTexture) {
            switchTexture(texture);
        } else if (idx == vertices.length) //
            flush();

        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = region.u;
        final float v = region.v2;
        final float u2 = region.u2;
        final float v2 = region.v;

        int idx = this.idx;
        float c1 = getAsPacked(color, ambientOcclusion[0]);
        float c2 = getAsPacked(color, ambientOcclusion[1]);
        float c3 = getAsPacked(color, ambientOcclusion[2]);
        float c4 = getAsPacked(color, ambientOcclusion[3]);
        vertices[idx    ] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = c1;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;

        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = c2;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;

        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = c3;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;

        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = c4;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

}