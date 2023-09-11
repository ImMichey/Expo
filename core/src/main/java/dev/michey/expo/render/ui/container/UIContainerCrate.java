package dev.michey.expo.render.ui.container;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

    public UIContainerCrate(int containerId, ServerInventorySlot[] slots) {
        super(InventoryViewType.CRATE, containerId);
        convertServerItemsToInventory(slots);

        crateBackground = tr("ui_crate_bg");
        TextureRegion crateTitle = tr("ui_crate_title");
        crateTitleLeft = new TextureRegion(crateTitle, 0, 0, 5, 16);
        crateTitleFiller = new TextureRegion(crateTitle, 7, 0, 1, 16);
        crateTitleRight = new TextureRegion(crateTitle, 9, 0, 5, 16);
        crateSlot = tr("ui_crate_slot");
        crateSlotS = tr("ui_crate_slotS");
    }

    @Override
    public void tick(RenderContext r, PlayerUI ui) {

    }

    @Override
    public void onReceiveItems() {

    }

    @Override
    public void onMouseMove() {

    }

    @Override
    public void draw(RenderContext r, PlayerUI ui) {
        String text = "Crate";
        ui.glyphLayout.setText(r.m5x7_use, text);
        float tw = ui.glyphLayout.width;
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

        r.m5x7_use.draw(r.hudBatch, text, _x + leftRightW, _y + (leftRightH - th) * 0.5f + th);

        drawSlots();
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
        dx = (Gdx.graphics.getWidth() - crateBackground.getRegionWidth() * ui.uiScale) * 0.5f;
        dy = Gdx.graphics.getHeight() - crateBackground.getRegionHeight() * ui.uiScale - 128 * ui.uiScale;

        float baseSlotX = dx + 4 * ui.uiScale;
        float baseSlotY = dy + 4 * ui.uiScale;
        float slotW = crateSlot.getRegionWidth() * ui.uiScale;
        float slotH = crateSlot.getRegionHeight() * ui.uiScale;

        for(int i = 0; i < interactableItemSlots.length; i++) {
            interactableItemSlots[i].update(baseSlotX + i * (slotW + 1 * ui.uiScale), baseSlotY, slotW, slotH);
        }
    }

    @Override
    public void onShow() {

    }

    @Override
    public void onHide() {

    }

}
