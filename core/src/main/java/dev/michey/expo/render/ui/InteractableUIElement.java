package dev.michey.expo.render.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.client.ItemRender;
import dev.michey.expo.logic.inventory.ClientInventorySlot;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.log.ExpoLogger.log;

public class InteractableUIElement {

    public final PlayerUI parent;

    public float x, y, w, h, ex, ey;
    public boolean selected, hovered, visible;
    public int inventorySlotId;

    private float fadeDelta;
    private float fadeGoal;

    private final TextureRegion drawSelected;
    private final TextureRegion drawNotSelected;

    public InteractableUIElement(PlayerUI parent, int inventorySlotId) {
        this(parent, inventorySlotId, parent.invSlotS, parent.invSlot);
    }

    public InteractableUIElement(PlayerUI parent, int inventorySlotId, TextureRegion drawSelected, TextureRegion drawNotSelected) {
        this.parent = parent;
        this.inventorySlotId = inventorySlotId;
        this.drawSelected = drawSelected;
        this.drawNotSelected = drawNotSelected;
        visible = true;
    }

    public void update(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        ex = x + w;
        ey = y + h;
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

}
