package dev.michey.expo.render.font;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;

import static dev.michey.expo.util.ClientUtils.darker;

public class GradientFont {

    public static void applyGradient(float[] vertices, int vertexCount, float color1, float color2, float color3, float color4) {
        for(int index = 0; index < vertexCount; index += 20) {
            vertices[index + SpriteBatch.C1] = color1;
            vertices[index + SpriteBatch.C2] = color2;
            vertices[index + SpriteBatch.C3] = color3;
            vertices[index + SpriteBatch.C4] = color4;
        }
    }

    public static GlyphLayout drawGradient(BitmapFont font, Batch batch, CharSequence str, float x, float y, Color topColor, Color bottomColor) {
        BitmapFontCache cache = font.getCache();
        float tc = topColor.toFloatBits();
        float bc = bottomColor.toFloatBits();

        cache.clear();
        GlyphLayout layout = cache.addText(str, x, y);

        for(int page = 0; page < cache.getFont().getRegions().size; page++) {
            applyGradient(cache.getVertices(page), cache.getVertexCount(page), bc, tc, tc, bc);
        }

        cache.draw(batch);
        return layout;
    }

    public static GlyphLayout drawGradient(BitmapFont font, Batch batch, CharSequence str, float x, float y, Color use) {
        BitmapFontCache cache = font.getCache();
        float tc = use.toFloatBits();
        float bc = darker(use).toFloatBits();

        cache.clear();
        GlyphLayout layout = cache.addText(str, x, y);

        for(int page = 0; page < cache.getFont().getRegions().size; page++) {
            applyGradient(cache.getVertices(page), cache.getVertexCount(page), bc, tc, tc, bc);
        }

        cache.draw(batch);
        return layout;
    }

    public static GlyphLayout drawGradient(BitmapFont font, Batch batch, CharSequence str, float x, float y) {
        BitmapFontCache cache = font.getCache();
        float tc = font.getColor().toFloatBits();
        float bc = darker(font.getColor()).toFloatBits();

        cache.clear();
        GlyphLayout layout = cache.addText(str, x, y);

        for(int page = 0; page < cache.getFont().getRegions().size; page++) {
            applyGradient(cache.getVertices(page), cache.getVertexCount(page), bc, tc, tc, bc);
        }

        cache.draw(batch);
        return layout;
    }

}