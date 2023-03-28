package dev.michey.expo.server.main.logic.entity;

import com.badlogic.gdx.utils.Null;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.fs.world.player.PlayerSaveFile;
import dev.michey.expo.server.main.logic.entity.arch.BoundingBox;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.ServerPlayerInventory;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.world.chunk.EntityVisibilityController;
import dev.michey.expo.server.packet.P11_ChunkData;
import dev.michey.expo.server.packet.P16_PlayerPunch;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.PLAYER_DEFAULT_ATTACK_SPEED;

public class ServerPlayer extends ServerEntity {

    public PlayerConnection playerConnection;
    public boolean localServerPlayer;
    public PlayerSaveFile playerSaveFile;
    public String username;

    public float playerSpeed = 1.5f;
    public int xDir = 0;
    public int yDir = 0;
    private boolean dirResetPacket = false;

    public int playerDirection = 1; // default in client

    public boolean punching;
    public float startAngle;
    public float endAngle;
    public float punchDelta;
    public float punchDeltaFinish;
    private boolean punchDamageApplied;

    public float serverArmRotation;

    public int selectedInventorySlot = 0;

    public float hunger = 100f;                 // actual hunger value, cannot exceed 100
    public float hungerCooldown = 180.0f;       // -> saturation that each hunger item gives
    public float nextHungerTickDown = 4.0f;     // seconds between each hunger down tick
    public float nextHungerDamageTick = 4.0f;   // seconds between each damage via hunger tick
    public float nextHealthRegenTickDown = 1.0f;// seconds between each health regen tick

    public int selectedEntity = -1;

    public HashSet<String> hasSeenChunks = new HashSet<>();

    public EntityVisibilityController entityVisibilityController = new EntityVisibilityController(this);

    public ServerPlayerInventory playerInventory = new ServerPlayerInventory(this);

    /** Physics body */
    private BoundingBox physicsBody;

    @Override
    public void onCreation() {
        // add physics body of player to world
        physicsBody = new BoundingBox(this, 2f, 0, 6, 6);
    }

    @Override
    public void onDeletion() {
        // remove physics body of player from world
        physicsBody.dispose();
    }

    @Override
    public void onDamage(ServerEntity damageSource, float damage) {

    }

    @Override
    public void onDie() {

    }

    @Override
    public void tick(float delta) {
        if(xDir != 0 || yDir != 0) {
            float waterFactor = isInWater() ? 0.5f : 1.0f;
            boolean normalize = xDir != 0 && yDir != 0;
            float normalizer = 1.0f;

            if(normalize) {
                float len = (float) Math.sqrt(xDir * xDir + yDir * yDir);
                normalizer = 1 / len;
            }

            float toMoveX = xDir * playerSpeed * waterFactor * normalizer;
            float toMoveY = yDir * playerSpeed * waterFactor * normalizer;

            var result = physicsBody.move(toMoveX, toMoveY);

            posX = result.goalX - physicsBody.xOffset;
            posY = result.goalY - physicsBody.yOffset;

            ServerPackets.p13EntityMove(entityId, xDir, yDir, posX, posY, PacketReceiver.whoCanSee(this));
            dirResetPacket = true;
        }

        if(xDir == 0 && yDir == 0 && dirResetPacket) {
            dirResetPacket = false;
            ServerPackets.p13EntityMove(entityId, xDir, yDir, posX, posY, PacketReceiver.whoCanSee(this));
        }

        if(punching) {
            punchDelta += delta;

            if(punchDelta >= (punchDeltaFinish * 0.6f) && !punchDamageApplied) {
                punchDamageApplied = true;

                ServerEntity selected = getDimension().getEntityManager().getEntityById(selectedEntity);

                if(selected != null) {
                    int item = getCurrentItem();
                    float dmg = ExpoShared.PLAYER_DEFAULT_ATTACK_DAMAGE;

                    if(item != -1) {
                        dmg = ItemMapper.get().getMapping(item).logic.attackDamage;
                    }

                    if(selected.damageableWith != null) {
                        if(item != -1) {
                            if(selected.damageableWith == ItemMapper.get().getMapping(item).logic.toolType) {
                                selected.applyDamageWithPacket(this, dmg);
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
            }

            // in range entities damage
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
                if(health > 100f) health = 100f;
                ServerPackets.p23PlayerLifeUpdate(health, hunger, PacketReceiver.player(this));
            }
        }
    }

    public int getCurrentItem() {
        return playerInventory.slots[selectedInventorySlot].item.itemId;
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
        if(getCurrentItem() != -1) {
            serverArmRotation = rotation;
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
        if(health < 0) health = 0;
    }

    public void removeHunger(float remove) {
        hunger -= remove;
        if(hunger < 0) hunger = 0;
    }

    public void parsePunchPacket(P16_PlayerPunch p) {
        if(!punching) {
            float angleSpan = 180;

            punching = true;
            float attackSpeed = PLAYER_DEFAULT_ATTACK_SPEED;

            if(getCurrentItem() != -1) {
                attackSpeed = ItemMapper.get().getMapping(getCurrentItem()).logic.attackSpeed;
            }

            punchDeltaFinish = attackSpeed;
            startAngle = p.punchAngle - angleSpan / 2;
            endAngle = p.punchAngle + angleSpan / 2;
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
    }

    @Override
    public SavableEntity onSave() {
        return null;
    }

    @Override
    public void onChunkChanged() {
        //log("PLAYER " + username + " changed chunk to " + chunkX + "," + chunkY);
        int[] chunks = getChunkGrid().getChunkNumbersInPlayerRange(this);
        List<Pair<String, P11_ChunkData>> chunkPacketList = null;

        for(int i = 0; i < chunks.length; i += 2) {
            int x = chunks[i    ];
            int y = chunks[i + 1];
            String key = x + "," + y;

            //log("Seen chunk " + key + " -> " + hasSeenChunks.contains(key));

            if(!hasSeenChunks.contains(key)) {
                if(chunkPacketList == null) chunkPacketList = new LinkedList<>();
                hasSeenChunks.add(key);

                var chunk = getChunkGrid().getChunk(x, y);
                var packet = new P11_ChunkData();
                packet.chunkX = x;
                packet.chunkY = y;
                packet.biomes = chunk.biomes;
                packet.layer0 = chunk.layer0;
                packet.layer1 = chunk.layer1;
                packet.layer2 = chunk.layer2;
                chunkPacketList.add(new Pair<>(key, packet));
            }
        }

        if(chunkPacketList != null) {
            for(var pair : chunkPacketList) {
                ServerPackets.p11ChunkData(pair.value.chunkX, pair.value.chunkY, pair.value.biomes, pair.value.layer0, pair.value.layer1, pair.value.layer2, PacketReceiver.player(this));
            }
        }
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
