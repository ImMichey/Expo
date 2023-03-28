package dev.michey.expo.server.main.logic.entity.arch;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunkGrid;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.log.ExpoLogger.log;

public abstract class ServerEntity {

    /** Generated by ServerEntityManager. */
    public int entityId;
    public String entityDimension;

    /** Handled by the dimension visibility handler */
    public boolean trackedVisibility = false;
    public boolean changedChunk = false;

    /** ServerEntity base fields */
    public float posX;
    public float posY;
    public boolean staticPosition = false;
    public int chunkX;
    public int chunkY;
    public boolean tileEntity = false;
    public int tileX;
    public int tileY;
    public float health;
    public ToolType damageableWith = null;

    /** ServerEntity base methods */
    public abstract void tick(float delta);
    public abstract void onCreation();
    public abstract void onDeletion();
    public abstract ServerEntityType getEntityType();
    public abstract void onChunkChanged();
    public abstract void onDamage(ServerEntity damageSource, float damage);
    public abstract void onDie();
    public abstract SavableEntity onSave();

    /** ServerEntity helper methods */
    public ServerDimension getDimension() {
        return ServerWorld.get().getDimension(entityDimension);
    }

    public ServerChunkGrid getChunkGrid() {
        return getDimension().getChunkHandler();
    }

    public BiomeType getTileBiome() {
        int tx = ExpoShared.posToTile(posX);
        int ty = ExpoShared.posToTile(posY);
        return getChunkGrid().getBiome(tx, ty);
    }

    public boolean isInWater() {
        BiomeType b = getTileBiome();
        return b == BiomeType.LAKE || b == BiomeType.OCEAN || b == BiomeType.RIVER;
    }

    public void setStaticEntity() {
        staticPosition = true;
        chunkX = ExpoShared.posToChunk(posX);
        chunkY = ExpoShared.posToChunk(posY);
    }

    public void attachToTile(ServerChunk chunk, int tileX, int tileY) {
        tileEntity = true;
        this.tileX = tileX;
        this.tileY = tileY;
        chunk.attachTileBasedEntity(entityId, tileX, tileY);
    }

    public void detachFromTile(ServerChunk chunk) {
        tileEntity = false;
        chunk.detachTileBasedEntity(tileX, tileY);
        tileX = 0;
        tileY = 0;
    }

    public void applyDamageWithPacket(ServerEntity damageSource, float damage) {
        health -= damage;
        onDamage(damageSource, damage);
        ServerPackets.p26EntityDamage(entityId, damage, health, PacketReceiver.whoCanSee(this));

        if(health <= 0) {
            onDie();
            ServerPackets.p4EntityDelete(entityId, EntityRemovalReason.DEATH, PacketReceiver.whoCanSee(this));
        }
    }

}
