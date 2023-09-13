package dev.michey.expo.render.ui.container;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.inventory.ClientInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.InteractableItemSlot;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.server.main.logic.inventory.InventoryViewType;
import dev.michey.expo.server.main.logic.inventory.ServerInventorySlot;

public class UIContainerCrate extends UIContainer {

    /** Cached texture references */
    private final TextureRegion crateBackground;
    private final TextureRegion crateTitleLeft;
    private final TextureRegion crateTitleFiller;
    private final TextureRegion crateTitleRight;
    private final TextureRegion crateSlot;
    private final TextureRegion crateSlotS;

    private float dx, dy;
    private float invX, invY, invW, invH;

    public UIContainerCrate(int containerId, ServerInventorySlot[] slots) {
        super(InventoryViewType.CRATE, containerId);
        convertServerItemsToInventory(slots);

        crateBackground = tr("ui_crate_bg");
        TextureRegion crateTitle = tr("ui_crate_title");
        crateTitleLeft = new TextureRegion(crateTitle, 0, 0, 5, 16);
        crateTitleFiller = new TextureRegion(crateTitle, 6, 0, 1, 16);
        crateTitleRight = new TextureRegion(crateTitle, 8, 0, 5, 16);
        crateSlot = tr("ui_crate_slot");
        crateSlotS = tr("ui_crate_slotS");

        interactableItemSlots = new InteractableItemSlot[9];
        for(int i = 0; i < interactableItemSlots.length; i++) {
            interactableItemSlots[i] = new InteractableItemSlot(containerId, i, crateSlotS, crateSlot) {

                @Override
                public void onTooltip() {
                    super.onTooltip(clientInventory.getSlotAt(inventorySlotId).item);
                }

            };
        }
    }

    @Override
    public void tick(RenderContext r, PlayerUI ui) {

    }

    @Override
    public void onReceiveItems() {

    }

    @Override
    public void onMouseMove() {
        PlayerUI ui = PlayerUI.get();

        for(InteractableItemSlot slot : interactableItemSlots) ui.hoverCheck(slot);
        UIContainerInventory inv = UIContainerInventory.PLAYER_INVENTORY_CONTAINER;
        for(InteractableItemSlot slot : inv.inventorySlots) ui.hoverCheck(slot);
        for(InteractableItemSlot slot : inv.inventoryArmorSlots) ui.hoverCheck(slot);
    }

    @Override
    public void draw(RenderContext r, PlayerUI ui) {
        String text = "Crate";
        ui.glyphLayout.setText(r.m5x7_border_use, text);
        float tw = ui.glyphLayout.width + 4 * ui.uiScale;
        float th = ui.glyphLayout.height;

        float crateW = crateBackground.getRegionWidth() * ui.uiScale;
        float crateH = crateBackground.getRegionHeight() * ui.uiScale;

        r.hudBatch.draw(crateBackground, dx, dy, crateW, crateH);

        float _x = dx + 10 * ui.uiScale;
        float _y = dy + crateH - 1 * ui.uiScale;

        float leftRightW = crateTitleLeft.getRegionWidth() * ui.uiScale;
        float leftRightH = crateTitleLeft.getRegionHeight() * ui.uiScale;

        r.hudBatch.draw(crateTitleLeft, _x, _y, leftRightW, leftRightH);
        r.hudBatch.draw(crateTitleFiller, _x + leftRightW, _y, tw, leftRightH);
        r.hudBatch.draw(crateTitleRight, _x + leftRightW + tw, _y, leftRightW, leftRightH);

        r.m5x7_border_use.draw(r.hudBatch, text, _x + leftRightW + 2 * ui.uiScale, _y + (leftRightH - th) * 0.5f + th - ui.uiScale);

        drawSlots();

        // Draw inventory background
        UIContainerInventory inv = UIContainerInventory.PLAYER_INVENTORY_CONTAINER;
        r.hudBatch.draw(inv.invBackgroundNoCrafting, invX, invY, invW, invH);

        // Draw inventory slots
        ui.drawHotbarSlots();
        inv.drawSlots(inv.inventoryArmorSlots);
        inv.drawSlots(inv.inventorySlots);
    }

