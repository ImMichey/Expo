package dev.michey.expo.render.ui;

public class InteractableRecipeSlot extends InteractableUIElement {

    public InteractableRecipeSlot(PlayerUI parent, int inventorySlotId) {
        super(parent, inventorySlotId, parent.craftSlotS, parent.craftSlot);
    }

    @Override
    public void onTooltip() {

    }

}
