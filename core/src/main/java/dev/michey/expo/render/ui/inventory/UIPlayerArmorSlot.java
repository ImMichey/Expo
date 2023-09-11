package dev.michey.expo.render.ui.inventory;

import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.render.ui.InteractableItemSlot;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;

public class UIPlayerArmorSlot extends InteractableItemSlot {

    private final UIContainerInventory parentContainer;
    private final int armorId;
    private final String displayName;

    public UIPlayerArmorSlot(UIContainerInventory parentContainer, int armorId, String textureName, String displayName) {
        super(ExpoShared.CONTAINER_ID_PLAYER, armorId,
                ExpoAssets.get().textureRegion("ui_inventory_" + textureName + "slot_sel"), ExpoAssets.get().textureRegion("ui_inventory_" + textureName + "slot"));

        this.parentContainer = parentContainer;
        this.armorId = armorId;
        this.displayName = displayName;
    }

    @Override
    public void onTooltip() {
        if(ClientPlayer.getLocalPlayer().playerInventory.getSlotAt(armorId).item == null) {
            PlayerUI ui = PlayerUI.get();
            ui.drawTooltipColored(displayName + " Armor Slot", ClientStatic.COLOR_ARMOR_TEXT);
        } else {
            super.onTooltip();
        }
    }

}