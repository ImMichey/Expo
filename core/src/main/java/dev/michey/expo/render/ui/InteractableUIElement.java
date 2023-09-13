package dev.michey.expo.render.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.ClientInventorySlot;
import dev.michey.expo.render.RenderContext;

public class InteractableUIElement {

    public float x, y, w, h, ex, ey, extX, extY;
    public boolean selected, hovered, visible;
    public int inventorySlotId;
    public int containerId;

    private float fadeDelta;
    private float fadeGoal;

    private final TextureRegion drawSelected;
    private final TextureRegion drawNotSelected;

    public InteractableUIElement(int containerId, int inventorySlotId) {
        this(containerId, inventorySlotId, PlayerUI.get().invSlotS, PlayerUI.get().invSlot);
    }

    public InteractableUIElement(int containerId, int inventorySlotId, TextureRegion drawSelected, TextureRegion drawNotSelected) {
        this.containerId = containerId;
        this.inventorySlotId = inventorySlotId;
        this.drawSelected = drawSelected;
        this.drawNotSelected = drawNotSelected;
        visible = false;
    }

    public void update(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        ex = x + w;
        ey = y + h;
    }

    public void update(float x, float y, float w, float h, float extX, float extY) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.extX = extX;
        this.extY = extY;
        ex = x + w + extX;
        ey = y + h + extY;
    }

    public void onHoverBegin() {
        fadeGoal = 0f;
        fadeDelta = 1.0f;
    }

    public void onHoverEnd() {
        fadeGoal = 1.0f;
        fadeDelta = 0.0f;
    }

    public void onLeftClick() {

    }

    public void onRightClick() {

    }

    public void onTooltip() {

    }

    public void drawBase() {
        RenderContext r = RenderContext.get();

        if(selected) {
            r.hudBatch.draw(drawSelected, x, y, w, h);
        } else {
            if(fadeGoal > fadeDelta) {
                fadeDelta += r.delta * 5.0f;
                if(fadeDelta > fadeGoal) fadeDelta = fadeGoal;
            } else if(fadeGoal < fadeDelta) {
                fadeDelta -= r.delta * 5.0f;
                if(fadeDelta < fadeGoal) fadeDelta = fadeGoal;
            }

            if(fadeDelta == fadeGoal) {
                if(hovered) {
                    r.hudBatch.draw(drawSelected, x, y, w, h);
                } else {
                    r.hudBatch.draw(drawNotSelected, x, y, w, h);
                }
            } else {
                r.hudBatch.draw(drawNotSelected, x, y, w, h);
                r.hudBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f - fadeDelta);
                r.hudBatch.draw(drawSelected, x, y, w, h);
                r.hudBatch.setColor(Color.WHITE);
            }
        }
    }

    public TextureRegion getDrawSelected() {
        return drawSelected;
    }

    public TextureRegion getDrawNotSelected() {
        return drawNotSelected;
    }

}
