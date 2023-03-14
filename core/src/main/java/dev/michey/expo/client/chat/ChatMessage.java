package dev.michey.expo.client.chat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.logic.container.ExpoClientContainer;

import static dev.michey.expo.client.chat.ExpoClientChat.CHAT_LOCK;

public class ChatMessage {

    // Pure data holder
    public String message;
    public long timestamp;
    public String sender;
    public boolean byUser;

    // generated for easier rendering
    public int lines;
    public String[] strLines;

    public int startIndex; // externally set
    public int endIndex; // externally set

    public ChatMessage(String message, String sender, boolean byUser) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.sender = sender;
        this.byUser = byUser;

        generateRenderData();
    }

    public void generateRenderData() {
        synchronized(CHAT_LOCK) {
            GlyphLayout layout = new GlyphLayout();
            layout.setText(ExpoClientContainer.get().getPlayerUI().chat.chatUseFont, "[" + sender + "] ");
            float minusWidth = layout.width;

            layout.setText(ExpoClientContainer.get().getPlayerUI().chat.chatUseFont, message, Color.WHITE, ExpoClientChat.get().CHAT_HISTORY_TARGET_WIDTH - minusWidth, Align.left, true);
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