package dev.michey.expo.server.main.logic.inventory;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class ServerInventory {

    /** What entity does the inventory belong to (player, mob, chest, etc.)? */
    private ServerEntity inventoryOwner = null;
    private final HashSet<Integer> viewerList;
    private final InventoryViewType type;

    /** Data structure */
    public final ServerInventorySlot[] slots;

    public ServerInventory(InventoryViewType type, int size) {
        this.type = type;
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
    }

    public void removeInventoryViewer(ServerPlayer player) {
        viewerList.remove(player.entityId);
    }

    public void kickViewers() {
        for(int viewer : viewerList) {
            ServerEntity viewerEntity = inventoryOwner.getDimension().getEntityManager().getEntityById(viewer);
            if(viewerEntity == null) continue;
            ServerPlayer player = (ServerPlayer) viewerEntity;
            ServerPackets.p41InventoryViewQuit(PacketReceiver.player(player));
        }
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
                drop.posX = getOwner().posX;
                drop.posY = getOwner().posY;
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
            ServerPackets.p19PlayerInventoryUpdate(updatedSlots, updated, PacketReceiver.player(player));
        }
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