package dev.michey.expo.render.ui.crafting;

import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.lang.Lang;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.InteractableUIElement;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;

public class UICraftOpen extends InteractableUIElement {

    private final UIContainerInventory parentContainer;

    public UICraftOpen(UIContainerInventory parentContainer) {
        super(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_OPEN,
                ExpoAssets.get().textureRegion("ui_crafting_open_sel"), ExpoAssets.get().textureRegion("ui_crafting_open"));

        this.parentContainer = parentContainer;
    }

    @Override
    public void onLeftClick() {
        parentContainer.craftingOpen = !parentContainer.craftingOpen;
        parentContainer.updatePosition(RenderContext.get(), PlayerUI.get());
        parentContainer.adjustSlotVisibility();
        parentContainer.onMouseMove();
    }

    @Override
    public void onTooltip() {
        PlayerUI.get().drawTooltipColored(Lang.str(parentContainer.craftingOpen ? "ui.inventory.closecrafting" : "ui.inventory.opencrafting"), ClientStatic.COLOR_CRAFT_TEXT);
    }

}