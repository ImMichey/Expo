package dev.michey.expo.render.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.util.GameSettings;

public class MenuButton extends MenuComponent {

    private final MenuGroup group;
    private String text;
    public int x;
    public int y;

    public boolean hovered = false;
    private float hoverDelta = 0.0f;

    public MenuButton(MenuGroup group, String text) {
        this.group = group;
        this.text = text;
    }

    public void update(RenderContext r) {

    }

    @Override
    public void onClick(boolean left, boolean middle, boolean right) {

    }

    public void draw(RenderContext r) {
        hovered = r.mouseX >= x
                && r.mouseX <= (x + group.getPropertyAsInt("buttonFullWidth"))
                && r.mouseY >= y
                && r.mouseY <= (y + group.getPropertyAsInt("buttonFullHeight"));

        if(hovered && hoverDelta < 1.0f) {
            hoverDelta += r.delta * 5;
            if(hoverDelta > 1.0f) hoverDelta = 1.0f;
        }

        if(!hovered && hoverDelta > 0.0f) {
            hoverDelta -= r.delta * 5;
            if(hoverDelta < 0.0f) hoverDelta = 0.0f;
        }

        if(hoverDelta != 0) {
            drawButton(r.buttonPartsSelected, false);
        }

        if(hoverDelta != 0) {
            r.hudBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f - hoverDelta);
        }
        drawButton(r.buttonParts, true);
        if(hoverDelta != 0) {
            r.hudBatch.setColor(Color.WHITE);
        }
    }

    private void drawButton(TextureRegion[] baseButton, boolean drawText) {
        RenderContext r = RenderContext.get();
        int scale = GameSettings.get().uiScale;
        int padding = group.getPropertyAsInt("padding") * scale;
        float w = group.getPropertyAsInt("buttonWidth") + padding * 2;
        float h = group.getPropertyAsInt("buttonHeight") + padding * 2;

        float blw = baseButton[3].getRegionWidth() * scale;
        float blh = baseButton[3].getRegionHeight() * scale;

        r.hudBatch.draw(baseButton[3], x, y, blw, blh);
        r.hudBatch.draw(baseButton[4], x + blw, y, w, blh);
        r.hudBatch.draw(baseButton[5], x + blw + w, y, blw, blh);

        r.hudBatch.draw(baseButton[6], x, y + blh, w + blw * 2, h);

        float tlw = baseButton[0].getRegionWidth() * scale;
        float tlh = baseButton[0].getRegionHeight() * scale;

        r.hudBatch.draw(baseButton[0], x, y + blh + h, tlw, tlh);
        r.hudBatch.draw(baseButton[1], x + tlw, y + blh + h, w, tlh);
        r.hudBatch.draw(baseButton[2], x + tlw + w, y + blh + h, tlw, tlh);

        BitmapFont useFont = r.m5x7_border_all[1];
        if(drawText) {
            r.globalGlyph.setText(useFont, text);
            useFont.draw(r.hudBatch, text, x + blw + padding + (group.getPropertyAsInt("buttonWidth") - r.globalGlyph.width) * 0.5f,
                    y + padding + r.globalGlyph.height + blh + (group.getPropertyAsInt("buttonHeight") - r.globalGlyph.height) * 0.5f);
        }
    }

}