    @Override
    public void convertServerItemsToInventory(ServerInventorySlot[] slots) {
        if(interactableItemSlots == null) {
            interactableItemSlots = new InteractableItemSlot[slots.length];

            for(int i = 0; i < interactableItemSlots.length; i++) {
                interactableItemSlots[i] = new InteractableItemSlot(containerId, i, crateSlotS, crateSlot);
            }

            clientInventory = new ClientInventory(slots);
        } else {
            clientInventory.updateFrom(slots);
        }

        onReceiveItems();
    }

    @Override
    public void updatePosition(RenderContext r, PlayerUI ui) {
        float crateW = crateBackground.getRegionWidth() * ui.uiScale;
        float crateH = crateBackground.getRegionHeight() * ui.uiScale;

        {
            // Player inventory
            UIContainerInventory inv = UIContainerInventory.PLAYER_INVENTORY_CONTAINER;
            invW = inv.invBackgroundNoCrafting.getRegionWidth() * ui.uiScale;
            invH = inv.invBackgroundNoCrafting.getRegionHeight() * ui.uiScale;
            invX = (Gdx.graphics.getWidth() - invW) * 0.5f;
            invY = (Gdx.graphics.getHeight() - invH - crateH - 16 * ui.uiScale) * 0.5f;

            float startX = invX + 35 * ui.uiScale;
            float startY = invY + 17 * ui.uiScale;

            for(int i = 0; i < ui.hotbarSlots.length; i++) {
                ui.hotbarSlots[i].update(startX + (i * ui.slotW + i * ui.uiScale), startY, ui.slotW, ui.slotH, ui.uiScale, 0);
            }

            for(int i = 0; i < inv.inventorySlots.length; i++) {
                int x = i % 9;
                int y = i / 9;
                inv.inventorySlots[i].update(startX + (x * ui.slotW + x * ui.uiScale), startY + 33 * ui.uiScale + y * 30 * ui.uiScale, ui.slotW, ui.slotH, ui.uiScale, 2 * ui.uiScale * (y == 2 ? 0 : 1));
            }

            for(int i = 0; i < inv.inventoryArmorSlots.length; i++) {
                inv.inventoryArmorSlots[inv.inventoryArmorSlots.length - 1 - i].update(invX + 4 * ui.uiScale, invY + 4 * ui.uiScale + i * 30 * ui.uiScale, ui.slotW, ui.slotH, 0, 2 * ui.uiScale * (i == 4 ? 0 : 1));
            }
        }

        dx = (Gdx.graphics.getWidth() - crateW) * 0.5f;
        dy = invY + invH + 16 * ui.uiScale;

        float baseSlotX = dx + 4 * ui.uiScale;
        float baseSlotY = dy + 4 * ui.uiScale;
        float slotW = crateSlot.getRegionWidth() * ui.uiScale;
        float slotH = crateSlot.getRegionHeight() * ui.uiScale;

        for(int i = 0; i < interactableItemSlots.length; i++) {
            interactableItemSlots[i].update(baseSlotX + i * (slotW + 1 * ui.uiScale), baseSlotY, slotW, slotH, ui.uiScale, 0);
        }
    }

    @Override
    public void onShow() {
        adjustSlotVisibility();
        updatePosition(RenderContext.get(), PlayerUI.get());
    }

    @Override
    public void onHide() {
        adjustSlotVisibility();
        PlayerUI.get().updateHotbarPosition();
        onMouseMove();
    }

    public void adjustSlotVisibility() {
        for(InteractableItemSlot slot : interactableItemSlots) slot.visible = visible;

        UIContainerInventory inv = UIContainerInventory.PLAYER_INVENTORY_CONTAINER;
        for(InteractableItemSlot slot : inv.inventorySlots) slot.visible = visible;
        for(InteractableItemSlot slot : inv.inventoryArmorSlots) slot.visible = visible;
    }

}
