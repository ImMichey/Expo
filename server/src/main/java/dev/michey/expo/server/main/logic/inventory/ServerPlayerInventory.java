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
        super(InventoryViewType.PLAYER_INVENTORY, ExpoShared.PLAYER_INVENTORY_SLOTS, ExpoShared.CONTAINER_ID_PLAYER);
        setOwner(player);
    }

    @Override
    public void dropAllItems(float offsetX, float offsetY, float radiusMin, float radiusMax) {
        int dropItems = 0;

        for(var slot : slots) {
            if(!slot.item.isEmpty()) {
                dropItems++;
            }
        }

        if(playerCursorItem != null && !playerCursorItem.isEmpty()) {
            dropItems++;
        }

        Vector2[] positions = GenerationUtils.positions(dropItems, radiusMin, radiusMax);
        int i = 0;

        for(var slot : slots) {
            if(!slot.item.isEmpty()) {
                createDrop(slot.item, offsetX, offsetY, positions[i]);
                i++;
            }
        }

        if(playerCursorItem != null && !playerCursorItem.isEmpty()) {
            createDrop(playerCursorItem, offsetX, offsetY, positions[positions.length - 1]);
        }
    }

    private void createDrop(ServerInventoryItem item, float offsetX, float offsetY, Vector2 pos) {
        ServerItem drop = new ServerItem();
        drop.itemContainer = new ServerInventoryItem().clone(item);
        item.setEmpty();
        drop.posX = getOwner().posX + offsetX;
        drop.posY = getOwner().posY + offsetY;
        drop.dstX = pos.x;
        drop.dstY = pos.y;
        ServerWorld.get().registerServerEntity(getOwner().entityDimension, drop);
    }

    public ServerItem spawnServerItem(ServerInventoryItem container) {
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

    @Override
    public void removeInventoryViewer(ServerPlayer player) {
        if(getOwner().entityId != player.entityId) {
            super.removeInventoryViewer(player);
        }
    }

    public InventoryChangeResult performPlayerAction(int actionType, int slotId, boolean shift) {
        InventoryChangeResult result = new InventoryChangeResult();
        int[] oldIds = getOwner().getEquippedItemIds();

        if(slotId == ExpoShared.PLAYER_INVENTORY_SLOT_VOID) {
            // Clicked into nothing/void.
            if(playerCursorItem != null) {
                // Something on cursor, throw out.
                boolean full = actionType == ExpoShared.PLAYER_INVENTORY_ACTION_LEFT || (actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT && playerCursorItem.itemAmount == 1);

                if(full) {
                    result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null); // Set cursor as null on client
                    spawnServerItem(playerCursorItem);
                    playerCursorItem = null;
                } else {
                    playerCursorItem.itemAmount -= 1;
                    ServerInventoryItem cloned = new ServerInventoryItem();
                    cloned.clone(playerCursorItem, 1);
                    spawnServerItem(cloned);
                    result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                }
            } else {
                // Cursor is null and right-clicked, check if you can interact with item
                if(oldIds[0] != -1 && actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT) {
                    ItemMapping mapping = ItemMapper.get().getMapping(oldIds[0]);

                    if(mapping.logic.foodData != null) {
                        // Is food item
                        float hr = mapping.logic.foodData.hungerRestore;
                        float hcr = mapping.logic.foodData.hungerCooldownRestore;
                        float hpr = mapping.logic.foodData.healthRestore;
                        ServerPlayer p = getOwner();
                        boolean use;

                        if(p.hunger < 100 && hr > 0) {
                            use = true;
                        } else use = p.health < 100 && hpr > 0;

                        if(use) {
                            p.consumeFood(hr, hcr, hpr);
                            ServerPackets.p23PlayerLifeUpdate(p.health, p.hunger, PacketReceiver.player(p));
                            ServerPackets.p28PlayerFoodParticle(p.entityId, oldIds[0], PacketReceiver.whoCanSee(p));
                            ServerPackets.p47ItemConsume(p.entityId, oldIds[0], -1, PacketReceiver.whoCanSee(p));

                            int existingAmount = slots[p.selectedInventorySlot].item.itemAmount;

                            if(existingAmount > 1) {
                                slots[p.selectedInventorySlot].item.itemAmount -= 1;
                            } else {
                                slots[p.selectedInventorySlot].item = new ServerInventoryItem();
                            }

                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, p.selectedInventorySlot, slots[p.selectedInventorySlot].item);
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

                        result.addChange(ExpoShared.CONTAINER_ID_PLAYER, slotId, slots[slotId].item);
                        result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                    } else {
                        if(shift && getOwner().viewingInventory != null && getOwner().viewingInventory != this && !slots[slotId].item.isEmpty()) {
                            ServerInventoryItem existing = slots[slotId].item;
                            InventoryAddItemResult transferResult = getOwner().viewingInventory.addItem(existing);

                            if(transferResult.changeResult.changePresent) {
                                if(transferResult.fullTransfer) {
                                    existing.setEmpty();
                                    transferResult.changeResult.addChange(getContainerId(), slotId, existing);
                                } else {
                                    existing.itemAmount = transferResult.remainingAmount;
                                    transferResult.changeResult.addChange(getContainerId(), slotId, existing);
                                }

                                ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(transferResult.changeResult, PacketReceiver.player(getOwner()));
                            }
                        } else {
                            playerCursorItem = slots[slotId].item;

                            ServerInventoryItem replaceWith = new ServerInventoryItem();
                            slots[slotId].item = replaceWith;

                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, slotId, replaceWith);
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                        }
                    }
                }
            } else {
                // Cursor is not null, swap or put into slot.
                if(slots[slotId].item.isEmpty()) {
                    if(armorCheck(slotId, playerCursorItem.itemId)) {
                        if(actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT && playerCursorItem.itemAmount > 1) {
                            slots[slotId].item = new ServerInventoryItem().clone(playerCursorItem, 1);
                            playerCursorItem.itemAmount -= 1;

                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, slotId, slots[slotId].item);
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                        } else {
                            slots[slotId].item = playerCursorItem;
                            playerCursorItem = null;

                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, slotId, slots[slotId].item);
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null);
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

                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, slotId, slots[slotId].item);
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
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
                                result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null);
                            } else {
                                playerCursorItem.itemAmount -= wantToTransfer;
                                result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
                            }

                            slots[slotId].item.itemAmount += wantToTransfer;
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, slotId, slots[slotId].item);
                        } else {
                            // It's a full stack already, swap stack.
                            ServerInventoryItem cachedCursor = playerCursorItem;

                            playerCursorItem = slots[slotId].item;
                            slots[slotId].item = cachedCursor;

                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, slotId, slots[slotId].item);
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, playerCursorItem);
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

        int maxStack = ItemMapper.get().getMapping(recipe.outputId).logic.maxStackSize;
        int loops = tryCraftAll ? maxStack : 1;
        int crafted = 0;

        InventoryChangeResult result = null;

        while(loops > 0) {
            loops--;

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

            if(!hasAllIngredients) break;
            slotsVisited.sort(Comparator.comparingInt(o -> o.value));

            // ================================================================================== Checking for output space
            boolean hasSpaceForOutput = false;

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

            if(!hasSpaceForOutput) {
                break;
            }

            // ================================================================================== Actual adding/removing + packets
            result = new InventoryChangeResult();
            crafted++;

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
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, index, slots[index].item);
                            amount -= slotAmount;
                        } else if(amount == slotAmount) {
                            slots[index].item.setEmpty();
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, index, slots[index].item);
                            continue nextInput;
                        } else {
                            slots[index].item.itemAmount -= amount;
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, index, slots[index].item);
                            continue nextInput;
                        }
                    }
                }
            }

            var addResult = addItem(new ServerInventoryItem(recipe.outputId, recipe.outputAmount));
            result.merge(addResult.changeResult);
        }

        if(result != null) {
            ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(result, PacketReceiver.player(getOwner()));
            ServerPackets.p36PlayerReceiveItem(new int[] {recipe.outputId}, new int[] {recipe.outputAmount * crafted}, PacketReceiver.player(getOwner()));

            int[] newIds = getOwner().getEquippedItemIds();
            boolean sameIds = Arrays.equals(oldIds, newIds);
            if(!sameIds) getOwner().heldItemPacket(PacketReceiver.whoCanSee(getOwner()));
        }
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
