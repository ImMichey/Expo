package dev.michey.expo.server.main.logic.inventory;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryonet.Server;
import dev.michey.expo.server.main.logic.entity.ServerItem;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.inventory.item.ItemMetadata;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerPlayerInventory extends ServerInventory {

    /*
     *  Player Inventory structure
     *
     *  0-8 = Main Slots (hot-bar)
     *  9-35 = Row 2, 3, 4 (inventory)
     *  36 = Helmet
     *  37 = Chest
     *  38 = Gloves
     *  39 = Legs
     *  40 = Boots
     *
     */

    public ServerInventoryItem playerCursorItem; // can be null

    public ServerPlayerInventory(ServerPlayer player) {
        super(ExpoShared.PLAYER_INVENTORY_SLOTS);
        setOwner(player);
    }

    public void fillRandom() {
        ItemMapper mapper = ItemMapper.get();

        for(ServerInventorySlot slot : slots) {
            slot.item = new ServerInventoryItem();

            if(slot.slotIndex == ExpoShared.PLAYER_INVENTORY_SLOT_HEAD ||
                    slot.slotIndex == ExpoShared.PLAYER_INVENTORY_SLOT_CHEST ||
                    slot.slotIndex == ExpoShared.PLAYER_INVENTORY_SLOT_GLOVES ||
                    slot.slotIndex == ExpoShared.PLAYER_INVENTORY_SLOT_LEGS ||
                    slot.slotIndex == ExpoShared.PLAYER_INVENTORY_SLOT_FEET) {
                continue;
            }

            if(MathUtils.randomBoolean()) {
                ItemMapping mapping = mapper.randomMapping();

                slot.item.itemId = mapping.id;
                slot.item.itemAmount = MathUtils.random(1, mapping.logic.maxStackSize);

                if(mapping.logic.toolType != null) {
                    slot.item.itemMetadata = new ItemMetadata();
                    slot.item.itemMetadata.toolType = mapping.logic.toolType;
                    slot.item.itemMetadata.durability = MathUtils.random(1, mapping.logic.durability);
                }
            }
        }

        int[] updatedSlots = new int[slots.length];
        for(int i = 0; i < updatedSlots.length; i++) updatedSlots[i] = i;

        ServerInventoryItem[] updated = new ServerInventoryItem[slots.length];
        for(int i = 0; i < updated.length; i++) updated[i] = slots[i].item;

        ServerPackets.p19PlayerInventoryUpdate(updatedSlots, updated, PacketReceiver.player(getOwner()));
    }

    private boolean isArmorItem(ServerInventoryItem item) {
        return item.hasMetadata() && (
                item.itemMetadata.toolType == ToolType.HELMET
                        || item.itemMetadata.toolType == ToolType.CHESTPLATE
                        || item.itemMetadata.toolType == ToolType.GLOVES
                        || item.itemMetadata.toolType == ToolType.LEGS
                        || item.itemMetadata.toolType == ToolType.BOOTS
        );
    }

    public InventoryAddItemResult addItem(ServerInventoryItem item) {
        InventoryAddItemResult result = new InventoryAddItemResult();
        result.changeResult = new InventoryChangeResult();
        result.remainingAmount = item.itemAmount;

        ItemMapping mapping = ItemMapper.get().getMapping(item.itemId);
        boolean singleStack = mapping.logic.maxStackSize == 1;

        if(singleStack) {
            for(var slot : slots) {
                if(slot.slotIndex >= ExpoShared.PLAYER_INVENTORY_SLOT_HEAD) break;
                if(slot.item.isEmpty()) {
                    slot.item = item;
                    result.changeResult.addChange(slot.slotIndex, slot.item);
                    result.fullTransfer = true;
                    result.remainingAmount = 0;
                    break;
                }
            }
        } else {
            int remaining = item.itemAmount;

            // Find slots that are not filled with same id first.
            List<Integer> visitSlotsFirst = new LinkedList<>();
            int firstEmptySlot = -1;
            boolean canFillGaps = false;

            for(var slot : slots) {
                if(slot.slotIndex >= ExpoShared.PLAYER_INVENTORY_SLOT_HEAD) break;

                if(slot.item.itemId == item.itemId && slot.item.itemAmount < mapping.logic.maxStackSize) {
                    visitSlotsFirst.add(slot.slotIndex);

                    int existingInSlot = slot.item.itemAmount;
                    int transferable = mapping.logic.maxStackSize - existingInSlot;

                    if(transferable >= remaining) {
                        canFillGaps = true;
                        break;
                    }
                } else if(slot.item.isEmpty() && firstEmptySlot == -1) {
                    firstEmptySlot = slot.slotIndex;
                }
            }

            remaining = item.itemAmount;

            for(int slotsToVisit : visitSlotsFirst) {
                int existingInSlot = slots[slotsToVisit].item.itemAmount;
                int transferable = mapping.logic.maxStackSize - existingInSlot;

                if(transferable >= remaining) {
                    slots[slotsToVisit].item.itemAmount += remaining;
                    remaining = 0;
                    result.fullTransfer = true;
                    result.remainingAmount = 0;
                } else {
                    slots[slotsToVisit].item.itemAmount += transferable;
                    remaining -= transferable;
                    result.remainingAmount -= transferable;
                }

                result.changeResult.addChange(slotsToVisit, slots[slotsToVisit].item);
            }

            if(!canFillGaps && firstEmptySlot != -1) {
                // Fill in empty slot.
                slots[firstEmptySlot].item = item;
                slots[firstEmptySlot].item.itemAmount = remaining;
                result.remainingAmount = 0;
                result.fullTransfer = true;
                result.changeResult.addChange(firstEmptySlot, slots[firstEmptySlot].item);
            }
        }

        return result;
    }

    private ServerItem spawnServerItem(ServerInventoryItem container) {
        // Spawn item entity
        ServerItem item = new ServerItem();
        item.itemContainer = container;

        item.posX = getOwner().toFeetCenterX();
        item.posY = getOwner().toFeetCenterY();

        Vector2 dst = GenerationUtils.circular(getOwner().serverArmRotation + 270, 24.0f);
        item.dstX = dst.x;
        item.dstY = dst.y;

        ServerWorld.get().registerServerEntity(getOwner().entityDimension, item);
        return item;
    }

    public InventoryChangeResult performPlayerAction(int actionType, int slotId) {
        InventoryChangeResult result = new InventoryChangeResult();
        int[] oldIds = getOwner().getEquippedItemIds();

        if(slotId == ExpoShared.PLAYER_INVENTORY_SLOT_VOID) {
            // Clicked into nothing/void.
            if(playerCursorItem != null) {
                // Something on cursor, throw out.
                boolean full = actionType == ExpoShared.PLAYER_INVENTORY_ACTION_LEFT || (actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT && playerCursorItem.itemAmount == 1);

                if(full) {
                    result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null); // Set cursor as null on client
                    spawnServerItem(playerCursorItem);
                    playerCursorItem = null;
                } else {
                    playerCursorItem.itemAmount -= 1;
                    ServerInventoryItem cloned = new ServerInventoryItem();
                    cloned.clone(playerCursorItem, 1);
                    spawnServerItem(cloned);
                    result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                }
            } else {
                // Cursor is null and right-clicked, check if you can interact with item
                if(oldIds[0] != -1 && actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT) {
                    ItemMapping mapping = ItemMapper.get().getMapping(oldIds[0]);

                    if(mapping.logic.foodData != null) {
                        // Is food item
                        float hr = mapping.logic.foodData.hungerRestore;
                        float hcr = mapping.logic.foodData.hungerCooldownRestore;
                        ServerPlayer p = getOwner();

                        if(p.hunger < 100f) {
                            p.consumeFood(hr, hcr);
                            ServerPackets.p23PlayerLifeUpdate(p.health, p.hunger, PacketReceiver.player(p));
                            ServerPackets.p28PlayerFoodParticle(p.entityId, oldIds[0], PacketReceiver.whoCanSee(p));

                            int existingAmount = slots[p.selectedInventorySlot].item.itemAmount;

                            if(existingAmount > 1) {
                                slots[p.selectedInventorySlot].item.itemAmount -= 1;
                            } else {
                                slots[p.selectedInventorySlot].item = new ServerInventoryItem();
                            }

                            result.addChange(p.selectedInventorySlot, slots[p.selectedInventorySlot].item);
                        }
                    }
                }
            }
        } else if(slotId == ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR) {
            // Clicked cursor, no use yet.
        } else {
            // Clicked regular slot.
            if(playerCursorItem == null) {
                // Cursor is null, check if slot contains something to pick up.
                if(!slots[slotId].item.isEmpty()) {

                    if(actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT && slots[slotId].item.itemAmount > 1) {
                        // Only pick up half.
                        int pickup = slots[slotId].item.itemAmount / 2;

                        playerCursorItem = new ServerInventoryItem().clone(slots[slotId].item, pickup);
                        slots[slotId].item.itemAmount -= pickup;

                        result.addChange(slotId, slots[slotId].item);
                        result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                    } else {
                        playerCursorItem = slots[slotId].item;

                        ServerInventoryItem replaceWith = new ServerInventoryItem();
                        slots[slotId].item = replaceWith;

                        result.addChange(slotId, replaceWith);
                        result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                    }
                }
            } else {
                // Cursor is not null, swap or put into slot.
                if(slots[slotId].item.isEmpty()) {
                    if(armorCheck(slotId, playerCursorItem.itemId)) {
                        if(actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT && playerCursorItem.itemAmount > 1) {
                            slots[slotId].item = new ServerInventoryItem().clone(playerCursorItem, 1);
                            playerCursorItem.itemAmount -= 1;

                            result.addChange(slotId, slots[slotId].item);
                            result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                        } else {
                            slots[slotId].item = playerCursorItem;
                            playerCursorItem = null;

                            result.addChange(slotId, slots[slotId].item);
                            result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null);
                        }
                    }
                } else {
                    // Compare or swap.
                    boolean swap = slots[slotId].item.itemId != playerCursorItem.itemId;

                    if(swap) {
                        if(armorCheck(slotId, playerCursorItem.itemId)) {
                            ServerInventoryItem cachedCursor = playerCursorItem;

                            playerCursorItem = slots[slotId].item;
                            slots[slotId].item = cachedCursor;

                            result.addChange(slotId, slots[slotId].item);
                            result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                        }
                    } else {
                        // COMPARE.
                        ItemMapping mapping = ItemMapper.get().getMapping(playerCursorItem.itemId);
                        int maxStackSize = mapping.logic.maxStackSize;

                        int cursorAmount = playerCursorItem.itemAmount;
                        int slotAmount = slots[slotId].item.itemAmount;

                        int wantToTransfer = actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT ? 1 : playerCursorItem.itemAmount;
                        //boolean completeTransferPossible = (wantToTransfer + slotAmount) <= maxStackSize;
                        boolean hasSpaceToTransfer = maxStackSize > 1 && slots[slotId].item.itemAmount < maxStackSize;

                        if(hasSpaceToTransfer) {
                            int maxWantToTransfer = maxStackSize - slotAmount;

                            if(wantToTransfer > maxWantToTransfer) {
                                wantToTransfer = maxWantToTransfer;
                            }

                            int postTransfer = cursorAmount - wantToTransfer;

                            if(postTransfer == 0) {
                                playerCursorItem = null;
                                result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null);
                            } else {
                                playerCursorItem.itemAmount -= wantToTransfer;
                                result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                            }

                            slots[slotId].item.itemAmount += wantToTransfer;
                            result.addChange(slotId, slots[slotId].item);
                        } else {
                            // It's a full stack already, swap stack.
                            ServerInventoryItem cachedCursor = playerCursorItem;

                            playerCursorItem = slots[slotId].item;
                            slots[slotId].item = cachedCursor;

                            result.addChange(slotId, slots[slotId].item);
                            result.addChange(ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                        }
                    }
                }
            }
        }

        if(!result.changePresent) return null;
        int[] newIds = getOwner().getEquippedItemIds();
        boolean sameIds = Arrays.equals(oldIds, newIds);
        if(!sameIds) {
            getOwner().heldItemPacket(PacketReceiver.whoCanSee(getOwner()));
        }
        return result;
    }

    private boolean armorCheck(int slot, int itemId) {
        ItemMapping map = ItemMapper.get().getMapping(itemId);

        if(slot == ExpoShared.PLAYER_INVENTORY_SLOT_HEAD) return map.logic.toolType == ToolType.HELMET;
        if(slot == ExpoShared.PLAYER_INVENTORY_SLOT_CHEST) return map.logic.toolType == ToolType.CHESTPLATE;
        if(slot == ExpoShared.PLAYER_INVENTORY_SLOT_GLOVES) return map.logic.toolType == ToolType.GLOVES;
        if(slot == ExpoShared.PLAYER_INVENTORY_SLOT_LEGS) return map.logic.toolType == ToolType.LEGS;
        if(slot == ExpoShared.PLAYER_INVENTORY_SLOT_FEET) return map.logic.toolType == ToolType.BOOTS;

        return true;
    }

    @Override
    public ServerPlayer getOwner() {
        return (ServerPlayer) super.getOwner();
    }

}
