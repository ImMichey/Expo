package dev.michey.expo.render.ui.crafting;

import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.InteractableUIElement;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;

public class UICraftPreviousCategory extends InteractableUIElement {

    private final UIContainerInventory parentContainer;

    public UICraftPreviousCategory(UIContainerInventory parentContainer) {
        super(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_ARROW_LEFT,
                ExpoAssets.get().textureRegion("ui_crafting_arrow_left_sel"), ExpoAssets.get().textureRegion("ui_crafting_arrow_left"));

        this.parentContainer = parentContainer;
    }

    @Override
    public void onTooltip() {
        PlayerUI.get().drawTooltipColored("Previous categories", ClientStatic.COLOR_CRAFT_TEXT);
    }

}
