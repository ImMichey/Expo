package dev.michey.expo.server.main.logic.inventory;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipe;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.util.ExpoShared.PLAYER_INVENTORY_NO_ARMOR_SLOT_AMOUNT;

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
        super(InventoryViewType.PLAYER_INVENTORY, ExpoShared.PLAYER_INVENTORY_SLOTS);
        setOwner(player);
        addInventoryViewer(player);
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

    public void craft(CraftingRecipe recipe, boolean tryCraftAll) {
        int[] oldIds = getOwner().getEquippedItemIds();

        // ================================================================================== Checking for existing input
        boolean hasAllIngredients = true;
        List<Pair<Integer, Integer>> slotsVisited = null;

        for(int i = 0; i < recipe.inputIds.length; i++) {
            int id = recipe.inputIds[i];
            int amount = recipe.inputAmounts[i];

            var pair = containsItem(id, amount);

            if(!pair.key) {
                hasAllIngredients = false;
                break;
            } else {
                if(slotsVisited == null) slotsVisited = new LinkedList<>();
                slotsVisited.addAll(pair.value);
            }
        }

        if(!hasAllIngredients) return;
        slotsVisited.sort(Comparator.comparingInt(o -> o.value));

        // ================================================================================== Checking for output space
        boolean hasSpaceForOutput = false;
        int maxStack = ItemMapper.get().getMapping(recipe.outputId).logic.maxStackSize;
        int needToFillRemaining = recipe.outputAmount;

        total: for(int i = 0; i < recipe.inputIds.length; i++) {
            int checkForId = recipe.inputIds[i];
            int checkForAmount = recipe.inputAmounts[i];

            for(var pair : slotsVisited) {
                int slotIndex = pair.key;
                int _id = slots[slotIndex].item.itemId;

                if(_id == checkForId) {
                    if(slots[slotIndex].item.itemAmount <= checkForAmount) {
                        hasSpaceForOutput = true;
                        break total;
                    }
                }
            }
        }

        if(!hasSpaceForOutput) {
            for(int i = 0; i < PLAYER_INVENTORY_NO_ARMOR_SLOT_AMOUNT; i++) {
                ServerInventorySlot slot = slots[i];

                if(slot.item.isEmpty()) {
                    hasSpaceForOutput = true;
                    break;
                }

                if(slot.item.itemId == recipe.outputId) {
                    int diff = maxStack - slot.item.itemAmount;
                    needToFillRemaining -= diff;

                    if(needToFillRemaining <= 0) {
                        hasSpaceForOutput = true;
                        break;
                    }
                }
            }
        }

        if(!hasSpaceForOutput) return;

        // ================================================================================== Actual adding/removing + packets
        InventoryChangeResult result = new InventoryChangeResult();

        // Do inventory stuff here
        nextInput: for(int i = 0; i < recipe.inputIds.length; i++) {
            int id = recipe.inputIds[i];
            int amount = recipe.inputAmounts[i];

            for(var entry : slotsVisited) {
                int index = entry.key;

                if(slots[index].item.itemId == id) {
                    int slotAmount = slots[index].item.itemAmount;

                    if(amount > slotAmount) {
                        slots[index].item.setEmpty();
                        result.addChange(index, slots[index].item);
                        amount -= slotAmount;
                    } else if(amount == slotAmount) {
                        slots[index].item.setEmpty();
                        result.addChange(index, slots[index].item);
                        continue nextInput;
                    } else {
                        slots[index].item.itemAmount -= amount;
                        result.addChange(index, slots[index].item);
                        continue nextInput;
                    }
                }
            }
        }

        ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(result, PacketReceiver.player(getOwner()));

        var addResult = addItem(new ServerInventoryItem(recipe.outputId, recipe.outputAmount));
        ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(addResult.changeResult, PacketReceiver.player(getOwner()));
        ServerPackets.p36PlayerReceiveItem(new int[] {recipe.outputId}, new int[] {recipe.outputAmount}, PacketReceiver.player(getOwner()));

        int[] newIds = getOwner().getEquippedItemIds();
        boolean sameIds = Arrays.equals(oldIds, newIds);
        if(!sameIds) getOwner().heldItemPacket(PacketReceiver.whoCanSee(getOwner()));
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
