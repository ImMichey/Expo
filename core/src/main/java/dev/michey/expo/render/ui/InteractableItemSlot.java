package dev.michey.expo.render.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.client.ItemRender;
import dev.michey.expo.logic.inventory.ClientInventorySlot;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ExpoShared;

public class InteractableItemSlot extends InteractableUIElement {

    public InteractableItemSlot(int containerId, int inventorySlotId) {
        this(containerId, inventorySlotId, PlayerUI.get().invSlotS, PlayerUI.get().invSlot);
    }

    public InteractableItemSlot(int containerId, int inventorySlotId, TextureRegion drawSelected, TextureRegion drawNotSelected) {
        super(containerId, inventorySlotId, drawSelected, drawNotSelected);
    }

    @Override
    public void onLeftClick() {
        ClientPackets.p18PlayerInventoryInteraction(ExpoShared.PLAYER_INVENTORY_ACTION_LEFT, containerId, inventorySlotId);
    }

    @Override
    public void onRightClick() {
        ClientPackets.p18PlayerInventoryInteraction(ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT, containerId, inventorySlotId);
    }

    public void drawContents(ClientInventorySlot slot) {
        if(slot.item != null && !slot.item.isEmpty()) {
            PlayerUI parent = PlayerUI.get();
            ItemMapping mapping = ItemMapper.get().getMapping(slot.item.itemId);
            ItemRender render = mapping.uiRender;
            TextureRegion draw = render.textureRegion;

            RenderContext r = RenderContext.get();

            float dw = draw.getRegionWidth() * render.scaleX * parent.uiScale;
            float dh = draw.getRegionHeight() * render.scaleY * parent.uiScale;

            float _x = this.x + (parent.slotW - dw) * 0.5f;
            float _y = this.y + (parent.slotH - dh) * 0.5f;

            r.hudBatch.setColor(0f, 0f, 0f, 0.125f);
            r.hudBatch.draw(draw, _x + parent.uiScale, _y - parent.uiScale * 2, dw, dh);
            r.hudBatch.setColor(Color.WHITE);
            r.hudBatch.draw(draw, _x, _y, dw, dh);

            if(mapping.logic.maxStackSize > 1) {
                int amount = slot.item.itemAmount;
                String amountAsText = amount + "";

                parent.glyphLayout.setText(r.m5x7_shadow_use, amountAsText);
                float aw = parent.glyphLayout.width;
                float ah = parent.glyphLayout.height;

                r.m5x7_shadow_use.draw(r.hudBatch, amountAsText, this.ex - this.extX - aw - 1 * parent.uiScale, this.y + ah + 1 * parent.uiScale);
            }

            if(mapping.logic.durability != -1) {
                // Has durability.
                boolean drawDurability = mapping.logic.durability > slot.item.itemMetadata.durability;

                if(drawDurability) {
                    float percentage = (float) slot.item.itemMetadata.durability / mapping.logic.durability;
                    int space = 5;
                    int thickness = 1;
                    float yOffset = 5 * parent.uiScale;
                    float fullW = parent.slotW - space * parent.uiScale * 2;

                    r.hudBatch.setColor(26f / 255f, 16f / 255f, 16f / 255f, 1.0f);
                    r.hudBatch.draw(parent.whiteSquare, (int) (this.x + space * parent.uiScale), (int) (this.y + yOffset), fullW, thickness * parent.uiScale);

                    if(percentage > 0.66f) {
                        r.hudBatch.setColor(35f / 255f, 187f / 255f, 67f / 255f, 1f);
                    } else if(percentage > 0.33f) {
                        r.hudBatch.setColor(230f / 255f, 230f / 255f, 21f / 255f, 1f);
                    } else {
                        r.hudBatch.setColor(238f / 255f, 26f / 255f, 26f / 255f, 1f);
                    }

                    float percToPx = fullW * percentage;
                    r.hudBatch.draw(parent.whiteSquare, (int) (this.x + space * parent.uiScale), (int) (this.y + yOffset), percToPx, thickness * parent.uiScale);

                    r.hudBatch.setColor(Color.WHITE);
                }
            }
        }
    }

    public void drawSlotIndices() {
        PlayerUI parent = PlayerUI.get();
        RenderContext r = RenderContext.get();
        String text = String.valueOf(inventorySlotId);

        parent.glyphLayout.setText(r.m5x7_use, text);
        float w = parent.glyphLayout.width;
        float h = parent.glyphLayout.height;

        r.m5x7_use.draw(r.hudBatch, text, x + (parent.slotW - w) * 0.5f, y + h + (parent.slotH - h) * 0.5f);
    }

    @Override
    public void onTooltip() {
        ClientInventoryItem item = ClientPlayer.getLocalPlayer().playerInventory.getSlotAt(inventorySlotId).item;
        onTooltip(item);
    }

    public void onTooltip(ClientInventoryItem item) {
        PlayerUI parent = PlayerUI.get();

        if(item != null && !item.isEmpty()) {
            ItemMapping mapping = ItemMapper.get().getMapping(item.itemId);

            if(mapping.logic.isTool() || mapping.logic.isArmor() || mapping.logic.isTool(ToolType.NET)) {
                float percentage = item.itemMetadata.durability / (float) mapping.logic.durability * 100f;
                float[] rgb = parent.percentageToColor(percentage);
                String hex = new Color(rgb[0], rgb[1], rgb[2], 1.0f).toString();

                String[] lines = new String[] {
                        parent.COLOR_DESCRIPTOR_HEX + "Durability: [#" + hex + "]" + item.itemMetadata.durability + "/" + mapping.logic.durability
                };

                parent.drawTooltipColored(mapping.displayName, mapping.color, lines);
            } else if(mapping.logic.isFood()) {
                int seconds = (int) mapping.logic.foodData.hungerCooldownRestore;

                parent.drawTooltipColored(mapping.displayName, mapping.color,
                        parent.COLOR_DESCRIPTOR_HEX + "Regenerates [#" + parent.COLOR_GREEN_HEX + "]" + ((int) mapping.logic.foodData.hungerRestore) + "% hunger",
                        parent.COLOR_DESCRIPTOR_HEX + "Sates hunger for [#" + parent.COLOR_GREEN_HEX + "]" + seconds + " second" + (seconds == 1 ? "" : "s"));
            } else {
                parent.drawTooltipColored(mapping.displayName, mapping.color);
            }
        }
    }

}
