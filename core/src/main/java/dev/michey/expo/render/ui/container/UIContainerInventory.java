package dev.michey.expo.render.ui.container;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.Expo;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.InteractableItemSlot;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.crafting.*;
import dev.michey.expo.render.ui.inventory.UIPlayerArmorSlot;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipe;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipeMapping;
import dev.michey.expo.server.main.logic.inventory.InventoryViewType;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public class UIContainerInventory extends UIContainer {

    public static UIContainerInventory PLAYER_INVENTORY_CONTAINER;

    public boolean craftingOpen;

    public InteractableItemSlot[] inventorySlots;
    public UIPlayerArmorSlot[] inventoryArmorSlots;

    public UICraftOpen craftOpenButton;
    public UICraftPreviousCategory craftPreviousCategoryButton;
    public UICraftNextCategory craftNextCategoryButton;
    public UICraftGroupCategory[] craftGroupCategoryButtons;
    private UICraftGroupCategory selectedCraftGroupCategory;
    public UICraftRecipeSlot[] craftRecipeSlots;

    /** Textures */
    private final TextureRegion invBackgroundCrafting;
    private final TextureRegion invBackground;

    /** Positions */
    private float invX, invY;
    private float invW, invH;

    public UIContainerInventory() {
        super(InventoryViewType.PLAYER_INVENTORY, ExpoShared.CONTAINER_ID_PLAYER);

        invBackground = tr("inv_bgc_");
        invBackgroundCrafting = tr("inv_bgco");

        craftOpenButton = new UICraftOpen(this);
        craftPreviousCategoryButton = new UICraftPreviousCategory(this);
        craftNextCategoryButton = new UICraftNextCategory(this);

        craftGroupCategoryButtons = new UICraftGroupCategory[4];
        craftGroupCategoryButtons[0] = new UICraftGroupCategory(this, ExpoShared.CRAFTING_CATEGORY_MISC, "misc", "Misc");
        craftGroupCategoryButtons[1] = new UICraftGroupCategory(this, ExpoShared.CRAFTING_CATEGORY_TOOLS, "tools", "Tools");
        craftGroupCategoryButtons[2] = new UICraftGroupCategory(this, ExpoShared.CRAFTING_CATEGORY_FOOD, "food", "Food");
        craftGroupCategoryButtons[3] = new UICraftGroupCategory(this, ExpoShared.CRAFTING_CATEGORY_3, null, "Unknown");

        craftRecipeSlots = new UICraftRecipeSlot[25];
        for(int i = 0; i < craftRecipeSlots.length; i++) craftRecipeSlots[i] = new UICraftRecipeSlot(this);
        setCraftGroupCategory(craftGroupCategoryButtons[0]);

        inventorySlots = new InteractableItemSlot[27];
        for(int i = 0; i < inventorySlots.length; i++) {
            inventorySlots[i] = new InteractableItemSlot(ExpoShared.CONTAINER_ID_PLAYER, i + 9);
        }

        inventoryArmorSlots = new UIPlayerArmorSlot[5];
        inventoryArmorSlots[0] = new UIPlayerArmorSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_HEAD, "head", "Head");
        inventoryArmorSlots[1] = new UIPlayerArmorSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_CHEST, "chest", "Chest");
        inventoryArmorSlots[2] = new UIPlayerArmorSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_GLOVES, "glove", "Gloves");
        inventoryArmorSlots[3] = new UIPlayerArmorSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_LEGS, "leg", "Legs");
        inventoryArmorSlots[4] = new UIPlayerArmorSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_FEET, "boot", "Boots");

        PLAYER_INVENTORY_CONTAINER = this;
    }

    public void setCraftGroupCategory(UICraftGroupCategory craftGroupCategory) {
        if(selectedCraftGroupCategory != null) selectedCraftGroupCategory.selected = false;
        selectedCraftGroupCategory = craftGroupCategory;
        selectedCraftGroupCategory.selected = true;

        for(var slot : craftRecipeSlots) {
            slot.setHoldingRecipe(null);
        }

        CraftingRecipeMapping mapping = CraftingRecipeMapping.get();
        var recipes = mapping.getCategoryMap().get(craftGroupCategory.getCategoryId());
        if(recipes == null) return;

        int slotId = 24;

        for(String s : recipes) {
            CraftingRecipe recipe = mapping.getRecipeMap().get(s);
            craftRecipeSlots[slotId].setHoldingRecipe(recipe);
            slotId--;
            if(slotId == -1) break; // Implement in future when > 25 recipes per category
        }
    }

    @Override
    public void tick(RenderContext r, PlayerUI ui) {

    }

    @Override
    public void drawSlots(InteractableItemSlot[] interactableItemSlots) {
        if(interactableItemSlots != null) {
            for(InteractableItemSlot slot : interactableItemSlots) {
                slot.drawBase();
            }

            for(int i = 0; i < interactableItemSlots.length; i++) {
                interactableItemSlots[i].drawContents(ClientPlayer.getLocalPlayer().playerInventory.getSlotAt(interactableItemSlots[i].inventorySlotId));
            }

            if(DEV_MODE && Expo.get().getImGuiExpo().shouldDrawSlotIndices()) {
                for(InteractableItemSlot slot : interactableItemSlots) {
                    slot.drawSlotIndices();
                }
            }
        }
    }

    @Override
    public void draw(RenderContext r, PlayerUI ui) {
        // Draw inventory background
        r.hudBatch.draw(craftingOpen ? invBackgroundCrafting : invBackground, invX, invY, invW, invH);

        // Draw inventory slots
        ui.drawHotbarSlots();
        drawSlots(inventoryArmorSlots);
        drawSlots(inventorySlots);

        craftOpenButton.drawBase();

        if(craftingOpen) {
            craftNextCategoryButton.drawBase();
            craftPreviousCategoryButton.drawBase();

            for(var el : craftGroupCategoryButtons) {
                el.drawBase();
            }

            for(var el : craftRecipeSlots) {
                el.drawBase();
            }
        }

        // Draw inventory text [244]
        ui.glyphLayout.setText(r.m5x7_shadow_use, "Inventory");
        float invTextOffsetX = ((244 * ui.uiScale) - ui.glyphLayout.width) * 0.5f;
        float invTextOffsetY = ui.glyphLayout.height + (craftingOpen ? 15 * ui.uiScale : 0) + 156 * ui.uiScale;
        r.m5x7_shadow_use.draw(r.hudBatch, "Inventory", invX + 35 * ui.uiScale + invTextOffsetX, invY + invTextOffsetY);

        // Draw Crafting text
        if(craftingOpen) {
            ui.glyphLayout.setText(r.m5x7_shadow_use, "Crafting");
            float cTextOffsetX = (150 * ui.uiScale - ui.glyphLayout.width) * 0.5f;
            r.m5x7_shadow_use.draw(r.hudBatch, "Crafting", invX + 278 * ui.uiScale + cTextOffsetX, invY + 199 * ui.uiScale + ui.glyphLayout.height);
        }
    }

    @Override
    public void updatePosition(RenderContext r, PlayerUI ui) {
        invW = craftingOpen ? invBackgroundCrafting.getRegionWidth() * ui.uiScale : invBackground.getRegionWidth() * ui.uiScale;
        invH = craftingOpen ? invBackgroundCrafting.getRegionHeight() * ui.uiScale : invBackground.getRegionHeight() * ui.uiScale;
        invX = (Gdx.graphics.getWidth() - invW) * 0.5f;
        invY = (Gdx.graphics.getHeight() - invH) * 0.5f;

        // Inventory is now open.
        float startX = invX + 35 * ui.uiScale;
        float startY = invY + 17 * ui.uiScale + (craftingOpen ? 15 * ui.uiScale : 0);

        for(int i = 0; i < ui.hotbarSlots.length; i++) {
            ui.hotbarSlots[i].update(startX + (i * ui.slotW + i * ui.uiScale), startY, ui.slotW, ui.slotH, ui.uiScale, 0);
        }

        for(int i = 0; i < inventorySlots.length; i++) {
            int x = i % 9;
            int y = i / 9;
            inventorySlots[i].update(startX + (x * ui.slotW + x * ui.uiScale), startY + 33 * ui.uiScale + y * 30 * ui.uiScale, ui.slotW, ui.slotH, ui.uiScale, 2 * ui.uiScale * (y == 2 ? 0 : 1));
        }

        for(int i = 0; i < inventoryArmorSlots.length; i++) {
            inventoryArmorSlots[inventoryArmorSlots.length - 1 - i].update(invX + 4 * ui.uiScale, invY + 4 * ui.uiScale + i * 30 * ui.uiScale + (craftingOpen ? 15 * ui.uiScale : 0), ui.slotW, ui.slotH, 0, 2 * ui.uiScale * (i == 4 ? 0 : 1));
        }

        if(craftingOpen) {
            craftOpenButton.update(invX + 429 * ui.uiScale, invY + 79 * ui.uiScale, ui.slotW, ui.slotH);
            craftPreviousCategoryButton.update(invX + 285 * ui.uiScale, invY + 163 * ui.uiScale, craftPreviousCategoryButton.getDrawNotSelected().getRegionWidth() * ui.uiScale, craftPreviousCategoryButton.getDrawNotSelected().getRegionHeight() * ui.uiScale);
            craftNextCategoryButton.update(invX + 409 * ui.uiScale, invY + 163 * ui.uiScale, craftNextCategoryButton.getDrawNotSelected().getRegionWidth() * ui.uiScale, craftNextCategoryButton.getDrawNotSelected().getRegionHeight() * ui.uiScale);
            for(int i = 0; i < craftGroupCategoryButtons.length; i++) {
                var el = craftGroupCategoryButtons[i];
                el.update(invX + 299 * ui.uiScale + i * ui.slotW + ui.uiScale * i, invY + 153 * ui.uiScale, ui.slotW, ui.slotH, ui.uiScale, 0);
            }
            for(int i = 0; i < craftRecipeSlots.length; i++) {
                var el = craftRecipeSlots[i];
                int x = i % 5;
                int y = i / 5;
                el.update(invX + 390 * ui.uiScale - x * ui.slotW - ui.uiScale * x, invY + 4 * ui.uiScale + y * ui.slotH + ui.uiScale * y, ui.slotW, ui.slotH, ui.uiScale, 2 * ui.uiScale * (y == 4 ? 0 : 1));
            }
        } else {
            craftOpenButton.update(invX + 282 * ui.uiScale, invY + 63 * ui.uiScale, ui.slotW, ui.slotH);
        }
    }

    @Override
    public void onMouseMove() {
        PlayerUI ui = PlayerUI.get();

        for(InteractableItemSlot slot : inventorySlots) ui.hoverCheck(slot);
        for(InteractableItemSlot slot : inventoryArmorSlots) ui.hoverCheck(slot);

        ui.hoverCheck(craftOpenButton);
        ui.hoverCheck(craftPreviousCategoryButton);
        ui.hoverCheck(craftNextCategoryButton);

        for(var button : craftGroupCategoryButtons) ui.hoverCheck(button);
        for(var button : craftRecipeSlots) ui.hoverCheck(button);
    }

    @Override
    public void onReceiveItems() {

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
        for(InteractableItemSlot slot : inventorySlots) slot.visible = visible;
        for(InteractableItemSlot slot : inventoryArmorSlots) slot.visible = visible;
        craftOpenButton.visible = visible;

        craftPreviousCategoryButton.visible = visible && craftingOpen;
        craftNextCategoryButton.visible = visible && craftingOpen;

        for(var button : craftGroupCategoryButtons) button.visible = visible && craftingOpen;
        for(var button : craftRecipeSlots) button.visible = visible && craftingOpen;
    }

}
