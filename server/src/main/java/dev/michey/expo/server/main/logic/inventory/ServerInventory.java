package dev.michey.expo.server.main.logic.inventory;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.ItemMetadata;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class ServerInventory {

    /** What entity does the inventory belong to (player, mob, chest, etc.)? */
    private ServerEntity inventoryOwner = null;
    private final HashSet<Integer> viewerList;
    private final InventoryViewType type;
    private int containerId;

    /** Data structure */
    public final ServerInventorySlot[] slots;

    public ServerInventory(InventoryViewType type, int size) {
        this(type, size, 0);
    }

    public ServerInventory(InventoryViewType type, int size, int containerId) {
        this.type = type;
        this.containerId = containerId;
        slots = new ServerInventorySlot[size];
        viewerList = new HashSet<>();

        for(int i = 0; i < slots.length; i++) {
            slots[i] = new ServerInventorySlot(i);
        }
    }

    public boolean isEmpty() {
        for(ServerInventorySlot slot : slots) {
            if(!slot.item.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public void addInventoryViewer(ServerPlayer player) {
        viewerList.add(player.entityId);
        player.viewingInventory = this;
    }

    public void removeInventoryViewer(ServerPlayer player) {
        viewerList.remove(player.entityId);
    }

    public InventoryChangeResult performPlayerAction(ServerPlayer player, int actionType, int slotId, boolean shift) {
        InventoryChangeResult result = new InventoryChangeResult();
        int[] oldIds = player.getEquippedItemIds();

        if(slotId == ExpoShared.PLAYER_INVENTORY_SLOT_VOID) {
            // Clicked into nothing/void.
            if(player.playerInventory.playerCursorItem != null) {
                // Something on cursor, throw out.
                boolean full = actionType == ExpoShared.PLAYER_INVENTORY_ACTION_LEFT || (actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT && player.playerInventory.playerCursorItem.itemAmount == 1);

                if(full) {
                    result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null); // Set cursor as null on client
                    player.playerInventory.spawnServerItem(player.playerInventory.playerCursorItem);
                    player.playerInventory.playerCursorItem = null;
                } else {
                    player.playerInventory.playerCursorItem.itemAmount -= 1;
                    ServerInventoryItem cloned = new ServerInventoryItem();
                    cloned.clone(player.playerInventory.playerCursorItem, 1);
                    player.playerInventory.spawnServerItem(cloned);
                    result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, player.playerInventory.playerCursorItem);
                }
            }
        } else if(slotId == ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR) {
            // Clicked cursor, no use yet.
        } else {
            // Clicked regular slot.
            if(player.playerInventory.playerCursorItem == null) {
                // Cursor is null, check if slot contains something to pick up.
                if(!slots[slotId].item.isEmpty()) {
                    if(actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT && slots[slotId].item.itemAmount > 1) {
                        // Only pick up half.
                        int pickup = slots[slotId].item.itemAmount / 2;

                        player.playerInventory.playerCursorItem = new ServerInventoryItem().clone(slots[slotId].item, pickup);
                        slots[slotId].item.itemAmount -= pickup;

                        result.addChange(getContainerId(), slotId, slots[slotId].item);
                        result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, player.playerInventory.playerCursorItem);
                    } else {
                        if(shift && !slots[slotId].item.isEmpty()) {
                            ServerInventoryItem existing = slots[slotId].item;
                            InventoryAddItemResult transferResult = player.playerInventory.addItem(existing);

                            if(transferResult.changeResult.changePresent) {
                                if(transferResult.fullTransfer) {
                                    existing.setEmpty();
                                    transferResult.changeResult.addChange(getContainerId(), slotId, existing);
                                } else {
                                    existing.itemAmount = transferResult.remainingAmount;
                                    transferResult.changeResult.addChange(getContainerId(), slotId, existing);
                                }

                                ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(transferResult.changeResult, PacketReceiver.player(player));
                            }
                        } else {
                            player.playerInventory.playerCursorItem = slots[slotId].item;

                            ServerInventoryItem replaceWith = new ServerInventoryItem();
                            slots[slotId].item = replaceWith;

                            result.addChange(getContainerId(), slotId, replaceWith);
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, player.playerInventory.playerCursorItem);
                        }
                    }
                }
            } else {
                // Cursor is not null, swap or put into slot.
                if(slots[slotId].item.isEmpty()) {
                    if(actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT && player.playerInventory.playerCursorItem.itemAmount > 1) {
                        slots[slotId].item = new ServerInventoryItem().clone(player.playerInventory.playerCursorItem, 1);
                        player.playerInventory.playerCursorItem.itemAmount -= 1;

                        result.addChange(getContainerId(), slotId, slots[slotId].item);
                        result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, player.playerInventory.playerCursorItem);
                    } else {
                        slots[slotId].item = player.playerInventory.playerCursorItem;
                        player.playerInventory.playerCursorItem = null;

                        result.addChange(getContainerId(), slotId, slots[slotId].item);
                        result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null);
                    }
                } else {
                    // Compare or swap.
                    boolean swap = slots[slotId].item.itemId != player.playerInventory.playerCursorItem.itemId;

                    if(swap) {
                        ServerInventoryItem cachedCursor = player.playerInventory.playerCursorItem;

                        player.playerInventory.playerCursorItem = slots[slotId].item;
                        slots[slotId].item = cachedCursor;

                        result.addChange(getContainerId(), slotId, slots[slotId].item);
                        result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, player.playerInventory.playerCursorItem);
                    } else {
                        // COMPARE.
                        ItemMapping mapping = ItemMapper.get().getMapping(player.playerInventory.playerCursorItem.itemId);
                        int maxStackSize = mapping.logic.maxStackSize;

                        int cursorAmount = player.playerInventory.playerCursorItem.itemAmount;
                        int slotAmount = slots[slotId].item.itemAmount;

                        int wantToTransfer = actionType == ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT ? 1 : player.playerInventory.playerCursorItem.itemAmount;
                        //boolean completeTransferPossible = (wantToTransfer + slotAmount) <= maxStackSize;
                        boolean hasSpaceToTransfer = maxStackSize > 1 && slots[slotId].item.itemAmount < maxStackSize;

                        if(hasSpaceToTransfer) {
                            int maxWantToTransfer = maxStackSize - slotAmount;

                            if(wantToTransfer > maxWantToTransfer) {
                                wantToTransfer = maxWantToTransfer;
                            }

                            int postTransfer = cursorAmount - wantToTransfer;

                            if(postTransfer == 0) {
                                player.playerInventory.playerCursorItem = null;
                                result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, null);
                            } else {
                                player.playerInventory.playerCursorItem.itemAmount -= wantToTransfer;
                                result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, player.playerInventory.playerCursorItem);
                            }

                            slots[slotId].item.itemAmount += wantToTransfer;
                            result.addChange(getContainerId(), slotId, slots[slotId].item);
                        } else {
                            // It's a full stack already, swap stack.
                            ServerInventoryItem cachedCursor = player.playerInventory.playerCursorItem;

                            player.playerInventory.playerCursorItem = slots[slotId].item;
                            slots[slotId].item = cachedCursor;

                            result.addChange(getContainerId(), slotId, slots[slotId].item);
                            result.addChange(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR, player.playerInventory.playerCursorItem);
                        }
                    }
                }
            }
        }

        if(!result.changePresent) return null;
        int[] newIds = player.getEquippedItemIds();
        boolean sameIds = Arrays.equals(oldIds, newIds);
        if(!sameIds) {
            player.heldItemPacket(PacketReceiver.whoCanSee(getOwner()));
        }

        if(result.changePresent) {
            for(int viewer : viewerList) {
                if(viewer == player.entityId) continue;

                var changedSlotsList = result.changedSlots.get(containerId);
                int[] changedSlots = changedSlotsList.stream().mapToInt(Integer::intValue).toArray();

                var changedItemsList = result.changedItems.get(containerId);
                ServerInventoryItem[] arr = new ServerInventoryItem[changedItemsList.size()];
                for(int i = 0; i < arr.length; i++) arr[i] = changedItemsList.get(i);

                notifyViewer(viewer, changedSlots, arr);
            }
        }

        return result;
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
                    slot.item = new ServerInventoryItem().clone(item);
                    result.changeResult.addChange(getContainerId(), slot.slotIndex, slot.item);
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

                result.changeResult.addChange(getContainerId(), slotsToVisit, slots[slotsToVisit].item);
            }

            if(!canFillGaps && firstEmptySlot != -1) {
                // Fill in empty slot.
                slots[firstEmptySlot].item = new ServerInventoryItem().clone(item, remaining);
                result.remainingAmount = 0;
                result.fullTransfer = true;
                result.changeResult.addChange(getContainerId(), firstEmptySlot, slots[firstEmptySlot].item);
            }
        }

        return result;
    }

    public void kickViewers() {
        for(int viewer : viewerList) {
            kickViewer(viewer);
        }
    }

    public void kickViewer(int viewer) {
        ServerEntity viewerEntity = inventoryOwner.getDimension().getEntityManager().getEntityById(viewer);
        if(viewerEntity == null) return;
        ServerPlayer player = (ServerPlayer) viewerEntity;
        ServerPackets.p41InventoryViewQuit(PacketReceiver.player(player));
    }

    public Pair<Boolean, List<Pair<Integer, Integer>>> containsItem(int id, int amount) {
        Pair<Boolean, List<Pair<Integer, Integer>>> pair = new Pair<>(true, null);
        int required = amount;

        for(ServerInventorySlot slot : slots) {
            if(slot.item.itemId == id) {
                required -= slot.item.itemAmount;
                if(pair.value == null) pair.value = new LinkedList<>();
                pair.value.add(new Pair<>(slot.slotIndex, slot.item.itemAmount));
                if(required <= 0) return pair;
            }
        }

        pair.key = false;
        return pair;
    }

    /** Fills the inventory with random items and notifies the viewers if present. */
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

            if(MathUtils.random() <= 0.8f) {
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

        notifyViewersLazy();
    }

    public void dropAllItems(float radiusMin, float radiusMax) {
        dropAllItems(0, 0, radiusMin, radiusMax);
    }

    public void dropAllItems(float offsetX, float offsetY, float radiusMin, float radiusMax) {
        int dropItems = 0;

        for(var slot : slots) {
            if(!slot.item.isEmpty()) {
                dropItems++;
            }
        }

        Vector2[] positions = GenerationUtils.positions(dropItems, radiusMin, radiusMax);
        int i = 0;

        for(var slot : slots) {
            if(!slot.item.isEmpty()) {
                ServerItem drop = new ServerItem();
                drop.itemContainer = new ServerInventoryItem().clone(slot.item);
                slot.item.setEmpty();
                drop.posX = getOwner().posX + offsetX;
                drop.posY = getOwner().posY + offsetY;
                drop.dstX = positions[i].x;
                drop.dstY = positions[i].y;
                ServerWorld.get().registerServerEntity(getOwner().entityDimension, drop);
                i++;
            }
        }
    }

    /** Clears the inventory and notifies the viewers if present. */
    public void clear() {
        for(var slot : slots) {
            if(!slot.item.isEmpty()) {
                slot.item.setEmpty();
            }
        }

        notifyViewersLazy();
    }

    private void notifyViewersLazy() {
        int[] updatedSlots = new int[slots.length];
        for(int i = 0; i < updatedSlots.length; i++) updatedSlots[i] = i;

        ServerInventoryItem[] updated = new ServerInventoryItem[slots.length];
        for(int i = 0; i < updated.length; i++) updated[i] = slots[i].item;

        notifyViewers(updatedSlots, updated);
    }

    private void notifyViewers(int[] updatedSlots, ServerInventoryItem[] updated) {
        for(int viewer : viewerList) {
            ServerEntity viewerEntity = inventoryOwner.getDimension().getEntityManager().getEntityById(viewer);
            if(viewerEntity == null) continue;
            ServerPlayer player = (ServerPlayer) viewerEntity;
            ServerPackets.p19ContainerUpdate(containerId, updatedSlots, updated, PacketReceiver.player(player));
        }
    }

    private void notifyViewersExcept(int exceptPlayerId, int[] updatedSlots, ServerInventoryItem[] updated) {
        for(int viewer : viewerList) {
            if(exceptPlayerId == viewer) continue;
            ServerEntity viewerEntity = inventoryOwner.getDimension().getEntityManager().getEntityById(viewer);
            if(viewerEntity == null) continue;
            ServerPlayer player = (ServerPlayer) viewerEntity;
            ServerPackets.p19ContainerUpdate(containerId, updatedSlots, updated, PacketReceiver.player(player));
        }
    }

    private void notifyViewer(int viewer, int[] updatedSlots, ServerInventoryItem[] updated) {
        ServerEntity viewerEntity = inventoryOwner.getDimension().getEntityManager().getEntityById(viewer);
        if(viewerEntity == null) return;
        ServerPlayer player = (ServerPlayer) viewerEntity;
        ServerPackets.p19ContainerUpdate(containerId, updatedSlots, updated, PacketReceiver.player(player));
    }

    public int getContainerId() {
        return containerId;
    }

    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    public InventoryViewType getType() {
        return type;
    }

    public void setOwner(ServerEntity owner) {
        this.inventoryOwner = owner;
    }

    public ServerEntity getOwner() {
        return inventoryOwner;
    }

}