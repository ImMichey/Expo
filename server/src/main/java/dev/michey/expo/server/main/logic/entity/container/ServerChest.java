package dev.michey.expo.server.main.logic.entity.container;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.InventoryViewType;
import dev.michey.expo.server.main.logic.inventory.ServerInventory;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import org.json.JSONArray;
import org.json.JSONObject;

public class ServerChest extends ServerEntity {

    public EntityPhysicsBox physicsBody;

    public ServerInventory chestInventory;

    public ServerChest() {
        health = 50.0f;
        setDamageableWith(ToolType.FIST, ToolType.AXE);

        chestInventory = new ServerInventory(InventoryViewType.CHEST, 9, ContainerRegistry.get().getNewUniqueContainerId());
        chestInventory.setOwner(this);
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, 0, 0, 17, 9.75f);
    }

    @Override
    public void onInteraction(ServerPlayer player) {
        chestInventory.addInventoryViewer(player);
        ServerPackets.p40InventoryView(chestInventory, PacketReceiver.player(player));
    }

    @Override
    public void onDie() {
        chestInventory.kickViewers();
        chestInventory.dropAllItems(7, 0, 8, 12);
        spawnItemSingle(posX + 8, posY + 2, 0, "item_chest", 8);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CHEST;
    }

    @Override
    public void onLoad(JSONObject saved) {
        InventoryFileLoader.loadFromStorage(chestInventory, (JSONArray) saved.get("inventory"));
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack()
                .add("inventory", InventoryFileLoader.toStorageObject(chestInventory))
                ;
    }

}
