package dev.michey.expo.server.util;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.ServerInventory;
import dev.michey.expo.server.main.logic.inventory.ServerPlayerInventory;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.main.logic.world.gen.WorldGenSettings;
import dev.michey.expo.server.packet.*;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;

import java.util.concurrent.RejectedExecutionException;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class ServerPackets {

    /** Sends the P1_Auth_Rsp packet via TCP protocol. */
    public static void p1AuthResponse(boolean authSuccessful, String authMessage, int serverTps, int worldSeed, WorldGenSettings genSettings, PacketReceiver receiver) {
        mt(() -> {
            P1_Auth_Rsp p = new P1_Auth_Rsp();
            p.authSuccessful = authSuccessful;
            p.authMessage = authMessage;
            p.serverTps = serverTps;
            p.worldSeed = worldSeed;
            if(genSettings != null) {
                p.noiseSettings = genSettings.getNoiseSettings();
                p.biomeDefinitionList = genSettings.getBiomeDefinitionList();
            }
            tcp(p, receiver);
        });
    }

    /** Sends the P44_Connect_Rsp packet via TCP protocol. */
    public static void p44ConnectResponse(boolean credentialsSuccessful, boolean requiresSteamTicket, String message, PacketReceiver receiver) {
        mt(() -> {
            P44_Connect_Rsp p = new P44_Connect_Rsp();
            p.credentialsSuccessful = credentialsSuccessful;
            p.requiresSteamTicket = requiresSteamTicket;
            p.message = message;
            tcp(p, receiver);
        });
    }

    /** Sends the P2_EntityCreate packet via TCP protocol. */
    public static void p2EntityCreate(ServerEntityType entityType, int entityId, float serverPosX, float serverPosY, int tileArray, float health, PacketReceiver receiver) {
        P2_EntityCreate p = new P2_EntityCreate();
        p.entityType = entityType;
        p.entityId = entityId;
        p.serverPosX = serverPosX;
        p.serverPosY = serverPosY;
        p.tileArray = tileArray;
        p.entityHealth = health;
        tcp(p, receiver);
    }

    public static void p2EntityCreate(ServerEntity entity, PacketReceiver receiver) {
        mt(() -> p2EntityCreate(entity.getEntityType(), entity.entityId, entity.posX, entity.posY, entity.tileEntity ? (entity.tileY * ROW_TILES + entity.tileX) : -1, entity.health, receiver));
    }

    /** Sends the P3_PlayerJoin packet via TCP protocol. */
    public static void p3PlayerJoin(String username, PacketReceiver receiver) {
        mt(() -> {
            P3_PlayerJoin p = new P3_PlayerJoin();
            p.username = username;
            tcp(p, receiver);
        });
    }

    /** Sends the P4_EntityDelete packet via TCP protocol. */
    public static void p4EntityDelete(int entityId, EntityRemovalReason reason, PacketReceiver receiver) {
        mt(() -> {
            P4_EntityDelete p = new P4_EntityDelete();
            p.entityId = entityId;
            p.reason = reason;
            tcp(p, receiver);
        });
    }

    /** Sends the P6_EntityPosition packet via UDP protocol. */
    public static void p6EntityPosition(int entityId, float xPos, float yPos, PacketReceiver receiver) {
        mt(() -> {
            P6_EntityPosition p = new P6_EntityPosition();
            p.entityId = entityId;
            p.xPos = xPos;
            p.yPos = yPos;
            udp(p, receiver);
        });
    }

    /** Sends the P8_EntityDeleteStack packet via TCP protocol. */
    public static void p8EntityDeleteStack(int[] entityList, EntityRemovalReason[] reasons, PacketReceiver receiver) {
        mt(() -> {
            P8_EntityDeleteStack p = new P8_EntityDeleteStack();
            p.entityList = entityList;
            p.reasons = reasons;
            tcp(p, receiver);
        });
    }

    /** Sends the P9_PlayerCreate packet via TCP protocol. */
    public static void p9PlayerCreate(int entityId, String dimensionName, float serverPosX, float serverPosY, int direction, String username, boolean player, int[] equippedItemIds, float armRotation, float health, float hunger, PacketReceiver receiver) {
        mt(() -> {
            P9_PlayerCreate p = new P9_PlayerCreate();
            p.entityType = ServerEntityType.PLAYER;
            p.entityId = entityId;
            p.dimensionName = dimensionName;
            p.serverPosX = serverPosX;
            p.serverPosY = serverPosY;
            p.username = username;
            p.player = player;
            p.direction = direction;
            p.equippedItemIds = equippedItemIds;
            p.armRotation = armRotation;
            p.health = health;
            p.hunger = hunger;
            tcp(p, receiver);
        });
    }

    public static void p9PlayerCreate(ServerPlayer entity, boolean player, PacketReceiver receiver) {
        p9PlayerCreate(entity.entityId, entity.entityDimension, entity.posX, entity.posY, entity.playerDirection, entity.username, player, entity.getEquippedItemIds(), entity.serverArmRotation, entity.health, entity.hunger, receiver);
    }

    /** Sends the P10_PlayerQuit packet via TCP protocol. */
    public static void p10PlayerQuit(String username, PacketReceiver receiver) {
        mt(() -> {
            P10_PlayerQuit p = new P10_PlayerQuit();
            p.username = username;
            tcp(p, receiver);
        });
    }

    /** Sends the P11_ChunkData packet via TCP protocol. */
    public static void p11ChunkData(ServerChunk chunk, PacketReceiver receiver) {
        mt(() -> {
            P11_ChunkData p = new P11_ChunkData();
            p.chunkX = chunk.chunkX;
            p.chunkY = chunk.chunkY;

            int s = chunk.tiles.length;
            p.biomes = new BiomeType[s];
            p.individualTileData = new DynamicTilePart[s][];

            for(int i = 0; i < s; i++) {
                p.biomes[i] = chunk.tiles[i].biome;
                p.individualTileData[i] = chunk.tiles[i].dynamicTileParts;
            }

            synchronized (chunk.tileEntityLock) {
                if(chunk.hasTileBasedEntities()) {
                    int count = 0;

                    for(int id : chunk.getTileBasedEntityIdGrid()) {
                        if(id != -1) count++;
                    }

                    p.tileEntityCount = count;
                }
            }

            tcp(p, receiver);
        });
    }

    /** Sends the P12_PlayerDirection packet via UDP protocol. */
    public static void p12PlayerDirection(int entityId, int direction, PacketReceiver receiver) {
        mt(() -> {
            P12_PlayerDirection p = new P12_PlayerDirection();
            p.entityId = entityId;
            p.direction = direction;
            udp(p, receiver);
        });
    }

    /** Sends the P13_EntityMove packet via UDP protocol. */
    public static void p13EntityMove(int entityId, int xDir, int yDir, boolean sprinting, float xPos, float yPos, float distance, PacketReceiver receiver) {
        mt(() -> {
            P13_EntityMove p = new P13_EntityMove();
            p.entityId = entityId;
            p.xDir = xDir;
            p.yDir = yDir;
            p.xPos = xPos;
            p.yPos = yPos;
            p.distance = distance;
            p.sprinting = sprinting;
            udp(p, receiver);
        });
    }

    public static void p13EntityMove(int entityId, int xDir, int yDir, float xPos, float yPos, float distance, PacketReceiver receiver) {
        p13EntityMove(entityId, xDir, yDir, false, xPos, yPos, distance, receiver);
    }

    /** Sends the P14_WorldUpdate packet via TCP protocol. */
    public static void p14WorldUpdate(String dimensionName, float worldTime, int worldWeather, float weatherStrength, PacketReceiver receiver) {
        mt(() -> {
            P14_WorldUpdate p = new P14_WorldUpdate();
            p.dimensionName = dimensionName;
            p.worldTime = worldTime;
            p.worldWeather = worldWeather;
            p.weatherStrength = weatherStrength;
            tcp(p, receiver);
        });
    }

    /** Sends the P15_PingList packet via TCP protocol. */
    public static void p15PingList(PacketReceiver receiver) {
        mt(() -> {
            var map = PlayerConnectionHandler.get().connections();
            int size = map.size();
            P15_PingList p = new P15_PingList();
            p.username = new String[size];
            p.ping = new int[size];

            int index = 0;

            for(PlayerConnection con : map) {
                p.username[index] = con.player.username;
                p.ping[index] = con.getKryoConnection().getReturnTripTime();
                index++;
            }

            udp(p, receiver);
        });
    }

    /** Sends the P17_PlayerPunchData packet via UDP protocol. */
    public static void p17PlayerPunchData(int entityId, float punchAngleStart, float punchAngleEnd, float punchDuration, PacketReceiver receiver) {
        mt(() -> {
            P17_PlayerPunchData p = new P17_PlayerPunchData();
            p.entityId = entityId;
            p.punchAngleStart = punchAngleStart;
            p.punchAngleEnd = punchAngleEnd;
            p.punchDuration = punchDuration;
            udp(p, receiver);
        });
    }

    /** Sends the P19_ContainerUpdate packet via TCP protocol. */
    public static void p19ContainerUpdate(int containerId, int[] updatedSlots, ServerInventoryItem[] updatedItems, PacketReceiver receiver) {
        mt(() -> {
            P19_ContainerUpdate p = new P19_ContainerUpdate();
            p.containerId = containerId;
            p.updatedSlots = updatedSlots;
            p.updatedItems = updatedItems;
            tcp(p, receiver);
        });
    }

    /** Sends the P19_PlayerInventoryUpdate packet via TCP protocol. */
    public static void p19ContainerUpdate(ServerPlayer player, PacketReceiver receiver) {
        mt(() -> {
            ServerPlayerInventory inv = player.playerInventory;
            int[] updatedSlots = new int[inv.slots.length];
            for(int i = 0; i < updatedSlots.length; i++) updatedSlots[i] = i;
            ServerInventoryItem[] updated = new ServerInventoryItem[inv.slots.length];
            for(int i = 0; i < updated.length; i++) updated[i] = inv.slots[i].item;

            P19_ContainerUpdate p = new P19_ContainerUpdate();
            p.containerId = ExpoShared.CONTAINER_ID_PLAYER;
            p.updatedSlots = updatedSlots;
            p.updatedItems = updated;
            tcp(p, receiver);
        });
    }

    /** Sends the P21_PlayerGearUpdate packet via UDP protocol. */
    public static void p21PlayerGearUpdate(int entityId, int[] heldItemIds, PacketReceiver receiver) {
        mt(() -> {
            P21_PlayerGearUpdate p = new P21_PlayerGearUpdate();
            p.entityId = entityId;
            p.heldItemIds = heldItemIds;
            udp(p, receiver);
        });
    }

    /** Sends the P22_PlayerArmDirection packet via UDP protocol. */
    public static void p22PlayerArmDirection(int entityId, float rotation, PacketReceiver receiver) {
        mt(() -> {
            P22_PlayerArmDirection p = new P22_PlayerArmDirection();
            p.entityId = entityId;
            p.rotation = rotation;
            udp(p, receiver);
        });
    }

    /** Sends the P23_PlayerLifeUpdate packet via UDP protocol. */
    public static void p23PlayerLifeUpdate(float health, float hunger, PacketReceiver receiver) {
        mt(() -> {
            P23_PlayerLifeUpdate p = new P23_PlayerLifeUpdate();
            p.health = health;
            p.hunger = hunger;
            udp(p, receiver);
        });
    }

    /** Sends the P24_PositionalSound packet via UDP protocol. */
    public static void p24PositionalSound(String soundName, float worldX, float worldY, float maxSoundRange, PacketReceiver receiver) {
        if(soundName == null) return; // Easier to put this here.
        mt(() -> {
            P24_PositionalSound p = new P24_PositionalSound();
            p.soundName = soundName;
            p.worldX = worldX;
            p.worldY = worldY;
            p.maxSoundRange = maxSoundRange;
            udp(p, receiver);
        });
    }

    public static void p24PositionalSound(String soundName, ServerEntity entity) {
        mt(() -> {
            P24_PositionalSound p = new P24_PositionalSound();
            p.soundName = soundName;
            p.worldX = entity.posX;
            p.worldY = entity.posY;
            p.maxSoundRange = ExpoShared.PLAYER_AUDIO_RANGE;
            udp(p, PacketReceiver.whoCanSee(entity));
        });
    }

    public static void p24PositionalSound(String soundName, ServerTile tile) {
        mt(() -> {
            P24_PositionalSound p = new P24_PositionalSound();
            p.soundName = soundName;
            p.worldX = ExpoShared.tileToPos(tile.tileX) + 8;
            p.worldY = ExpoShared.tileToPos(tile.tileY) + 8;
            p.maxSoundRange = ExpoShared.PLAYER_AUDIO_RANGE;
            udp(p, PacketReceiver.whoCanSee(tile));
        });
    }

    /** Sends the P25_ChatMessage packet via TCP protocol. */
    public static void p25ChatMessage(String sender, String message, PacketReceiver receiver) {
        mt(() -> {
            P25_ChatMessage p = new P25_ChatMessage();
            p.sender = sender;
            p.message = message;
            tcp(p, receiver);
        });
    }

    /** Sends the P26_EntityDamage packet via UDP protocol. */
    public static void p26EntityDamage(int entityId, float damage, float newHealth, int damageSourceEntityId, PacketReceiver receiver) {
        mt(() -> {
            P26_EntityDamage p = new P26_EntityDamage();
            p.entityId = entityId;
            p.damage = damage;
            p.newHealth = newHealth;
            p.damageSourceEntityId = damageSourceEntityId;
            udp(p, receiver);
        });
    }

    /** Sends the P28_PlayerFoodParticle packet via UDP protocol. */
    public static void p28PlayerFoodParticle(int entityId, int itemId, PacketReceiver receiver) {
        mt(() -> {
            P28_PlayerFoodParticle p = new P28_PlayerFoodParticle();
            p.entityId = entityId;
            p.itemId = itemId;
            udp(p, receiver);
        });
    }

    /** Sends the P29_EntityCreateAdvanced packet via TCP protocol. */
    private static void p29EntityCreateAdvanced(ServerEntityType entityType, int entityId, float serverPosX, float serverPosY, int tileArray, float health, Object[] payload, PacketReceiver receiver) {
        P29_EntityCreateAdvanced p = new P29_EntityCreateAdvanced();
        p.entityType = entityType;
        p.entityId = entityId;
        p.serverPosX = serverPosX;
        p.serverPosY = serverPosY;
        p.payload = payload;
        p.tileArray = tileArray;
        p.entityHealth = health;
        tcp(p, receiver);
    }

    public static void p29EntityCreateAdvanced(ServerEntity entity, PacketReceiver receiver) {
        mt(() -> {
            int tileEntityId = entity.tileEntity ? (entity.tileY * ROW_TILES + entity.tileX) : -1;
            p29EntityCreateAdvanced(entity.getEntityType(), entity.entityId, entity.posX, entity.posY, tileEntityId, entity.health, entity.getPacketPayload(), receiver);
        });
    }

    /** Sends the P30_EntityDataUpdate packet via UDP protocol. */
    public static void p30EntityDataUpdate(int entityId, Object[] payload, PacketReceiver receiver) {
        mt(() -> {
            P30_EntityDataUpdate p = new P30_EntityDataUpdate();
            p.entityId = entityId;
            p.payload = payload;
            udp(p, receiver);
        });
    }

    /** Sends the P30_EntityDataUpdate packet via UDP protocol. */
    public static void p30EntityDataUpdate(ServerEntity entity) {
        mt(() -> {
            P30_EntityDataUpdate p = new P30_EntityDataUpdate();
            p.entityId = entity.entityId;
            p.payload = entity.getPacketPayload();
            udp(p, PacketReceiver.whoCanSee(entity));
        });
    }

    /** Sends the P32_ChunkDataSingle packet via UDP protocol. */
    public static void p32ChunkDataSingle(int chunkX, int chunkY, int layer, int tileArray, DynamicTilePart dynamicTile, PacketReceiver receiver) {
        mt(() -> {
            P32_ChunkDataSingle p = new P32_ChunkDataSingle();
            p.chunkX = chunkX;
            p.chunkY = chunkY;
            p.layer = layer;
            p.tileArray = tileArray;
            p.tile = dynamicTile;
            udp(p, receiver);
        });
    }

    public static void p32ChunkDataSingle(ServerTile tile, int layer) {
        p32ChunkDataSingle(tile.chunk.chunkX, tile.chunk.chunkY, layer, tile.tileArray, tile.dynamicTileParts[layer], PacketReceiver.whoCanSee(tile));
    }

    /** Sends the P33_TileDig packet via UDP protocol. */
    public static void p33TileDig(int tileX, int tileY, int particleColorId, PacketReceiver receiver) {
        mt(() -> {
            P33_TileDig p = new P33_TileDig();
            p.tileX = tileX;
            p.tileY = tileY;
            p.particleColorId = particleColorId;
            udp(p, receiver);
        });
    }

    /** Sends the P36_PlayerReceiveItem packet via UDP protocol. */
    public static void p36PlayerReceiveItem(int[] ids, int[] amounts, PacketReceiver receiver) {
        mt(() -> {
            P36_PlayerReceiveItem p = new P36_PlayerReceiveItem();
            p.itemIds = ids;
            p.itemAmounts = amounts;
            udp(p, receiver);
        });
    }

    /** Sends the P37_EntityTeleport packet via UDP protocol. */
    public static void p37EntityTeleport(int entityId, float x, float y, TeleportReason teleportReason, PacketReceiver receiver) {
        mt(() -> {
            P37_EntityTeleport p = new P37_EntityTeleport();
            p.entityId = entityId;
            p.x = x;
            p.y = y;
            p.teleportReason = teleportReason;
            udp(p, receiver);
        });
    }

    /** Sends the P38_PlayerAnimation packet via UDP protocol. */
    public static void p38PlayerAnimation(int entityId, int animationId, PacketReceiver receiver) {
        mt(() -> {
            P38_PlayerAnimation p = new P38_PlayerAnimation();
            p.entityId = entityId;
            p.animationId = animationId;
            udp(p, receiver);
        });
    }

    /** Sends the P40_InventoryView packet via TCP protocol. */
    public static void p40InventoryView(ServerInventory inventory, PacketReceiver receiver) {
        mt(() -> {
            P40_InventoryView p = new P40_InventoryView();
            p.type = inventory.getType();
            p.containerId = inventory.getContainerId();
            p.viewSlots = inventory.slots;
            tcp(p, receiver);
        });
    }

    /** Sends the P41_InventoryViewQuit packet via TCP protocol. */
    public static void p41InventoryViewQuit(PacketReceiver receiver) {
        mt(() -> {
            P41_InventoryViewQuit p = new P41_InventoryViewQuit();
            tcp(p, receiver);
        });
    }

    /** Sends the P42_EntityAnimation packet via UDP protocol. */
    public static void p42EntityAnimation(int entityId, int animationId, PacketReceiver receiver) {
        mt(() -> {
            P42_EntityAnimation p = new P42_EntityAnimation();
            p.entityId = entityId;
            p.animationId = animationId;
            udp(p, receiver);
        });
    }

    /** Sends the P43_EntityDeleteAdvanced packet via TCP protocol. */
    public static void p43EntityDeleteAdvanced(int entityId, EntityRemovalReason reason, float damage, float newHealth, int damageSourceEntityId, PacketReceiver receiver) {
        mt(() -> {
            P43_EntityDeleteAdvanced p = new P43_EntityDeleteAdvanced();
            p.entityId = entityId;
            p.reason = reason;
            p.damage = damage;
            p.newHealth = newHealth;
            p.damageSourceEntityId = damageSourceEntityId;
            tcp(p, receiver);
        });
    }

    /** Sends the P46_EntityConstruct packet via UDP protocol. */
    public static void p46EntityConstruct(int itemId, int tileX, int tileY, float worldX, float worldY, PacketReceiver receiver) {
        mt(() -> {
            P46_EntityConstruct p = new P46_EntityConstruct();
            p.itemId = itemId;
            p.tileX = tileX;
            p.tileY = tileY;
            p.worldX = worldX;
            p.worldY = worldY;
            udp(p, receiver);
        });
    }

    /** Sends the P47_ItemConsume packet via UDP protocol. */
    public static void p47ItemConsume(int entityId, int itemId, float duration, PacketReceiver receiver) {
        mt(() -> {
            P47_ItemConsume p = new P47_ItemConsume();
            p.entityId = entityId;
            p.itemId = itemId;
            p.duration = duration;
            udp(p, receiver);
        });
    }

    /** Sends the P50_TileFullUpdate packet via UDP protocol. */
    public static void p50TileFullUpdate(int chunkX, int chunkY, int tileArray, DynamicTilePart[] dynamicTileParts, PacketReceiver receiver) {
        mt(() -> {
            P50_TileFullUpdate p = new P50_TileFullUpdate();
            p.chunkX = chunkX;
            p.chunkY = chunkY;
            p.tileArray = tileArray;
            p.dynamicTileParts = dynamicTileParts;
            udp(p, receiver);
        });
    }

    public static void p50TileFullUpdate(ServerTile tile) {
        p50TileFullUpdate(tile.chunk.chunkX, tile.chunk.chunkY, tile.tileArray, tile.dynamicTileParts, PacketReceiver.whoCanSee(tile));
    }

    /** Sends the P51_PositionalSoundAdvanced packet via UDP protocol. */
    public static void p51PositionalSoundAdvanced(String soundName, float worldX, float worldY, float maxSoundRange, float volumeMultiplier, PacketReceiver receiver) {
        mt(() -> {
            P51_PositionalSoundAdvanced p = new P51_PositionalSoundAdvanced();
            p.soundName = soundName;
            p.worldX = worldX;
            p.worldY = worldY;
            p.maxSoundRange = maxSoundRange;
            p.volumeMultiplier = volumeMultiplier;
            udp(p, receiver);
        });
    }

    /** Sends the P52_TranslatableChatMessage packet via TCP protocol. */
    public static void p52TranslatableChatMessage(String sender, String langKey, Object[] payload, PacketReceiver receiver) {
        mt(() -> {
            P52_TranslatableChatMessage p = new P52_TranslatableChatMessage();
            p.sender = sender;
            p.langKey = langKey;
            p.payload = payload;
            tcp(p, receiver);
        });
    }

    /** Helper methods below. */
    private static void udp(Packet p, PacketReceiver receiver) {
        if(receiver == null) return;

        if(receiver.all) {
            ExpoServerBase.get().broadcastPacketUDP(p);
            return;
        }

        if(receiver.receiverAllExcept != null) {
            ExpoServerBase.get().broadcastPacketUDPExcept(p, receiver.receiverAllExcept);
            return;
        }

        if(receiver.receiverList != null) {
            for(Connection con : receiver.receiverList) {
                con.sendUDP(p);
            }

            return;
        }

        if(receiver.receiverSingle == null) {
            ExpoServerBase.get().broadcastPacketUDP(p);
        } else {
            receiver.receiverSingle.sendUDP(p);
        }
    }

    private static void tcp(Packet p, PacketReceiver receiver) {
        if(receiver == null) return;

        if(receiver.all) {
            ExpoServerBase.get().broadcastPacketTCP(p);
            return;
        }

        if(receiver.receiverAllExcept != null) {
            ExpoServerBase.get().broadcastPacketTCPExcept(p, receiver.receiverAllExcept);
            return;
        }

        if(receiver.receiverList != null) {
            for(Connection con : receiver.receiverList) {
                con.sendTCP(p);
            }

            return;
        }

        if(receiver.receiverSingle == null) {
            ExpoServerBase.get().broadcastPacketTCP(p);
        } else {
            receiver.receiverSingle.sendTCP(p);
        }
    }

    private static void mt(Runnable run) {
        try {
            ServerWorld.get().mtVirtualService.execute(run);
        } catch (RejectedExecutionException ignored) { }
    }

}
