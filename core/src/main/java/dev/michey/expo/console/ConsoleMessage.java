package dev.michey.expo.console;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import java.text.SimpleDateFormat;
import java.util.Date;

import static dev.michey.expo.console.GameConsole.CONSOLE_LOCK;

public class ConsoleMessage {

    // Pure data holder
    public String message;
    public long timestamp;
    public boolean byUser;
    public String timeStr;

    // generated for easier rendering
    public int lines;
    public String[] strLines;

    public int startIndex; // externally set
    public int endIndex; // externally set

    public ConsoleMessage(String message, boolean byUser) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.byUser = byUser;
        this.timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));

        generateRenderData();
    }

    public void generateRenderData() {
        synchronized (CONSOLE_LOCK) {
            GlyphLayout layout = new GlyphLayout();
            layout.setText(GameConsole.get().consoleFont, message, Color.WHITE, GameConsole.get().CONSOLE_HISTORY_TARGET_WIDTH, Align.left, true);
            lines = layout.runs.size;
            strLines = new String[lines];

            StringBuilder builder = new StringBuilder();
            Array<GlyphLayout.GlyphRun> gra = layout.runs;

            for(int i = 0; i < strLines.length; i++) {
                Array<BitmapFont.Glyph> glyphs = gra.get(i).glyphs;

                for(int j = 0, n = glyphs.size; j < n; j++) {
                    BitmapFont.Glyph g = glyphs.get(j);
                    builder.append((char) g.id);
                }

                strLines[strLines.length - 1 - i] = builder.toString();
                builder.setLength(0);
            }
        }
    }

}