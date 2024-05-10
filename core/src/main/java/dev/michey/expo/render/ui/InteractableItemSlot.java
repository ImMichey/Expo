package dev.michey.expo.render.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.ClientInventorySlot;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ExpoShared;

import java.util.LinkedList;
import java.util.List;

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
        if(selected) {
            RenderContext r = RenderContext.get();
            PlayerUI parent = PlayerUI.get();

            float mw = parent.slotSelectionMarker.getRegionWidth() * parent.uiScale;
            float mh = parent.slotSelectionMarker.getRegionHeight() * parent.uiScale;

            r.hudBatch.draw(parent.slotSelectionMarker, x + (w - mw) * 0.5f, y + h - mh + parent.uiScale, mw, mh);
        }

        if(slot.item != null && !slot.item.isEmpty()) {
            RenderContext r = RenderContext.get();
            PlayerUI parent = PlayerUI.get();
            ItemMapping mapping = ItemMapper.get().getMapping(slot.item.itemId);
            r.hudBatch.setColor(0f, 0f, 0f, 0.125f);

            for(ItemRender ir : mapping.uiRender) {
                // Draw background...
                float _x = this.x + (parent.slotW - mapping.uiRender[0].useWidth * parent.uiScale) * 0.5f;
                float _y = this.y + (parent.slotH - mapping.uiRender[0].useHeight * parent.uiScale) * 0.5f;

                r.hudBatch.draw(ir.useTextureRegion, (int) (_x + parent.uiScale + parent.uiScale * ir.offsetX), (int) (_y - parent.uiScale * 2 + parent.uiScale * ir.offsetY),
                        ir.useTextureRegion.getRegionWidth() * parent.uiScale, ir.useTextureRegion.getRegionHeight() * parent.uiScale);
            }

            r.hudBatch.setColor(Color.WHITE);
            for(ItemRender ir : mapping.uiRender) {
                float _x = this.x + (parent.slotW - mapping.uiRender[0].useWidth * parent.uiScale) * 0.5f;
                float _y = this.y + (parent.slotH - mapping.uiRender[0].useHeight * parent.uiScale) * 0.5f;

                r.hudBatch.draw(ir.useTextureRegion, (int) (_x + parent.uiScale * ir.offsetX), (int) (_y + parent.uiScale * ir.offsetY),
                        ir.useTextureRegion.getRegionWidth() * parent.uiScale, ir.useTextureRegion.getRegionHeight() * parent.uiScale);
            }

            if(mapping.logic.maxStackSize > 1) {
                int amount = slot.item.itemAmount;
                String amountAsText = amount + "";

                parent.glyphLayout.setText(r.m5x7_shadow_use, amountAsText);
                float aw = parent.glyphLayout.width;
                float ah = parent.glyphLayout.height;

                r.m5x7_shadow_use.draw(r.hudBatch, amountAsText, (int) (this.ex - this.extX - aw - 1 * parent.uiScale), (int) (this.y + ah + 1 * parent.uiScale));
            }

            if(mapping.logic.durability != -1) {
                // Has durability.
                boolean drawDurability = mapping.logic.durability > slot.item.itemMetadata.durability;

                if(drawDurability) {
                    float percentage = (float) slot.item.itemMetadata.durability / mapping.logic.durability;
                    int space = 5;
                    int thickness = 1;
                    float yOffset = 4 * parent.uiScale;
                    float fullW = parent.slotW - space * parent.uiScale * 2;

                    int dbx = (int) (this.x + space * parent.uiScale);
                    int dby = (int) (this.y + yOffset);

                    r.hudBatch.setColor(26f / 255f, 16f / 255f, 16f / 255f, 1.0f);
                    r.hudBatch.draw(parent.whiteSquare, dbx - 1, dby, fullW + 2, thickness * parent.uiScale);
                    r.hudBatch.draw(parent.whiteSquare, dbx, dby - 1, fullW, thickness * parent.uiScale + 2);
                    r.hudBatch.setColor(39f / 255f, 24f / 255f, 24f / 255f, 1.0f);
                    r.hudBatch.draw(parent.whiteSquare, dbx, dby, fullW, thickness * parent.uiScale);

                    if(percentage > 0.66f) {
                        r.hudBatch.setColor(35f / 255f, 187f / 255f, 67f / 255f, 1f);
                    } else if(percentage > 0.33f) {
                        r.hudBatch.setColor(230f / 255f, 230f / 255f, 21f / 255f, 1f);
                    } else {
                        r.hudBatch.setColor(238f / 255f, 26f / 255f, 26f / 255f, 1f);
                    }

                    float percToPx = fullW * percentage;
                    r.hudBatch.draw(parent.whiteSquare, dbx, dby, percToPx, thickness * parent.uiScale);

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

        r.m5x7_use.draw(r.hudBatch, text, (int) (x + (parent.slotW - w) * 0.5f), (int) (y + h + (parent.slotH - h) * 0.5f));
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

            if(mapping.logic.isTool() || mapping.logic.isArmor() || mapping.logic.isWeapon() || mapping.logic.isTool(ToolType.NET)) {
                float percentage = item.itemMetadata.durability / (float) mapping.logic.durability * 100f;
                float[] rgb = parent.percentageToColor(percentage);
                String hex = new Color(rgb[0], rgb[1], rgb[2], 1.0f).toString();

                String[] lines = new String[] {
                        PlayerUI.COLOR_DESCRIPTOR_HEX + "Durability: [#" + hex + "]" + item.itemMetadata.durability + "/" + mapping.logic.durability
                };

                parent.drawTooltipColored(mapping.displayName, mapping.color, lines);
            } else if(mapping.logic.isFood()) {
                int hungerCooldownRestore = (int) mapping.logic.foodData.hungerCooldownRestore;
                int hungerRestore = (int) mapping.logic.foodData.hungerRestore;
                int healthRestore = (int) mapping.logic.foodData.healthRestore;

                List<String> tooltip = new LinkedList<>();
                List<TextureRegion> icons = new LinkedList<>();
                ClientPlayer cp = ClientPlayer.getLocalPlayer();

                if(healthRestore > 0) {
                    String jump;

                    if(cp.playerHealth < 100) {
                        float startHp = cp.playerHealth;
                        float endHp = Math.min(startHp + healthRestore, 100);

                        jump = PlayerUI.COLOR_DESCRIPTOR2_HEX + " (" + ExpoShared.toDisplayNumber(startHp) + "% > " + ExpoShared.toDisplayNumber(endHp) + "%)";
                    } else {
                        jump = "";
                    }

                    tooltip.add(PlayerUI.COLOR_DESCRIPTOR_HEX + "Heals [#" + parent.COLOR_GREEN_HEX + "]" + healthRestore + "% health" + jump + PlayerUI.COLOR_DESCRIPTOR_HEX);
                    icons.add(parent.iconHealthRestore);
                }

                if(hungerRestore > 0) {
                    String jump;

                    if(cp.playerHunger < 100) {
                        float startHunger = cp.playerHunger;
                        float endHunger = Math.min(startHunger + hungerRestore, 100);

                        jump = PlayerUI.COLOR_DESCRIPTOR2_HEX + " (" + ExpoShared.toDisplayNumber(startHunger) + "% > " + ExpoShared.toDisplayNumber(endHunger) + "%)";
                    } else {
                        jump = "";
                    }

                    tooltip.add(PlayerUI.COLOR_DESCRIPTOR_HEX + "Fills [#" + parent.COLOR_GREEN_HEX + "]" + hungerRestore + "% hunger" + jump + PlayerUI.COLOR_DESCRIPTOR_HEX);
                    icons.add(parent.iconHungerRestore);
                }

                if(hungerCooldownRestore > 0) {
                    tooltip.add(PlayerUI.COLOR_DESCRIPTOR_HEX + "Sates [#" + parent.COLOR_GREEN_HEX + "]" + hungerCooldownRestore + " second" + (hungerCooldownRestore == 1 ? "" : "s"));
                    icons.add(parent.iconHungerCooldownRestore);
                }

                parent.drawTooltipColored(mapping.displayName, mapping.color,
                        icons.toArray(new TextureRegion[0]),
                        tooltip.toArray(new String[0]));
            } else {
                parent.drawTooltipColored(mapping.displayName, mapping.color);
            }
        }
    }

}
