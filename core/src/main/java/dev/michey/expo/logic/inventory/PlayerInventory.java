package dev.michey.expo.logic.inventory;

import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ExpoShared;

public class PlayerInventory extends ClientInventory {

    public static PlayerInventory LOCAL_INVENTORY;

    public int selectedSlot = 0;
    public ClientInventoryItem cursorItem; // can be null

    public PlayerInventory(ClientPlayer player) {
        super(ExpoShared.PLAYER_INVENTORY_SLOTS);
        setOwner(player);
        LOCAL_INVENTORY = this;
    }

    public void modifySelectedSlot(int slot) {
        int oldSelection = selectedSlot;
        if(oldSelection == slot) return;

        PlayerUI ui = getOwner().getUI();

        ui.hotbarSlots[selectedSlot].selected = false;
        selectedSlot = slot;
        ui.hotbarSlots[selectedSlot].selected = true;

        ClientPackets.p20PlayerInventorySwitch(selectedSlot);
        AudioEngine.get().playSoundGroup("switch");
    }

    public ClientInventoryItem currentItem() {
        return getSlotAt(selectedSlot).item;
    }

    public void scrollSelectedSlot(int amount) {
        int finalSlot = selectedSlot + amount;

        if(finalSlot < 0) {
            finalSlot = 8;
        } else if(finalSlot > 8) {
            finalSlot = 0;
        }

        modifySelectedSlot(finalSlot);
    }

    @Override
    public ClientPlayer getOwner() {
        return (ClientPlayer) super.getOwner();
    }

}
