package dev.michey.expo.render.ui.container;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.inventory.ClientInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.InteractableItemSlot;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.server.main.logic.inventory.InventoryViewType;
import dev.michey.expo.server.main.logic.inventory.ServerInventorySlot;

import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public abstract class UIContainer {

    public InventoryViewType containerType;
    public int containerId;
    public boolean visible;

    public ClientInventory clientInventory = null;
    public InteractableItemSlot[] interactableItemSlots = null;

    public UIContainer(InventoryViewType containerType, int containerId) {
        this.containerType = containerType;
        this.containerId = containerId;
    }

    public static UIContainer fromType(InventoryViewType type, int containerId, ServerInventorySlot[] slots) {
        if(type == InventoryViewType.CRATE) {
            return new UIContainerCrate(containerId, slots);
        } else if(type == InventoryViewType.CHEST) {
            return new UIContainerChest(containerId, slots);
        }

        return null;
    }

    public abstract void tick(RenderContext r, PlayerUI ui);
    public abstract void draw(RenderContext r, PlayerUI ui);
    public abstract void onReceiveItems();
    public abstract void updatePosition(RenderContext r, PlayerUI ui);
    public abstract void onShow();
    public abstract void onHide();
    public abstract void onMouseMove();

    public TextureRegion tr(String name) {
        return ExpoAssets.get().textureRegion(name);
    }

    public void convertServerItemsToInventory(ServerInventorySlot[] slots) {
        ExpoLogger.log("Convert -> " + slots.length);

        if(interactableItemSlots == null) {
            interactableItemSlots = new InteractableItemSlot[slots.length];

            for(int i = 0; i < interactableItemSlots.length; i++) {
                interactableItemSlots[i] = new InteractableItemSlot(containerId, i);
            }

            clientInventory = new ClientInventory(slots);
        } else {
            clientInventory.updateFrom(slots);
        }

        onReceiveItems();
    }

    public void drawSlots(InteractableItemSlot[] interactableItemSlots) {
        if(interactableItemSlots != null) {
            for(InteractableItemSlot slot : interactableItemSlots) {
                slot.drawBase();
            }

            for(int i = 0; i < interactableItemSlots.length; i++) {
                interactableItemSlots[i].drawContents(clientInventory.getSlotAt(i));
            }

            if(DEV_MODE && Expo.get().getImGuiExpo().shouldDrawSlotIndices()) {
                for(InteractableItemSlot slot : interactableItemSlots) {
                    slot.drawSlotIndices();
                }
            }
        }
    }

    public void drawSlots() {
        if(interactableItemSlots != null) {
            for(InteractableItemSlot slot : interactableItemSlots) {
                slot.drawBase();
            }

            for(int i = 0; i < interactableItemSlots.length; i++) {
                interactableItemSlots[i].drawContents(clientInventory.getSlotAt(i));
            }

            if(DEV_MODE && Expo.get().getImGuiExpo().shouldDrawSlotIndices()) {
                for(InteractableItemSlot slot : interactableItemSlots) {
                    slot.drawSlotIndices();
                }
            }
        }
    }

}