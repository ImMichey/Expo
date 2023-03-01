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

public class InteractableItemSlot {

    private final PlayerUI parent;

    public float x, y, w, h, ex, ey;
    public boolean selected, hovered, visible;
    public int inventorySlotId;

    private float fadeDelta;
    private float fadeGoal;

    private final TextureRegion drawSelected;
    private final TextureRegion drawNotSelected;

    public InteractableItemSlot(PlayerUI parent, int inventorySlotId) {
        this(parent, inventorySlotId, parent.invSlotS, parent.invSlot);
    }

    public InteractableItemSlot(PlayerUI parent, int inventorySlotId, TextureRegion drawSelected, TextureRegion drawNotSelected) {
        this.parent = parent;
        this.inventorySlotId = inventorySlotId;
        if(inventorySlotId == 0) selected = true;
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
        ClientPackets.p18PlayerInventoryInteraction(ExpoShared.PLAYER_INVENTORY_ACTION_LEFT, inventorySlotId);
    }

    public void onRightClick() {
        ClientPackets.p18PlayerInventoryInteraction(ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT, inventorySlotId);
    }

    public void drawBase() {
        RenderContext r = RenderContext.get();

        if(selected) {
            r.hudBatch.draw(parent.invSlotS, x, y, w, h);
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

    private ClientInventorySlot toInventorySlot() {
        PlayerInventory inventory = PlayerInventory.LOCAL_INVENTORY;
        return inventory.getSlotAt(inventorySlotId);
    }

    public void drawContents() {
        ClientInventorySlot inventorySlot = toInventorySlot();

        if(inventorySlot.item != null) {
            ItemMapping mapping = ItemMapper.get().getMapping(inventorySlot.item.itemId);
            ItemRender render = mapping.uiRender;
            TextureRegion draw = render.textureRegion;

            RenderContext r = RenderContext.get();

            float dw = draw.getRegionWidth() * render.scaleX * parent.uiScale;
            float dh = draw.getRegionHeight() * render.scaleY * parent.uiScale;

            float _x = this.x + (parent.slotW - dw) * 0.5f;
            float _y = this.y + (parent.slotH - dh) * 0.5f;

            r.hudBatch.draw(draw, _x, _y, dw, dh);

            if(mapping.logic.maxStackSize > 1) {
                int amount = inventorySlot.item.itemAmount;
                String amountAsText = amount + "";

                parent.glyphLayout.setText(parent.m5x7_shadow_use, amountAsText);
                float aw = parent.glyphLayout.width;
                float ah = parent.glyphLayout.height;

                parent.m5x7_shadow_use.draw(r.hudBatch, amountAsText, this.ex - aw - 1 * parent.uiScale, this.y + ah + 1 * parent.uiScale);
            }

            if(mapping.logic.durability != -1) {
                // Has durability.
                boolean drawDurability = mapping.logic.durability > inventorySlot.item.itemMetadata.durability;

                if(drawDurability) {
                    float percentage = (float) inventorySlot.item.itemMetadata.durability / mapping.logic.durability;
                    int space = 5;
                    int thickness = 1;
                    float yOffset = 5 * parent.uiScale;
                    float fullW = parent.slotW - space * parent.uiScale * 2;

                    r.hudBatch.setColor(26f / 255f, 16f / 255f, 16f / 255f, 1.0f);
                    r.hudBatch.draw(parent.whiteSquare, this.x + space * parent.uiScale, this.y + yOffset, fullW, thickness * parent.uiScale);

                    if(percentage > 0.66f) {
                        r.hudBatch.setColor(35f / 255f, 187f / 255f, 67f / 255f, 1f);
                    } else if(percentage > 0.33f) {
                        r.hudBatch.setColor(230f / 255f, 230f / 255f, 21f / 255f, 1f);
                    } else {
                        r.hudBatch.setColor(238f / 255f, 26f / 255f, 26f / 255f, 1f);
                    }

                    float percToPx = fullW * percentage;
                    r.hudBatch.draw(parent.whiteSquare, this.x + space * parent.uiScale, this.y + yOffset, percToPx, thickness * parent.uiScale);

                    r.hudBatch.setColor(Color.WHITE);
                }
            }
        }
    }

    public void drawSlotIndices() {
        RenderContext r = RenderContext.get();
        String text = inventorySlotId + "";

        parent.glyphLayout.setText(parent.m5x7_use, text);
        float w = parent.glyphLayout.width;
        float h = parent.glyphLayout.height;

        parent.m5x7_use.draw(r.hudBatch, text, x + (parent.slotW - w) * 0.5f, y + h + (parent.slotH - h) * 0.5f);
    }

    public void onTooltip() {
        if(toInventorySlot().item == null) {
            if(inventorySlotId == ExpoShared.PLAYER_INVENTORY_SLOT_HEAD) {
                parent.drawTooltipColored("Head Armor Slot", ClientStatic.COLOR_ARMOR_TEXT);
            } else if(inventorySlotId == ExpoShared.PLAYER_INVENTORY_SLOT_CHEST) {
                parent.drawTooltipColored("Chest Armor Slot", ClientStatic.COLOR_ARMOR_TEXT);
            } else if(inventorySlotId == ExpoShared.PLAYER_INVENTORY_SLOT_GLOVES) {
                parent.drawTooltipColored("Gloves Armor Slot", ClientStatic.COLOR_ARMOR_TEXT);
            } else if(inventorySlotId == ExpoShared.PLAYER_INVENTORY_SLOT_LEGS) {
                parent.drawTooltipColored("Legs Armor Slot", ClientStatic.COLOR_ARMOR_TEXT);
            } else if(inventorySlotId == ExpoShared.PLAYER_INVENTORY_SLOT_FEET) {
                parent.drawTooltipColored("Boots Armor Slot", ClientStatic.COLOR_ARMOR_TEXT);
            }
        } else {
            ItemMapping mapping = ItemMapper.get().getMapping(toInventorySlot().item.itemId);
            parent.drawTooltipColored(mapping.displayName, mapping.color);
        }
    }

}
