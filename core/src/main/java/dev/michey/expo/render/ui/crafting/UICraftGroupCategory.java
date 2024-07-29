package dev.michey.expo.render.ui.crafting;

import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.lang.Lang;
import dev.michey.expo.render.ui.InteractableUIElement;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;

public class UICraftGroupCategory extends InteractableUIElement {

    private final UIContainerInventory parentContainer;
    private final int categoryId;
    private final String displayName;

    public UICraftGroupCategory(UIContainerInventory parentContainer, int categoryId, String textureName, String displayName) {
        super(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_MISC + categoryId,
                ExpoAssets.get().textureRegion(textureName == null ? "ui_crafting_emptyslot_sel" : "ui_crafting_category_" + textureName + "_sel"),
                ExpoAssets.get().textureRegion(textureName == null ? "ui_crafting_emptyslot" : "ui_crafting_category_" + textureName));

        this.parentContainer = parentContainer;
        this.categoryId = categoryId;
        this.displayName = displayName;
    }

    @Override
    public void onLeftClick() {
        parentContainer.setCraftGroupCategory(this);
    }

    @Override
    public void onTooltip() {
        PlayerUI.get().drawTooltipColored(Lang.str(displayName), ClientStatic.COLOR_CRAFT_TEXT);
    }

    public int getCategoryId() {
        return categoryId;
    }

}
