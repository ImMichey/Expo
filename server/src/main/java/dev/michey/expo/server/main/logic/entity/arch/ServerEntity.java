package dev.michey.expo.server.main.logic.entity.arch;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.bbox.KnockbackCalculation;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunkGrid;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.util.*;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.weather.Weather;
import org.json.JSONObject;

import java.util.*;

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public abstract class ServerEntity {

    /** Generated by ServerEntityManager. */
    public int entityId;
    public String entityDimension;
    public boolean persistentEntity = true;

    /** Handled by the dimension visibility handler */
    public boolean trackedVisibility = false;
    public boolean changedChunk = false;

    /** ServerEntity base fields */
    public float posX;
    public float posY;
    public boolean staticPosition = false;
    public int chunkX;
    public int chunkY;
    public boolean forceChunkChange = false;
    public boolean tileEntity = false;
    public int tileX;
    public int tileY;
    public float health;
    public ToolType[] damageableWith = null;

    /** Knockback fields */
    public float invincibility = 0.75f;
    public List<KnockbackCalculation> knockbackCalculations;
    public List<KnockbackCalculation> removeKnockback;

    /** ServerEntity base methods */
    public void tick(float delta) {

    }

    public void onCreation() {

    }

    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {

    }

    public void onDeletion() {

    }

    public abstract ServerEntityType getEntityType();

    public void onChunkChanged() {

    }

    public boolean onDamage(ServerEntity damageSource, float damage) {
        return true;
    }

    public void onDie() {

    }

    public SavableEntity onSave() {
        return null;
    }

    public void onLoad(JSONObject saved) {

    }

    public Object[] getPacketPayload() {
        return null;
    }

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

    public void spawnEntitiesAround(int min, int max, float radiusMin, float radiusMax, ServerEntityType type) {
        int amount = MathUtils.random(min, max);
        Vector2[] positions = GenerationUtils.positions(amount, MathUtils.random(radiusMin, radiusMax));

        for(int i = 0; i < amount; i++) {
            ServerEntity spawned = ServerEntityType.typeToEntity(type);
            spawned.posX = posX + positions[i].x;
            spawned.posY = posY + positions[i].y;
            ServerWorld.get().registerServerEntity(entityDimension, spawned);
        }
    }

    public void spawnItemsAround(int min, int max, float xOff, float yOff, String itemName, float radius) {
        spawnItemsAround(min, max, xOff, yOff, itemName, radius, radius);
    }

    public void spawnItemsAround(int min, int max, float xOff, float yOff, String itemName, float radiusMin, float radiusMax) {
        int amount = MathUtils.random(min, max);
        Vector2[] positions = GenerationUtils.positions(amount, MathUtils.random(radiusMin, radiusMax));

        for(int i = 0; i < amount; i++) {
            ServerItem item = new ServerItem();

            ItemMapping r = ItemMapper.get().getMapping(itemName);
            item.itemContainer = new ServerInventoryItem(r.id, 1);
            item.posX = posX + xOff;
            item.posY = posY + yOff;
            item.dstX = positions[i].x;
            item.dstY = positions[i].y;
            ServerWorld.get().registerServerEntity(entityDimension, item);
        }
    }

    public void spawnItemSingle(float originX, float originY, float originOffsetRadius, String itemName, float moveToRadius) {
        Vector2 position = GenerationUtils.circularRandom(moveToRadius);
        Vector2 offset = GenerationUtils.circularRandom(originOffsetRadius);

        ServerItem item = new ServerItem();

        ItemMapping r = ItemMapper.get().getMapping(itemName);
        item.itemContainer = new ServerInventoryItem(r.id, 1);
        item.posX = originX + offset.x;
        item.posY = originY + offset.y;
        item.dstX = position.x;
        item.dstY = position.y;

        ServerWorld.get().registerServerEntity(entityDimension, item);
    }

    public void spawnItemsAlongLine(float startX, float startY, float maxDistanceX, float maxDistanceY, float radiusDst, SpawnItem... spawnItems) {
        int total = 0;

        for(SpawnItem item : spawnItems) {
            total += item.amount;
        }

        List<ServerInventoryItem> items = new ArrayList<>(total);

        for(SpawnItem item : spawnItems) {
            for(int i = 0; i < item.amount; i++) {
                items.add(new ServerInventoryItem(item.id, 1));
            }
        }

        Collections.shuffle(items);
        float perX = maxDistanceX / (float) total;
        float perY = maxDistanceY / (float) total;

        for(int i = 0; i < total; i++) {
            ServerItem item = new ServerItem();
            item.itemContainer = items.get(i);

            float nextX = startX + perX * i;
            float nextY = startY + perY * i;

            Vector2 dst = GenerationUtils.circularRandom(radiusDst);

            item.posX = nextX;
            item.posY = nextY;
            item.dstX = dst.x;
            item.dstY = dst.y;
            ServerWorld.get().registerServerEntity(entityDimension, item);
        }
    }

    public void spawnItemsAround(float xOff, float yOff, float radiusMin, float radiusMax, SpawnItem... spawnItems) {
        int total = 0;

        for(SpawnItem item : spawnItems) {
            if(item == null) continue;
            total += item.amount;
        }

        List<ServerInventoryItem> items = new ArrayList<>(total);

        for(SpawnItem item : spawnItems) {
            if(item == null) continue;
            for(int i = 0; i < item.amount; i++) {
                items.add(new ServerInventoryItem(item.id, 1));
            }
        }

        Collections.shuffle(items);

        Vector2[] positions = GenerationUtils.positions(total, MathUtils.random(radiusMin, radiusMax));

        for(int i = 0; i < total; i++) {
            ServerItem item = new ServerItem();
            item.itemContainer = items.get(i);

            item.posX = posX + xOff;
            item.posY = posY + yOff;
            item.dstX = positions[i].x;
            item.dstY = positions[i].y;
            ServerWorld.get().registerServerEntity(entityDimension, item);
        }
    }

    public boolean isInWater() {
        BiomeType b = getTileBiome();
        return b == BiomeType.LAKE || b == BiomeType.OCEAN || b == BiomeType.RIVER || b == BiomeType.PUDDLE;
    }

    public boolean isInDeepWater() {
        BiomeType b = getTileBiome();
        return b == BiomeType.OCEAN_DEEP;
    }

    public void attemptMove(float x, float y) {
        attemptMove(x, y, PhysicsBoxFilters.generalFilter);
    }

    public void attemptMove(float x, float y, CollisionFilter filter) {
        float dst = Math.abs(x) + Math.abs(y);

        if(this instanceof PhysicsEntity pe) {
            float opx = posX;
            float opy = posY;

            EntityPhysicsBox epb = pe.getPhysicsBox();
            var result = epb.move(x, y, filter);

            float tx = result.goalX - epb.xOffset;
            float ty = result.goalY - epb.yOffset;

            // Check if valid chunk, otherwise teleport back to old location.
            int chunkX = ExpoShared.posToChunk(tx);
            int chunkY = ExpoShared.posToChunk(ty);

            if(getChunkGrid().isActiveChunk(chunkX, chunkY)) {
                posX = tx;
                posY = ty;
                ServerPackets.p13EntityMove(entityId, velToPos(x), velToPos(y), posX, posY, dst, PacketReceiver.whoCanSee(this));
            } else {
                epb.teleport(opx, opy);
            }
        } else {
            float tx = posX + x;
            float ty = posY + y;

            // Check if valid chunk.
            int chunkX = ExpoShared.posToChunk(tx);
            int chunkY = ExpoShared.posToChunk(ty);

            if(getChunkGrid().isActiveChunk(chunkX, chunkY)) {
                posX = tx;
                posY = ty;
                ServerPackets.p13EntityMove(entityId, velToPos(x), velToPos(y), posX, posY, dst, PacketReceiver.whoCanSee(this));
            }
        }
    }

    public float movementSpeedMultiplicator() {
        boolean water = isInWater();
        if(water) return 0.6f;

        boolean deepWater = isInDeepWater();
        if(deepWater) return 0.3f;

        int tileX = ExpoShared.posToTile(posX);
        int tileY = ExpoShared.posToTile(posY);
        ServerTile t = getChunkGrid().getTile(tileX, tileY);
        if(t == null) {
            ExpoLogger.log("NULL " + tileX + " " + tileY + " ( " + posX + "," + posY + ")");
        }
        if(t.dynamicTileParts == null) {
            ExpoLogger.log("DYNAMIC PARTS NULL " + t.tileX + "," + t.tileY + ", " + posX + "," + posY);
        }
        boolean hole = t.dynamicTileParts[0].emulatingType == TileLayerType.SOIL_HOLE;
        if(hole) return 0.75f;

        return 1.0f;
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

    public boolean applyDamageWithPacket(ServerEntity damageSource, float damage) {
        boolean applied = onDamage(damageSource, damage);

        if(applied) {
            health -= damage;

            if(health <= 0) {
                killEntityWithAdvancedPacket(damage, health, damageSource.entityId);
            } else {
                ServerPackets.p26EntityDamage(entityId, damage, health, damageSource.entityId, PacketReceiver.whoCanSee(this));
            }
        }

        return applied;
    }

    public void onInteraction(ServerPlayer player) {

    }

    public void addKnockback(float knockbackStrength, float knockbackDuration, Vector2 knockbackDirection) {
        if(knockbackCalculations == null) {
            knockbackCalculations = new LinkedList<>();
            removeKnockback = new LinkedList<>();
        }

        knockbackCalculations.add(new KnockbackCalculation(knockbackStrength, knockbackDuration, knockbackDirection));
    }

    public void tickKnockback(float delta) {
        if(knockbackCalculations == null) return;

        for(KnockbackCalculation kc : removeKnockback) {
            knockbackCalculations.remove(kc);
        }
        removeKnockback.clear();

        for(var k : knockbackCalculations) {
            if(k.tick(delta)) {
                removeKnockback.add(k);
            }
        }
    }

    public void applyKnockback() {
        if(knockbackCalculations == null) return;
        float moveByX = 0, moveByY = 0;

        for(KnockbackCalculation kc : knockbackCalculations) {
            moveByX += kc.applyKnockbackX;
            moveByY += kc.applyKnockbackY;
        }

        if(moveByX != 0 || moveByY != 0) {
            attemptMove(moveByX, moveByY);
        }
    }

    public void movePhysicsBoxBy(EntityPhysicsBox box, float x, float y) {
        var result = box.move(x, y, PhysicsBoxFilters.generalFilter);
        posX = result.goalX - box.xOffset;
        posY = result.goalY - box.yOffset;
    }

    public int velToPos(float vel) {
        if(vel > 0) return 1;
        if(vel < 0) return -1;
        return 0;
    }

    public void killEntityWithPacket() {
        killEntityWithPacket(EntityRemovalReason.DEATH);
    }

    public void killEntityWithAdvancedPacket(float damage, float newHealth, int damageSourceEntityId) {
        // Detach from internal tile entity structure if existing
        if(tileEntity) detachFromTile(getChunkGrid().getChunkSafe(chunkX, chunkY));

        onDie();

        if(getEntityType() != ServerEntityType.PLAYER) {
            getDimension().getEntityManager().removeEntitySafely(this);
            ServerPackets.p43EntityDeleteAdvanced(entityId, EntityRemovalReason.DEATH, damage, newHealth, damageSourceEntityId, PacketReceiver.whoCanSee(this));
            // untrack entity
            for(ServerPlayer player : getDimension().getEntityManager().getAllPlayers()) {
                player.entityVisibilityController.removeTrackedEntity(entityId);
            }
        }
    }

    public void killEntityWithPacket(EntityRemovalReason reason) {
        // Detach from internal tile entity structure if existing
        if(tileEntity) detachFromTile(getChunkGrid().getChunkSafe(chunkX, chunkY));

        onDie();

        if(getEntityType() != ServerEntityType.PLAYER) {
            getDimension().getEntityManager().removeEntitySafely(this);
            ServerPackets.p4EntityDelete(entityId, reason, PacketReceiver.whoCanSee(this));
            // untrack entity
            for(ServerPlayer player : getDimension().getEntityManager().getAllPlayers()) {
                player.entityVisibilityController.removeTrackedEntity(entityId);
            }
        }
    }

    public boolean postGeneration() {
        return true;
    }

    public void setDamageableWith(ToolType... types) {
        damageableWith = types;
    }

    public void applyDamageToArea(float[] damageAreaVertices, float damage, float knockbackStrength, float knockbackDuration, boolean useEntityOriginX, boolean useEntityOriginY) {
        // Apply damage to proximity entities
        Collection<ServerEntity> check = getDimension().getEntityManager().getAllDamageableEntities();
        Vector2 dirVector = new Vector2();

        float originX = (damageAreaVertices[2] - damageAreaVertices[0]) * 0.5f + damageAreaVertices[0];
        float originY = (damageAreaVertices[3] - damageAreaVertices[1]) * 0.5f + damageAreaVertices[1];

        for(ServerEntity se : check) {
            if(se.entityId == entityId) continue;
            if(se.invincibility > 0) continue;
            EntityHitbox hitbox = ((DamageableEntity) se).getEntityHitbox();

            float[] entityVertices = hitbox.toWorld(se.posX, se.posY);
            boolean isHit = ExpoShared.overlap(damageAreaVertices, entityVertices);

            if(isHit) {
                float preDamageHp = se.health;
                boolean applied = se.applyDamageWithPacket(this, damage);

                if(applied) {
                    // Sound effect
                    ServerPackets.p24PositionalSound("bonk", se.posX, se.posY, PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(se));

                    // Knockback
                    if(preDamageHp > se.health) {
                        se.addKnockback(knockbackStrength, knockbackDuration, dirVector.set(se.posX, se.posY)
                                .sub(useEntityOriginX ? se.posX : originX, useEntityOriginY ? se.posY : originY).nor());
                    }

                    // Player packet
                    if(se instanceof ServerPlayer player) {
                        ServerPackets.p23PlayerLifeUpdate(player.health, player.hunger, PacketReceiver.player(player));
                    }
                }
            }
        }
    }

    public EntityMetadata getMetadata() {
        return EntityMetadataMapper.get().getFor(getEntityType());
    }

    public ServerTile getCurrentTile() {
        return getChunkGrid().getTile(ExpoShared.posToTile(posX), ExpoShared.posToTile(posY));
    }

    public boolean isRaining() {
        return getDimension().dimensionWeather == Weather.RAIN;
    }

}
