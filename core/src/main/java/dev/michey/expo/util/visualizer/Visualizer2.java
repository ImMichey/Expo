package dev.michey.expo.util.visualizer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.render.RenderContext;
import org.json.JSONArray;
import org.json.JSONObject;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Locale;

import static dev.michey.expo.util.ClientUtils.screencap;

public class Visualizer2 {

    public Visualizer2() throws IOException {
        RenderContext r = RenderContext.get();
        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        fbo.begin();
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1.0f);

        // gen renderer
        ShapeDrawer render = new ShapeDrawer(r.hudBatch, ExpoAssets.get().textureRegion("square16x16"));

        // gen font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.shadowOffsetX = 1;
        parameter.shadowOffsetY = 1;
        parameter.size = 24;
        BitmapFont font = generator.generateFont(parameter);
        font.getData().markupEnabled = true;
        GlyphLayout layout = new GlyphLayout();
        parameter.size = 16;
        BitmapFont font16 = generator.generateFont(parameter);
        font16.getData().markupEnabled = true;
        parameter.size = 32;
        parameter.shadowColor = Color.BLACK;
        BitmapFont font32 = generator.generateFont(parameter);
        font32.getData().markupEnabled = true;

        // read data
        JSONObject obj = new JSONObject(Files.readString(new File("C:\\IDEAProjects\\ProjectV\\exportedData\\div1_ecos.json").toPath()));
        JSONArray data = obj.getJSONArray("data");

        render.getBatch().begin();
        // premultiplied alpha cause frame buffer (^_>^)b
        render.getBatch().setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        int bars = data.length();
        int startOffset = 32;
        int endOffset = 32;
        int spacingBetween = 20;
        int barWidth = 72;
        int maxHeight = 400;
        int totalWidth = startOffset + endOffset + barWidth * bars + (spacingBetween * (bars - 1));
        int totalHeight = 532;
        int startAt = (Gdx.graphics.getWidth() - totalWidth) / 2;
        int startAtY = (Gdx.graphics.getHeight() - totalHeight) / 2;
        int defaultPadding = 16;
        int rectangleBorder = 24;
        int borderRectangleThickness = 2;

        LinkedList<VisualizerData> list = new LinkedList<>();

        for(int i = 0; i < bars; i++) {
            JSONObject dataset = data.getJSONObject(i);
            list.add(new VisualizerData(dataset));
        }

        list.sort((o1, o2) -> Float.compare(o1.winrate, o2.winrate));

        //float bestPerRound = list.get(0).perc;
        //float worstPerRound = list.get(list.size() - 1).perc;
        float best = list.get(list.size() - 1).winrate;

        layout.setText(font16, "Small text");
        float h16 = layout.height;
        layout.setText(font, "Medium text");
        float h24 = layout.height;
        layout.setText(font32, "Large text");
        float h32 = layout.height;

        String header = "[#B6FF00]Project V[WHITE] - Div. 1 Stage 3 - Eco Winrate (<8500$)";
        layout.setText(font32, header);
        float headerW = layout.width;
        font32.draw(r.hudBatch, header, (Gdx.graphics.getWidth() - headerW) * 0.5f, startAtY + h16 + 8 + h24 + defaultPadding + maxHeight + h32 * 3);

        for(int i = 0; i < list.size(); i++) {
            VisualizerData v = list.get(list.size() - 1 - i);
            float height = maxHeight * v.winrate * 0.01f;
            float barX = startAt + startOffset + i * barWidth + i * spacingBetween;

            float roundsTextY = startAtY + h16;
            float aliasTextY = roundsTextY + 8 + h24;

            // 29/42
            String roundsPlayedText = v.roundsWon + "/" + v.roundsPlayed;
            layout.setText(font16, roundsPlayedText);
            float fontW = layout.width;
            font16.draw(r.hudBatch, roundsPlayedText, barX + (barWidth - fontW) * 0.5f, roundsTextY);

            // GHR
            layout.setText(font, v.alias);
            fontW = layout.width;
            font.draw(r.hudBatch, v.alias, barX + (barWidth - fontW) * 0.5f + 0.5f, aliasTextY);

            Color c = colorFrom(v.winrate / best, Color.RED, Color.YELLOW, Color.LIME);

            // BAR
            render.setColor(c);
            render.filledRectangle(barX, aliasTextY + defaultPadding, barWidth, height);

            c.a = 0.1f;
            render.setColor(c);
            render.filledRectangle(barX, aliasTextY + defaultPadding + height, barWidth, maxHeight - height);

            c.a = 0.75f;
            String percAsText = String.format(Locale.US, "%.1f", v.winrate) + "%";
            layout.setText(font, percAsText);
            fontW = layout.width;
            font.setColor(c);
            font.draw(r.hudBatch, percAsText, barX + (barWidth - fontW) * 0.5f, aliasTextY + defaultPadding + height + (maxHeight - height + h24) * 0.5f);

            font.setColor(Color.WHITE);
        }

        render.setColor(0.5f, 0.5f, 0.5f, 1.0f);
        render.rectangle(startAt - rectangleBorder, startAtY - rectangleBorder, totalWidth + rectangleBorder * 2, totalHeight + rectangleBorder * 2, borderRectangleThickness);

        //render.setColor(1.0f, 1.0f, 1.0f, 0.75f);
        //render.rectangle(startAt + defaultPadding, startAtY + h16 + 8 + h24 + defaultPadding + maxHeight * 0.5f, totalWidth - defaultPadding * 2, 0);

        render.getBatch().end();
        screencap("visualizer");
        fbo.end();

        fbo.dispose();
        font.dispose();
    }

    private Color colorFrom(float perc, Color... colors) {
        int c = colors.length;
        if(c == 1) return colors[0];

        float percPerColor = 1f / (float) c;

        int index = 0;
        float _perc = perc;

        while(true) {
            if(_perc > percPerColor) {
                _perc -= percPerColor;
                index++;
            } else {
                break;
            }
        }

        float colorPerc = _perc / percPerColor;

        int prevIndex = MathUtils.clamp(index - 1, 0, colors.length - 1);
        float rBase = colors[prevIndex].r;
        float gBase = colors[prevIndex].g;
        float bBase = colors[prevIndex].b;

        int nextIndex = index;
        float rDst = colors[nextIndex].r;
        float gDst = colors[nextIndex].g;
        float bDst = colors[nextIndex].b;

        float rDiff = (rDst - rBase) * colorPerc;
        float gDiff = (gDst - gBase) * colorPerc;
        float bDiff = (bDst - bBase) * colorPerc;

        return new Color(rBase + rDiff, gBase + gDiff, bBase + bDiff, 1.0f);
    }

}
