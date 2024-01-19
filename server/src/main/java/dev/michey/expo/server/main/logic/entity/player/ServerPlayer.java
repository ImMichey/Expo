package dev.michey.expo.server.main.logic.entity.player;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.fs.world.player.PlayerSaveFile;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.entity.misc.ServerGravestone;
import dev.michey.expo.server.main.logic.entity.misc.ServerThrownEntity;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.ServerInventory;
import dev.michey.expo.server.main.logic.inventory.ServerPlayerInventory;
import dev.michey.expo.server.main.logic.inventory.item.*;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.bbox.*;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;
import dev.michey.expo.server.main.logic.world.chunk.EntityVisibilityController;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.packet.P16_PlayerPunch;
import dev.michey.expo.server.packet.P48_ClientPlayerPosition;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.server.util.ServerUtils;
import dev.michey.expo.server.util.TeleportReason;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.*;

public class ServerPlayer extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public PlayerConnection playerConnection;
    public boolean localServerPlayer;
    public PlayerSaveFile playerSaveFile;
    public String username;

    public float playerSpeed = 64f;
    public int xDir = 0;
    public int yDir = 0;
    public boolean sprinting;
    private boolean dirResetPacket = false;
    private P48_ClientPlayerPosition nextMovementPacket;

    public boolean noclip = false;
    public boolean god = false;

    public int playerDirection = 1; // default in client

    public boolean queuedPunchStatus;
    public boolean punching;
    public float startAngle;
    public float endAngle;
    public float punchDelta;
    public float punchDeltaFinish = -1;
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

    public boolean itemCooldown;

    public HashMap<ServerChunk, Long> hasSeenChunks = new HashMap<>();
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
        resetInvincibility();
    }

    @Override
    public void onDeletion() {
        // remove physics body of player from world
        physicsBody.dispose();

        // Maybe move this code somewhere else in the future
        getChunkGrid().removeFromChunkMemoryMap(this);
    }

    @Override
    public void onDie() {
        playerInventory.dropAllItems(0, 4, 32, 32);
        playerInventory.clear();

        // update held item
        heldItemPacket(PacketReceiver.whoCanSee(this));

        ServerGravestone gravestone = new ServerGravestone();
        gravestone.owner = username;
        gravestone.posX = (int) posX;
        gravestone.posY = (int) posY;
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

        if(knockbackCalculations != null) {
            knockbackCalculations.clear();
            knockbackCalculations = null;
            removeKnockback.clear();
            removeKnockback = null;
        }

        ServerPackets.p23PlayerLifeUpdate(health, hunger, PacketReceiver.player(this));

        teleportPlayer(
                getDimension().getDimensionSpawnX(),
                getDimension().getDimensionSpawnY(),
                TeleportReason.RESPAWN
        );

        resetInvincibility();
        nextMovementPacket = null;

        // chat message
        ExpoServerBase.get().broadcastMessage("Player " + username + " died.");
    }

    public void teleportPlayer(float x, float y, TeleportReason teleportReason) {
        // Ensure chunk is loaded.
        getChunkGrid().getChunkSafe(ExpoShared.posToChunk(x), ExpoShared.posToChunk(y));

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
    public Object[] getPacketPayload() {
        return new Object[] {playerSpeed, noclip};
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

    private void applyKnockbackPlayer(int dirX, int dirY) {
        if(knockbackCalculations == null) return;
        float moveByX = 0, moveByY = 0;

        for(KnockbackCalculation kc : knockbackCalculations) {
            moveByX += kc.applyKnockbackX;
            moveByY += kc.applyKnockbackY;
        }

        if(moveByX != 0 || moveByY != 0) {
            attemptMove(moveByX, moveByY, PhysicsBoxFilters.playerKnockbackFilter, dirX, dirY);
        }
    }

    @Override
    public void tick(float delta) {
        long a = System.nanoTime();
        updateChunks();
        itemCooldown = false;

        tickKnockback(delta);

        if(invincibility > 0) invincibility -= delta;

        if(nextMovementPacket != null) {
            float dstX = nextMovementPacket.xPos;
            float dstY = nextMovementPacket.yPos;
            xDir = nextMovementPacket.xDir;
            yDir = nextMovementPacket.yDir;
            nextMovementPacket = null;

            if(getChunkGrid().isActiveChunk(ExpoShared.posToChunk(dstX), ExpoShared.posToChunk(dstY))) {
                physicsBody.moveAbsolute(dstX, dstY, PhysicsBoxFilters.noclipFilter);
                posX = dstX;
                posY = dstY;

                ServerPackets.p13EntityMove(entityId, xDir, yDir, sprinting, posX, posY, 0, PacketReceiver.whoCanSeeExcept(this, this));
                dirResetPacket = true;
            }
        } else if(dirResetPacket) {
            xDir = 0;
            yDir = 0;
            dirResetPacket = false;
            ServerPackets.p13EntityMove(entityId, xDir, yDir, sprinting, posX, posY, 0, PacketReceiver.whoCanSeeExcept(this, this));
        }

        applyKnockbackPlayer(velToPos(xDir), velToPos(yDir));

        evaluateNextPunch();

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
                    float oy = posY;// + 7f;

                    if(ServerUtils.rectIsInArc(ox, oy, hitbox.xOffset + se.posX, hitbox.yOffset + se.posY, hitbox.width, hitbox.height, usePunchRange, convertedStartAngle, convertedAngle, usePunchDirection, usePunchSpan)) {
                        // Hit.
                        hitEntities.add(se.entityId);
                        float preDamageHp = se.health;
                        boolean applied = se.applyDamageWithPacket(this, usePunchDamage);

                        if(applied) {
                            removeDurability++;
                            ServerPackets.p24PositionalSound(se.getImpactSound(), se.posX, se.posY, PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(se));

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

        long b = System.nanoTime();
        if(TRACK_PERFORMANCE) {
            ServerUtils.recordPerformanceMetric("player" + username, new String[] {"total"}, new long[] {a, b});
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

    public void consumeFood(float hungerRestore, float hungerCooldown, float healthRestore) {
        nextHungerTickDown = 4.0f;
        nextHungerDamageTick = 4.0f;
        if(hungerCooldown > 0) {
            if(hungerCooldown > this.hungerCooldown) {
                this.hungerCooldown = hungerCooldown;
            }
        }
        hunger += hungerRestore;
        if(hunger > 100.0f) hunger = 100.0f;
        health += healthRestore;
        if(health > getMetadata().getMaxHealth()) health = getMetadata().getMaxHealth();
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

    public void consumePosition(P48_ClientPlayerPosition p) {
        nextMovementPacket = p;
    }

    private void evaluateNextPunch() {
        if(queuedPunchStatus) {
            // We are currently in punch mode
            punching = true;

            if(punchDelta > punchDeltaFinish) {
                // We finished our last punch, instantiate new punch
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
                    //convertedMiddleAngle = p.punchAngle - 90f;
                    convertedMiddleAngle = serverArmRotation - 90f;
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
                startAngle = serverArmRotation - attackSpan / 2;
                endAngle = serverArmRotation + attackSpan / 2;
                punchDelta = 0;

                float _clientStart;
                float _clientEnd;

                if(serverArmRotation > 0) {
                    _clientStart = 0;
                    _clientEnd = 180;
                } else {
                    _clientStart = -180;
                    _clientEnd = 0;
                }

                ServerPackets.p17PlayerPunchData(entityId, _clientStart, _clientEnd, punchDeltaFinish, PacketReceiver.whoCanSee(this));
            }
        } /*else {
            We are currently not in punch mode anymore
        }
        */
    }

    public void parsePunchPacket(P16_PlayerPunch p) {
        serverArmRotation = p.punchAngle;
        queuedPunchStatus = p.punchStatus;
    }

    private void updateChunks() {
        chunkPacketList.clear();
        currentlyVisibleChunks = getChunkGrid().getChunksInPlayerRange(this);

        for(ServerChunk chunk : currentlyVisibleChunks) {
            if(chunk == null) continue; // If null, it is generating/loading from disk right now; otherwise it is fully loaded.
            if(!chunk.ready) continue;

            boolean isNewChunk = !hasSeenChunks.containsKey(chunk);
            boolean resend = false;

            if(!isNewChunk) {
                long cached = hasSeenChunks.get(chunk);
                resend = cached < chunk.lastTileUpdate;
            }

            if(resend || isNewChunk) {
                hasSeenChunks.put(chunk, chunk.lastTileUpdate);
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

    public void throwEntity(float dstX, float dstY) {
        ServerInventoryItem item = getCurrentItem();
        if(item.isEmpty()) return;
        ItemMapping m = ItemMapper.get().getMapping(item.itemId);
        if(m.logic.throwData == null) return; // to combat de-sync server<->client, double check current item
        ThrowData td = m.logic.throwData;

        Vector2 origin = new Vector2(posX, posY + 7);
        Vector2 dst = new Vector2(dstX, dstY);

        float clDst = Vector2.dst(origin.x, origin.y, dst.x, dst.y);

        if(clDst > td.maxThrowDistance) {
            dst = new Vector2(dst).sub(origin).nor().scl(td.maxThrowDistance).add(origin);
        }

        float totalThrowDuration = td.minThrowDuration + (td.maxThrowDuration - td.minThrowDuration) * Vector2.dst(origin.x, origin.y, dst.x, dst.y) / td.maxThrowDistance;

        ServerThrownEntity ste = new ServerThrownEntity();
        ste.thrownItemId = item.itemId;
        ste.thrownSpeed = 1f / totalThrowDuration;
        ste.originPos = origin;
        ste.dstPos = dst;
        ste.ignoreThrowerId = entityId;
        ste.explosionRadius = td.explosionRadius;
        ste.explosionDamage = td.explosionDamage;
        ste.knockbackStrength = td.knockbackStrength;
        ste.knockbackDuration = td.knockbackDuration;
        ServerWorld.get().registerServerEntity(entityDimension, ste);

        ServerPackets.p38PlayerAnimation(entityId, PLAYER_ANIMATION_ID_PLACE, PacketReceiver.whoCanSee(this));
        ServerPackets.p24PositionalSound("punch", this);

        useItemAmount(item);
    }

    public void placeAt(int chunkX, int chunkY, int tileArray, float mouseWorldX, float mouseWorldY) {
        ServerInventoryItem item = getCurrentItem();
        if(item.isEmpty()) return;
        ItemMapping m = ItemMapper.get().getMapping(item.itemId);
        if(m.logic.placeData == null) return; // to combat de-sync server<->client, double check current item

        var chunk = getChunkGrid().getChunkSafe(chunkX, chunkY);
        var tile = chunk.tiles[tileArray];
        PlaceData p = m.logic.placeData;

        HashSet<String> affectedChunks = new HashSet<>();
        affectedChunks.add(chunk.getChunkKey());

        if(p.type == PlaceType.FLOOR_0) {
            // Update tile timestamp
            chunk.lastTileUpdate = System.currentTimeMillis();

            { // Update tile data
                if(p.floorType == FloorType.DIRT && TileLayerType.isWater(tile.dynamicTileParts[2].emulatingType)) {
                    boolean deepPriority = tile.dynamicTileParts[1].emulatingType == TileLayerType.SOIL_DEEP_WATERLOGGED;

                    if(deepPriority) {
                        tile.updateLayer1(TileLayerType.SOIL_WATERLOGGED);
                        ServerPackets.p32ChunkDataSingle(tile, 1);

                        for(ServerTile st : tile.getNeighbouringTiles()) {
                            if(st.updateLayer1Adjacent(false)) {
                                ServerPackets.p32ChunkDataSingle(st, 1);
                                affectedChunks.add(st.chunk.getChunkKey());
                            }
                        }
                    } else {
                        tile.updateLayer2(TileLayerType.EMPTY);
                        tile.updateLayer1(TileLayerType.EMPTY);
                        tile.updateLayer0(TileLayerType.SOIL);
                        ServerPackets.p32ChunkDataSingle(tile, 0);
                        ServerPackets.p32ChunkDataSingle(tile, 1);
                        ServerPackets.p32ChunkDataSingle(tile, 2);

                        for(ServerTile st : tile.getNeighbouringTiles()) {
                            if(st.updateLayer0Adjacent(false)) {
                                ServerPackets.p32ChunkDataSingle(st, 0);
                                affectedChunks.add(st.chunk.getChunkKey());
                            }
                            if(st.updateLayer1Adjacent(false)) {
                                ServerPackets.p32ChunkDataSingle(st, 1);
                                affectedChunks.add(st.chunk.getChunkKey());
                            }
                            if(st.updateLayer2Adjacent(false)) {
                                ServerPackets.p32ChunkDataSingle(st, 2);
                                affectedChunks.add(st.chunk.getChunkKey());
                            }
                        }
                    }
                } else {
                    tile.updateLayer0(p.floorType.TILE_LAYER_TYPE);
                    ServerPackets.p32ChunkDataSingle(tile, 0);

                    for(ServerTile st : tile.getNeighbouringTiles()) {
                        if(st.updateLayer0Adjacent(p.floorType.TILE_LAYER_TYPE.TILE_IS_WALL)) {
                            ServerPackets.p32ChunkDataSingle(st, 0);
                            affectedChunks.add(st.chunk.getChunkKey());
                        }
                    }
                }
            }

            ServerPackets.p46EntityConstruct(item.itemId, tile.tileX, tile.tileY, mouseWorldX, mouseWorldY, PacketReceiver.whoCanSee(tile));
            ServerPackets.p38PlayerAnimation(entityId, PLAYER_ANIMATION_ID_PLACE, PacketReceiver.whoCanSee(this));

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
                        affectedChunks.add(st.chunk.getChunkKey());
                    }
                }
            }

            ServerPackets.p46EntityConstruct(item.itemId, tile.tileX, tile.tileY, mouseWorldX, mouseWorldY, PacketReceiver.whoCanSee(tile));
            ServerPackets.p38PlayerAnimation(entityId, PLAYER_ANIMATION_ID_PLACE, PacketReceiver.whoCanSee(this));

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
                        affectedChunks.add(st.chunk.getChunkKey());
                    }
                }
            }

            ServerPackets.p46EntityConstruct(item.itemId, tile.tileX, tile.tileY, mouseWorldX, mouseWorldY, PacketReceiver.whoCanSee(tile));
            ServerPackets.p38PlayerAnimation(entityId, PLAYER_ANIMATION_ID_PLACE, PacketReceiver.whoCanSee(this));

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

                if(proceed) {
                    // Proceed.
                    ServerEntity createdTileEntity = ServerEntityType.typeToEntity(p.entityType);
                    int x = tileArray % ROW_TILES;
                    int y = tileArray / ROW_TILES;
                    createdTileEntity.posX = ExpoShared.tileToPos(tile.tileX) + p.placeAlignmentOffsetX;
                    createdTileEntity.posY = ExpoShared.tileToPos(tile.tileY) + p.placeAlignmentOffsetY;

                    if(p.staticFlag) {
                        createdTileEntity.setStaticEntity();
                    } else {
                        createdTileEntity.chunkX = ExpoShared.posToChunk(createdTileEntity.posX);
                        createdTileEntity.chunkY = ExpoShared.posToChunk(createdTileEntity.posY);
                    }

                    ServerWorld.get().registerServerEntity(entityDimension, createdTileEntity);
                    createdTileEntity.attachToTile(chunk, x, y);

                    ServerPackets.p46EntityConstruct(item.itemId, tile.tileX, tile.tileY, mouseWorldX, mouseWorldY, PacketReceiver.whoCanSee(tile));
                    ServerPackets.p38PlayerAnimation(entityId, PLAYER_ANIMATION_ID_PLACE, PacketReceiver.whoCanSee(this));

                    useItemAmount(item);
                    playPlaceSound(p.sound, mouseWorldX, mouseWorldY);
                }
            } else {
                // Proceed.
                ServerEntity placedEntity = ServerEntityType.typeToEntity(p.entityType);
                placedEntity.posX = mouseWorldX + p.placeAlignmentOffsetX;
                placedEntity.posY = mouseWorldY + p.placeAlignmentOffsetY;

                if(p.staticFlag) {
                    placedEntity.setStaticEntity();
                } else {
                    placedEntity.chunkX = ExpoShared.posToChunk(placedEntity.posX);
                    placedEntity.chunkY = ExpoShared.posToChunk(placedEntity.posY);
                }

                ServerWorld.get().registerServerEntity(entityDimension, placedEntity);

                ServerPackets.p46EntityConstruct(item.itemId, tile.tileX, tile.tileY, mouseWorldX + p.placeAlignmentOffsetX, mouseWorldY + p.placeAlignmentOffsetY, PacketReceiver.whoCanSee(tile));
                ServerPackets.p38PlayerAnimation(entityId, PLAYER_ANIMATION_ID_PLACE, PacketReceiver.whoCanSee(this));

                useItemAmount(item);
                playPlaceSound(p.sound, mouseWorldX, mouseWorldY);
            }
        }

        long now = System.currentTimeMillis();

        for(String affectedChunkKey : affectedChunks) {
            // Update tile timestamp
            ServerChunk sv = getChunkGrid().getActiveChunk(affectedChunkKey);

            for(ServerPlayer player : getDimension().getEntityManager().getAllPlayers()) {
                if(player.canSeeChunk(affectedChunkKey)) {
                    player.hasSeenChunks.put(sv, now);
                }
            }

            sv.lastTileUpdate = now;
        }
    }

    public void digAt(int chunkX, int chunkY, int tileArray) {
        ServerInventoryItem item = getCurrentItem();
        ToolType useTool = item.isTool(ToolType.SHOVEL, ToolType.SCYTHE);
        if(useTool == null) return; // to combat de-sync server<->client, double check current item

        var chunk = getChunkGrid().getChunkSafe(chunkX, chunkY);
        var tile = chunk.tiles[tileArray];

        boolean digResult = tile.performDigOperation(ItemMapper.get().getMapping(item.itemId).logic.harvestDamage, item, true, false, 0, 0);

        if(digResult) {
            useItemDurability(item);
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
            ServerPackets.p24PositionalSound("log_split", posX, posY + 10, PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(this));
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