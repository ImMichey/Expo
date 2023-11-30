package dev.michey.expo.server.main.logic.entity.player;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.fs.world.player.PlayerSaveFile;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.animal.ServerWorm;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.inventory.ServerInventory;
import dev.michey.expo.server.main.logic.world.bbox.*;
import dev.michey.expo.server.main.logic.entity.misc.ServerGravestone;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.ServerPlayerInventory;
import dev.michey.expo.server.main.logic.inventory.item.*;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;
import dev.michey.expo.server.main.logic.world.chunk.EntityVisibilityController;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.packet.P16_PlayerPunch;
import dev.michey.expo.server.util.*;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.*;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.*;

public class ServerPlayer extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public PlayerConnection playerConnection;
    public boolean localServerPlayer;
    public PlayerSaveFile playerSaveFile;
    public String username;

    public float playerSpeed = 56f;
    public final float sprintMultiplier = 1.6f;
    public int xDir = 0;
    public int yDir = 0;
    public boolean sprinting;
    private boolean dirResetPacket = false;

    public boolean noclip = false;
    public boolean god = false;

    public int playerDirection = 1; // default in client

    public boolean punching;
    public float startAngle;
    public float endAngle;
    public float punchDelta;
    public float punchDeltaFinish;
    private boolean punchDamageApplied;

    public float usePunchSpan;
    public float usePunchRange;
    public float usePunchDirection;
    public float usePunchDamage;
    public float usePunchKnockbackStrength;
    public float usePunchKnockbackDuration;
    public float convertedMiddleAngle;
    public float convertedStartAngle;

    public float serverArmRotation;

    public int selectedInventorySlot = 0;

    public float hunger = 100f;                 // actual hunger value, cannot exceed 100
    public float hungerCooldown = 180.0f;       // -> saturation that each hunger item gives
    public float nextHungerTickDown = 4.0f;     // seconds between each hunger down tick
    public float nextHungerDamageTick = 4.0f;   // seconds between each damage via hunger tick
    public float nextHealthRegenTickDown = 1.0f;// seconds between each health regen tick

    public int selectedEntity = -1;

    public HashMap<String, Long> hasSeenChunks = new HashMap<>();
    //public int[] currentlyVisibleChunks;
    public ServerChunk[] currentlyVisibleChunks;
    private final LinkedList<Pair<String, ServerChunk>> chunkPacketList = new LinkedList<>();

    public EntityVisibilityController entityVisibilityController = new EntityVisibilityController(this);

    public ServerPlayerInventory playerInventory = new ServerPlayerInventory(this);

    /** Physics body */
    private EntityPhysicsBox physicsBody;
    /** Hitbox */
    private EntityHitbox hitbox;
    private final LinkedList<Integer> hitEntities = new LinkedList<>();

    /** Inventory view */
    public ServerInventory viewingInventory = null;

    @Override
    public void onCreation() {
        // add physics body of player to world
        physicsBody = new EntityPhysicsBox(this, -3, 0, 6, 6);
        hitbox = EntityHitboxMapper.get().getFor(ServerEntityType.PLAYER);

        playerInventory.addInventoryViewer(this);
    }

    @Override
    public void onDeletion() {
        // remove physics body of player from world
        physicsBody.dispose();
    }

    @Override
    public void onDie() {
        playerInventory.dropAllItems(0, 4, 32, 32);
        playerInventory.clear();

        // update held item
        heldItemPacket(PacketReceiver.whoCanSee(this));

        ServerGravestone gravestone = new ServerGravestone();
        gravestone.owner = username;
        gravestone.posX = posX;
        gravestone.posY = posY;
        gravestone.setStaticEntity();
        ServerWorld.get().registerServerEntity(entityDimension, gravestone);

        // play sound
        ServerPackets.p24PositionalSound("player_death", posX, posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(this));

        // reset health + hunger
        health = getMetadata().getMaxHealth();
        hunger = 100.0f;
        hungerCooldown = 180.0f;
        nextHungerTickDown = 4.0f;
        nextHungerDamageTick = 4.0f;
        nextHealthRegenTickDown = 1.0f;

        if(knockbackCalculations != null) knockbackCalculations.clear();

        ServerPackets.p23PlayerLifeUpdate(health, hunger, PacketReceiver.player(this));

        teleportPlayer(
                getDimension().getDimensionSpawnX(),
                getDimension().getDimensionSpawnY(),
                TeleportReason.RESPAWN
        );

        // chat message
        ExpoServerBase.get().broadcastMessage("Player " + username + " died.");
    }

    public void teleportPlayer(float x, float y, TeleportReason teleportReason) {
        physicsBody.teleport(x, y);
        posX = x;
        posY = y;
        ServerPackets.p37EntityTeleport(entityId, posX, posY, teleportReason, PacketReceiver.whoCanSee(this));
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return physicsBody;
    }

    @Override
    public void onMoved() {

    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.PLAYER;
    }

    @Override
    public float movementSpeedMultiplicator() {
        if(noclip) return 1.0f;
        return super.movementSpeedMultiplicator();
    }

    @Override
    public void attemptMove(float x, float y) {
        attemptMove(x, y, PhysicsBoxFilters.playerCollisionFilter);
    }

    @Override
    public void tick(float delta) {
        updateChunks();

        /*
        if(hasSeenChunks.size() < (PLAYER_CHUNK_VIEW_RANGE_X * PLAYER_CHUNK_VIEW_RANGE_Y)) {
            onChunkChanged();
        }
        */

        tickKnockback(delta);

        if(invincibility > 0) invincibility -= delta;

        if(xDir != 0 || yDir != 0) {
            float multiplicator = movementSpeedMultiplicator() * (sprinting ? sprintMultiplier : 1.0f);
            boolean normalize = xDir != 0 && yDir != 0;
            float normalizer = 1.0f;

            if(normalize) {
                float len = (float) Math.sqrt(xDir * xDir + yDir * yDir);
                normalizer = 1 / len;
            }

            float toMoveX = xDir * delta * playerSpeed * multiplicator * normalizer;
            float toMoveY = yDir * delta * playerSpeed * multiplicator * normalizer;

            // Hook here and check if chunk exists as it's multithreaded as of 21.09.2023
            int chunkX = ExpoShared.posToChunk(toMoveX + posX);
            int chunkY = ExpoShared.posToChunk(toMoveY + posY);

            if(getChunkGrid().isActiveChunk(chunkX, chunkY)) {
                var result = physicsBody.move(toMoveX, toMoveY, noclip ? PhysicsBoxFilters.noclipFilter : PhysicsBoxFilters.playerCollisionFilter);

                posX = result.goalX - physicsBody.xOffset;
                posY = result.goalY - physicsBody.yOffset;

                ServerPackets.p13EntityMove(entityId, xDir, yDir, sprinting, posX, posY, 0, PacketReceiver.whoCanSee(this));
                dirResetPacket = true;
            }
        } else if(dirResetPacket) {
            dirResetPacket = false;
            ServerPackets.p13EntityMove(entityId, xDir, yDir, sprinting, posX, posY, 0, PacketReceiver.whoCanSee(this));
        }

        applyKnockback();

        if(punching) {
            punchDelta += delta;
            float punchInterpolated = Interpolation.circle.apply(punchDelta / punchDeltaFinish);

            if(punchInterpolated >= (punchDeltaFinish * 0.6f) && !punchDamageApplied) {
                punchDamageApplied = true;

                ServerEntity selected = getDimension().getEntityManager().getEntityById(selectedEntity);

                if(selected != null) {
                    int item = getCurrentItemId();
                    float dmg = ExpoShared.PLAYER_DEFAULT_ATTACK_DAMAGE;

                    if(selected.damageableWith != null) {
                        if(item != -1) {
                            ItemMapping mapping = ItemMapper.get().getMapping(item);

                            ToolType usingType = mapping.logic.toolType;
                            boolean used = false;
                            boolean fist = false;

                            for(ToolType checkFor : selected.damageableWith) {
                                if(usingType == checkFor) {
                                    dmg = mapping.logic.harvestDamage;
                                    if(selected.applyDamageWithPacket(this, dmg)) {
                                        useItemDurability(getCurrentItem());
                                    }
                                    used = true;
                                    break;
                                } else if(checkFor == ToolType.FIST) {
                                    fist = true;
                                }
                            }

                            if(!used && fist) {
                                selected.applyDamageWithPacket(this, dmg);
                            }
                        }  else {
                            for(ToolType checkFor : selected.damageableWith) {
                                if(checkFor == ToolType.FIST) {
                                    selected.applyDamageWithPacket(this, dmg);
                                    break;
                                }
                            }
                        }
                    } else {
                        selected.applyDamageWithPacket(this, dmg);
                    }
                }
            }

            if(punchDelta >= punchDeltaFinish) {
                punching = false;
                punchDamageApplied = false;
                hitEntities.clear();
            } else {
                // Apply damage to proximity entities
                float currentAngle = convertedStartAngle - usePunchSpan * punchDelta / punchDeltaFinish * usePunchDirection;
                float convertedAngle = currentAngle < 0 ? 360f + currentAngle : currentAngle;
                Collection<ServerEntity> check = getDimension().getEntityManager().getAllDamageableEntities();
                int removeDurability = 0;

                for(ServerEntity se : check) {
                    if(se.entityId == entityId) continue;
                    if(se.invincibility > 0) continue;
                    if(hitEntities.contains(se.entityId)) continue;
                    EntityHitbox hitbox = ((DamageableEntity) se).getEntityHitbox();

                    float ox = posX;
                    float oy = posY + 7f;

                    if(ServerUtils.rectIsInArc(ox, oy, hitbox.xOffset + se.posX, hitbox.yOffset + se.posY, hitbox.width, hitbox.height, usePunchRange, convertedStartAngle, convertedAngle, usePunchDirection, usePunchSpan)) {
                        // Hit.
                        hitEntities.add(se.entityId);
                        float preDamageHp = se.health;
                        boolean applied = se.applyDamageWithPacket(this, usePunchDamage);

                        if(applied) {
                            removeDurability++;
                            ServerPackets.p24PositionalSound("slap", se.posX, se.posY, PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(se));

                            // Apply knockback.
                            if(preDamageHp > se.health) {
                                se.addKnockback(usePunchKnockbackStrength, usePunchKnockbackDuration, new Vector2(se.posX, se.posY).sub(ox, oy).nor());
                            }

                            if(se instanceof ServerPlayer otherPlayer) {
                                ServerPackets.p23PlayerLifeUpdate(otherPlayer.health, otherPlayer.hunger, PacketReceiver.player(otherPlayer));
                            }
                        }
                    }
                }

                if(removeDurability > 0 && getCurrentItem().isBreakableItem()) {
                    useItemDurability(getCurrentItem());
                }
            }
        }

        if(hungerCooldown > 0) {
            hungerCooldown -= delta;
            if(hungerCooldown < 0) hungerCooldown = 0;
        } else {
            if(hunger > 0) {
                nextHungerTickDown -= delta;

                if(nextHungerTickDown <= 0) {
                    nextHungerTickDown += 4.0f;
                    removeHunger(1);
                    ServerPackets.p23PlayerLifeUpdate(health, hunger, PacketReceiver.player(this));
                }
            } else {
                // hunger == 0, do damage
                nextHungerDamageTick -= delta;

                if(nextHungerDamageTick <= 0) {
                    nextHungerDamageTick += 4.0f;
                    damagePlayer(1);
                    ServerPackets.p23PlayerLifeUpdate(health, hunger, PacketReceiver.player(this));
                }
            }
        }

        if(health < 100 && hunger > 90) {
            nextHealthRegenTickDown -= delta;

            if(nextHealthRegenTickDown <= 0) {
                nextHealthRegenTickDown += 1.0f;
                health += 1f;
                float maxHp = getMetadata().getMaxHealth();
                if(health > maxHp) health = maxHp;
                ServerPackets.p23PlayerLifeUpdate(health, hunger, PacketReceiver.player(this));
            }
        }

        if(viewingInventory != null) {
            ServerEntity owner = viewingInventory.getOwner();

            if(owner != null) {
                float dst = Vector2.dst(owner.posX + 8, owner.posY, posX, posY);

                float MAX_CONTAINER_DST = 48.0f;

                if(dst >= MAX_CONTAINER_DST) {
                    viewingInventory.removeInventoryViewer(this);
                    viewingInventory.kickViewer(entityId);
                    viewingInventory = null;
                }
            }
        }
    }

    public ServerInventoryItem getCurrentItem() {
        return playerInventory.slots[selectedInventorySlot].item;
    }

    public int getCurrentItemId() {
        return getCurrentItem().itemId;
    }

    public int[] getEquippedItemIds() {
        // [0] = hand
        // [1-6] = armor
        return new int[] {
                playerInventory.slots[selectedInventorySlot].item.itemId,
                playerInventory.slots[ExpoShared.PLAYER_INVENTORY_SLOT_HEAD].item.itemId,
                playerInventory.slots[ExpoShared.PLAYER_INVENTORY_SLOT_CHEST].item.itemId,
                playerInventory.slots[ExpoShared.PLAYER_INVENTORY_SLOT_GLOVES].item.itemId,
                playerInventory.slots[ExpoShared.PLAYER_INVENTORY_SLOT_LEGS].item.itemId,
                playerInventory.slots[ExpoShared.PLAYER_INVENTORY_SLOT_FEET].item.itemId
        };
    }

    public void switchToSlot(int slot) {
        selectedInventorySlot = slot;
        heldItemPacket(PacketReceiver.whoCanSee(this));
    }

    public void heldItemPacket(PacketReceiver receiver) {
        ServerPackets.p21PlayerGearUpdate(entityId, getEquippedItemIds(), receiver);
    }

    public void applyArmDirection(float rotation) {
        serverArmRotation = rotation;

        if(getCurrentItemId() != -1) {
            ServerPackets.p22PlayerArmDirection(entityId, serverArmRotation, PacketReceiver.whoCanSee(this));
        }
    }

    public void consumeFood(float hungerRestore, float hungerCooldown) {
        nextHungerTickDown = 4.0f;
        nextHungerDamageTick = 4.0f;
        if(hungerCooldown > this.hungerCooldown) {
            this.hungerCooldown = hungerCooldown;
        }
        hunger += hungerRestore;
        if(hunger > 100.0f) hunger = 100.0f;
    }

    public void damagePlayer(float damage) {
        health -= damage;
        if(health <= 0) {
            health = 0;
            onDie();
        }
    }

    public void removeHunger(float remove) {
        hunger -= remove;
        if(hunger < 0) hunger = 0;
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        if(god) return false;
        return super.onDamage(damageSource, damage);
    }

    public void parsePunchPacket(P16_PlayerPunch p) {
        punching = true;
        punchDamageApplied = false;
        hitEntities.clear();

        float attackSpeed = PLAYER_DEFAULT_ATTACK_SPEED;
        float attackRange = PLAYER_DEFAULT_RANGE;
        float attackDamage = PLAYER_DEFAULT_ATTACK_DAMAGE;
        float attackSpan = PLAYER_DEFAULT_ATTACK_ANGLE_SPAN;
        float attackKnockbackStrength = PLAYER_DEFAULT_ATTACK_KNOCKBACK_STRENGTH;
        float attackKnockbackDuration = PLAYER_DEFAULT_ATTACK_KNOCKBACK_DURATION;

        int id = getCurrentItemId();

        if(id != -1) {
            ItemMapping mapping = ItemMapper.get().getMapping(id);
            attackSpeed = mapping.logic.attackSpeed;
            attackRange = mapping.logic.range;
            attackDamage = mapping.logic.attackDamage;
            attackSpan = mapping.logic.attackAngleSpan;
            attackKnockbackStrength = mapping.logic.attackKnockbackStrength;
            attackKnockbackDuration = mapping.logic.attackKnockbackDuration;
        }

        {
            convertedMiddleAngle = p.punchAngle - 90f;
            if(convertedMiddleAngle < 0) convertedMiddleAngle = 360 + convertedMiddleAngle;

            if(convertedMiddleAngle >= 270f && convertedMiddleAngle <= 360f) {
                convertedStartAngle = convertedMiddleAngle + attackSpan / 2;
                if(convertedStartAngle > 360f) convertedStartAngle -= 360f;
            } else if(convertedMiddleAngle >= 90f && convertedMiddleAngle <= 270f) {
                convertedStartAngle = convertedMiddleAngle - attackSpan / 2;
            } else {
                convertedStartAngle = convertedMiddleAngle + attackSpan / 2;
            }

            usePunchSpan = attackSpan;
            usePunchDirection = 1;
            if(convertedMiddleAngle >= 90f && convertedMiddleAngle <= 270f) usePunchDirection = -1;
            usePunchRange = attackRange;
            usePunchDamage = attackDamage;
            usePunchKnockbackStrength = attackKnockbackStrength;
            usePunchKnockbackDuration = attackKnockbackDuration;
        }

        punchDeltaFinish = attackSpeed;
        startAngle = p.punchAngle - attackSpan / 2;
        endAngle = p.punchAngle + attackSpan / 2;
        punchDelta = 0;

        float _clientStart;
        float _clientEnd;

        if(p.punchAngle > 0) {
            _clientStart = 0;
            _clientEnd = 180;
        } else {
            _clientStart = -180;
            _clientEnd = 0;
        }

        ServerPackets.p17PlayerPunchData(entityId, _clientStart, _clientEnd, punchDeltaFinish, PacketReceiver.whoCanSee(this));
    }

    private void updateChunks() {
        chunkPacketList.clear();
        currentlyVisibleChunks = getChunkGrid().getChunksInPlayerRange(this);

        for(ServerChunk chunk : currentlyVisibleChunks) {
            if(chunk == null) continue; // If null, it is generating/loading from disk right now; otherwise it is fully loaded.

            // var pair = getChunkGrid().generatingChunkMap.get(chunk.getChunkKey());
            // boolean isGenerating = pair != null && pair.value.contains(entityId);
            boolean isNewChunk = !hasSeenChunks.containsKey(chunk.getChunkKey());
            boolean resend = false;

            if(!isNewChunk) {
                long cached = hasSeenChunks.get(chunk.getChunkKey());
                resend = cached < chunk.lastTileUpdate;
            }

            if(resend || isNewChunk) {
                hasSeenChunks.put(chunk.getChunkKey(), chunk.lastTileUpdate);
                chunkPacketList.add(new Pair<>(chunk.getChunkKey(), chunk));
            }
        }

        for(var pair : chunkPacketList) {
            ServerPackets.p11ChunkData(pair.value, PacketReceiver.player(this));
        }
    }

    private void playPlaceSound(String sound, float x, float y) {
        if(sound != null) {
            ServerPackets.p24PositionalSound(sound, x, y, PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(this));
        }
    }

    public void placeAt(int chunkX, int chunkY, int tileArray, float mouseWorldX, float mouseWorldY) {
        ServerInventoryItem item = getCurrentItem();
        ItemMapping m = ItemMapper.get().getMapping(item.itemId);
        if(m.logic.placeData == null) return; // to combat de-sync server<->client, double check current item

        ExpoLogger.log("-> " + tileArray + " " + mouseWorldX + " " + mouseWorldY);

        var chunk = getChunkGrid().getChunkSafe(chunkX, chunkY);
        var tile = chunk.tiles[tileArray];
        PlaceData p = m.logic.placeData;

        List<String> affectedChunks = new LinkedList<>();
        affectedChunks.add(chunk.getChunkKey());

        if(p.type == PlaceType.FLOOR_0) {
            // Update tile timestamp
            chunk.lastTileUpdate = System.currentTimeMillis();

            { // Update tile data
                tile.updateLayer0(p.floorType.TILE_LAYER_TYPE);
                ServerPackets.p32ChunkDataSingle(tile, 0);

                for(ServerTile st : tile.getNeighbouringTiles()) {
                    if(st.updateLayer0Adjacent(p.floorType.TILE_LAYER_TYPE.TILE_IS_WALL)) {
                        ServerPackets.p32ChunkDataSingle(st, 0);
                        if(!affectedChunks.contains(st.chunk.getChunkKey())) affectedChunks.add(st.chunk.getChunkKey());
                    }
                }
            }

            { // Update inventory
                useItemAmount(item);
            }

            playPlaceSound(p.sound, mouseWorldX, mouseWorldY);
        } else if(p.type == PlaceType.FLOOR_1) {
            // Update tile timestamp
            chunk.lastTileUpdate = System.currentTimeMillis();

            { // Update tile data
                tile.updateLayer1(p.floorType.TILE_LAYER_TYPE);
                ServerPackets.p32ChunkDataSingle(tile, 1);

                for(ServerTile st : tile.getNeighbouringTiles()) {
                    if(st.updateLayer1Adjacent(p.floorType.TILE_LAYER_TYPE.TILE_IS_WALL)) {
                        ServerPackets.p32ChunkDataSingle(st, 1);
                        if(!affectedChunks.contains(st.chunk.getChunkKey())) affectedChunks.add(st.chunk.getChunkKey());
                    }
                }
            }

            { // Update inventory
                useItemAmount(item);
            }

            playPlaceSound(p.sound, mouseWorldX, mouseWorldY);
        } else if(p.type == PlaceType.FLOOR_2) {
            // Update tile timestamp
            chunk.lastTileUpdate = System.currentTimeMillis();

            { // Update tile data
                tile.updateLayer2(p.floorType.TILE_LAYER_TYPE);
                ServerPackets.p32ChunkDataSingle(tile, 2);

                for(ServerTile st : tile.getNeighbouringTiles()) {
                    if(st.updateLayer2Adjacent(p.floorType.TILE_LAYER_TYPE.TILE_IS_WALL)) {
                        ServerPackets.p32ChunkDataSingle(st, 2);
                        if(!affectedChunks.contains(st.chunk.getChunkKey())) affectedChunks.add(st.chunk.getChunkKey());
                    }
                }
            }

            { // Update inventory
                useItemAmount(item);
            }

            playPlaceSound(p.sound, mouseWorldX, mouseWorldY);
        } else if(p.type == PlaceType.ENTITY) {
            if(p.alignment == PlaceAlignment.TILE) {
                boolean proceed = true;

                if(chunk.hasTileBasedEntities() && chunk.getTileBasedEntityIdGrid()[tileArray] != -1) {
                    proceed = false;
                }

                if(p.floorRequirement != null && proceed) {
                    proceed = false;

                    for(DynamicTilePart dtp : tile.dynamicTileParts) {
                        if(dtp.emulatingType == p.floorRequirement) {
                            proceed = true;
                            break;
                        }
                    }
                }

                ExpoLogger.log("Proceed, " + tile.tileX + " " + tile.tileY);

                if(proceed) {
                    // Proceed.
                    ServerEntity createdTileEntity = ServerEntityType.typeToEntity(p.entityType);
                    int x = tileArray % ROW_TILES;
                    int y = tileArray / ROW_TILES;
                    createdTileEntity.posX = ExpoShared.tileToPos(tile.tileX) + p.placeAlignmentOffsetX;
                    createdTileEntity.posY = ExpoShared.tileToPos(tile.tileY) + p.placeAlignmentOffsetY;
                    ServerWorld.get().registerServerEntity(entityDimension, createdTileEntity);
                    createdTileEntity.attachToTile(chunk, x, y);

                    useItemAmount(item);
                    playPlaceSound(p.sound, mouseWorldX, mouseWorldY);
                }
            } else {
                // Proceed.
                ServerEntity placedEntity = ServerEntityType.typeToEntity(p.entityType);
                placedEntity.posX = mouseWorldX + p.placeAlignmentOffsetX;
                placedEntity.posY = mouseWorldY + p.placeAlignmentOffsetY;
                ServerWorld.get().registerServerEntity(entityDimension, placedEntity);

                useItemAmount(item);
                playPlaceSound(p.sound, mouseWorldX, mouseWorldY);
            }
        }

        long now = System.currentTimeMillis();

        for(String affectedChunkKey : affectedChunks) {
            // Update tile timestamp
            ServerChunk sv = getChunkGrid().getActiveChunk(affectedChunkKey);
            sv.lastTileUpdate = now;

            for(ServerPlayer player : getDimension().getEntityManager().getAllPlayers()) {
                if(player.canSeeChunk(affectedChunkKey)) {
                    player.hasSeenChunks.put(affectedChunkKey, now);
                }
            }
        }
    }

    private int digLayer(ServerTile tile) {
        TileLayerType t1 = tile.dynamicTileParts[1].emulatingType;
        if(t1 == TileLayerType.GRASS || t1 == TileLayerType.FOREST) return 1;
        if(t1 == TileLayerType.SAND) return 1;

        TileLayerType t0 = tile.dynamicTileParts[0].emulatingType;
        if(t0 == TileLayerType.SOIL) return 0;

        return -1;
    }

    public void digAt(int chunkX, int chunkY, int tileArray) {
        ServerInventoryItem item = getCurrentItem();
        ToolType useTool = item.isTool(ToolType.SHOVEL, ToolType.SCYTHE);
        if(useTool == null) return; // to combat de-sync server<->client, double check current item

        var chunk = getChunkGrid().getChunkSafe(chunkX, chunkY);
        var tile = chunk.tiles[tileArray];
        int pColor = tile.toParticleColorId();
        int digLayer = digLayer(tile);

        if(digLayer != -1) {
            ItemMapping mapping = item.toMapping();
            boolean dugUp = tile.dig(digLayer, mapping.logic.attackDamage);

            { // Play dig up sound.
                String sound = TileLayerType.typeToHitSound(tile.dynamicTileParts[digLayer].emulatingType);
                tile.playTileSound(sound);
            }

            if(dugUp) {
                {
                    if(digLayer == 0 && (tile.biome == BiomeType.PLAINS || tile.biome == BiomeType.FOREST || tile.biome == BiomeType.DENSE_FOREST)) {
                        // SPAWN THE WORM!
                        if(MathUtils.random() <= 0.1f) {
                            ServerWorm worm = new ServerWorm();
                            worm.posX = ExpoShared.tileToPos(tile.tileX) + 8f;
                            worm.posY = ExpoShared.tileToPos(tile.tileY) + 4f;
                            ServerWorld.get().registerServerEntity(entityDimension, worm);
                            ServerPackets.p24PositionalSound("pop", worm.posX, worm.posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(getDimension(), chunkX, chunkY));
                        }
                    }

                    // Drop layer as item.
                    String identifier = TileLayerType.typeToItemDrop(tile.dynamicTileParts[digLayer].emulatingType);

                    if(identifier != null) {
                        spawnItemSingle(ExpoShared.tileToPos(tile.tileX) + 8.0f,
                                ExpoShared.tileToPos(tile.tileY) + 8.0f, 2.0f, identifier, 3.0f);
                    }
                }

                List<String> affectedChunks = new LinkedList<>();
                affectedChunks.add(chunk.getChunkKey());

                { // Update tile data
                    if(digLayer == 0 && tile.dynamicTileParts[digLayer].emulatingType == TileLayerType.SOIL) {
                        tile.updateLayer0(useTool == ToolType.SCYTHE ? TileLayerType.SOIL_FARMLAND : TileLayerType.SOIL_HOLE);
                        ServerPackets.p32ChunkDataSingle(tile, 0);

                        for(ServerTile neighbour : tile.getNeighbouringTiles()) {
                            if(neighbour.updateLayer0Adjacent(false)) {
                                ServerPackets.p32ChunkDataSingle(neighbour, 0);
                                if(!affectedChunks.contains(neighbour.chunk.getChunkKey())) affectedChunks.add(neighbour.chunk.getChunkKey());
                            }
                        }
                    } else if(digLayer == 1) {
                        tile.updateLayer1(TileLayerType.EMPTY);
                        ServerPackets.p32ChunkDataSingle(tile, 1);

                        for(ServerTile neighbour : tile.getNeighbouringTiles()) {
                            if(neighbour.updateLayer1Adjacent(false)) {
                                ServerPackets.p32ChunkDataSingle(neighbour, 1);
                                if(!affectedChunks.contains(neighbour.chunk.getChunkKey())) affectedChunks.add(neighbour.chunk.getChunkKey());
                            }
                        }
                    }

                }

                long now = System.currentTimeMillis();

                for(String affectedChunkKey : affectedChunks) {
                    // Update tile timestamp
                    ServerChunk sv = getChunkGrid().getActiveChunk(affectedChunkKey);
                    sv.lastTileUpdate = now;

                    for(ServerPlayer player : getDimension().getEntityManager().getAllPlayers()) {
                        if(player.canSeeChunk(affectedChunkKey)) {
                            player.hasSeenChunks.put(affectedChunkKey, now);
                        }
                    }
                }
            }

            { // Dig packet.
                ServerPackets.p33TileDig(tile.tileX, tile.tileY, pColor, PacketReceiver.whoCanSee(getDimension(), chunkX, chunkY));
            }

            { // Update player inventory/item.
                useItemDurability(item);
            }
        }
    }

    public boolean canSeeChunk(String chunkKey) {
        if(currentlyVisibleChunks != null) {
            for(ServerChunk chunk : currentlyVisibleChunks) {
                if(chunk == null) continue;
                if(chunk.getChunkKey().equals(chunkKey)) return true;
            }
        }

        return false;
    }

    private void useItemAmount(ServerInventoryItem item) {
        item.itemAmount -= 1;
        boolean itemNowUsedUp = item.itemAmount <= 0;

        if(itemNowUsedUp) {
            playerInventory.slots[selectedInventorySlot].item.setEmpty();
            heldItemPacket(PacketReceiver.whoCanSee(this));
        }

        ServerPackets.p19ContainerUpdate(ExpoShared.CONTAINER_ID_PLAYER, new int[] {selectedInventorySlot}, new ServerInventoryItem[] {item}, PacketReceiver.player(this));
    }

    public void useItemDurability(ServerInventoryItem item) {
        item.itemMetadata.durability -= 1;
        boolean itemNowBroken = item.itemMetadata.durability <= 0;

        if(itemNowBroken) {
            playerInventory.slots[selectedInventorySlot].item.setEmpty();
            heldItemPacket(PacketReceiver.whoCanSee(this));
        }

        ServerPackets.p19ContainerUpdate(ExpoShared.CONTAINER_ID_PLAYER, new int[] {selectedInventorySlot}, new ServerInventoryItem[] {item}, PacketReceiver.player(this));
    }

    public float toFeetCenterX() {
        return posX;
    }

    public float toFeetCenterY() {
        return posY;
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return hitbox;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.PLAYER;
    }

    public void updateSaveFile() {
        log("Updating save file for " + username);
        playerSaveFile.getHandler().update("posX", posX);
        playerSaveFile.getHandler().update("posY", posY);
        playerSaveFile.getHandler().update("dimensionName", entityDimension);
        playerSaveFile.getHandler().update("entityId", entityId);
        playerSaveFile.getHandler().update("hunger", hunger);
        playerSaveFile.getHandler().update("hungerCooldown", hungerCooldown);
        playerSaveFile.getHandler().update("health", health);
        playerSaveFile.getHandler().update("nextHungerTickDown", nextHungerTickDown);
        playerSaveFile.getHandler().update("nextHungerDamageTick", nextHungerDamageTick);
        playerSaveFile.getHandler().update("nextHealthRegenTickDown", nextHealthRegenTickDown);
        if(!localServerPlayer) {
            playerSaveFile.getHandler().update("username", username);
        }
        playerSaveFile.getHandler().update("inventory", InventoryFileLoader.toStorageObject(playerInventory));

        if(playerSaveFile.getHandler().fileJustCreated) {
            playerSaveFile.getHandler().onTermination();
            playerSaveFile.getHandler().fileJustCreated = false;
        }
    }

    /** Code below only works in a local server environment. */
    private static ServerPlayer LOCAL_PLAYER;

    public static void setLocalPlayer(ServerPlayer localPlayer) {
        LOCAL_PLAYER = localPlayer;
    }

    public static ServerPlayer getLocalPlayer() {
        return LOCAL_PLAYER;
    }

}