package dev.michey.expo.render.ui.crafting;

import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.lang.Lang;
import dev.michey.expo.render.ui.InteractableUIElement;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;

public class UICraftNextCategory extends InteractableUIElement {

    private final UIContainerInventory parentContainer;

    public UICraftNextCategory(UIContainerInventory parentContainer) {
        super(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_ARROW_LEFT,
                ExpoAssets.get().textureRegion("ui_crafting_arrow_right_sel"), ExpoAssets.get().textureRegion("ui_crafting_arrow_right"));

        this.parentContainer = parentContainer;
    }

    @Override
    public void onTooltip() {
        PlayerUI.get().drawTooltipColored(Lang.str("ui.crafting.next"), ClientStatic.COLOR_CRAFT_TEXT);
    }

}
