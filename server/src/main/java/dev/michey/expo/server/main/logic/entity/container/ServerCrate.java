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

public class ServerCrate extends ServerEntity {

    public EntityPhysicsBox physicsBody;

    public ServerInventory crateInventory;

    public ServerCrate() {
        health = 50.0f;
        setDamageableWith(ToolType.FIST, ToolType.AXE);

        crateInventory = new ServerInventory(InventoryViewType.CRATE, 9, ContainerRegistry.get().getNewUniqueContainerId());
        crateInventory.setOwner(this);
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -1, 0, 16, 10);
    }

    @Override
    public void onInteraction(ServerPlayer player) {
        crateInventory.addInventoryViewer(player);
        ServerPackets.p40InventoryView(crateInventory, PacketReceiver.player(player));
    }

    @Override
    public void onDie() {
        crateInventory.kickViewers();
        crateInventory.dropAllItems(7, 0, 8, 12);
        spawnItemSingle(posX + 8, posY + 2, 0, "item_crate", 8);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CRATE;
    }

    @Override
    public void onLoad(JSONObject saved) {
        InventoryFileLoader.loadFromStorage(crateInventory, (JSONArray) saved.get("inventory"));
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack()
                .add("inventory", InventoryFileLoader.toStorageObject(crateInventory))
                ;
    }

}
