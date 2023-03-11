package dev.michey.expo.client;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryonet.Client;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.ClientPlayer;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.packet.*;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.PacketUtils;

import java.util.Arrays;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoClientPacketReader {

    /** Handle an instant incoming packet by a local source (local server). */
    public void handlePacketLocal(Packet packet) {
        handlePacket(packet, true);
    }

    /** Handle an incoming packet by an external source (dedicated server). */
    public void handlePacketDedicated(Object o) {
        if(!(o instanceof Packet)) {
            log("The dedicated server sent an invalid object: " + o.toString());
            return;
        }

        handlePacket((Packet) o, false);
    }

    private void handlePacket(Packet o, boolean local) {
        if(o instanceof P1_Auth_Rsp p) {
            log("Received authorization packet: " + p.authorized + " (" + p.message + ") server tps: " + p.serverTps);

            if(!p.authorized) {
                Expo.get().switchToExistingScreen(ClientStatic.SCREEN_MENU);
                Expo.get().disposeAndRemoveInactiveScreen(ClientStatic.SCREEN_GAME);
            } else {
                if(!local) ExpoClientContainer.get().getClientWorld().setNoiseSeed(p.worldSeed);
                ExpoClientContainer.get().setServerTickRate(p.serverTps);
            }
        } else if(o instanceof P3_PlayerJoin p) {
            ExpoClientContainer.get().notifyPlayerJoin(p.username);
            log("Join " + p.username);
        } else if(o instanceof P2_EntityCreate p) {
            // log("Creating Entity object " + p.entityId + " " + p.entityType);
            ClientEntity entity = ClientEntityManager.get().createFromPacket(p);
            ClientEntityManager.get().addEntity(entity);
        } else if(o instanceof P4_EntityDelete p) {
            // log("Removing Entity object " + p.entityId);
            ClientEntityManager.get().removeEntity(p.entityId);
        } else if(o instanceof P6_EntityPosition p) {
            ClientEntity entity = entityFromId(p.entityId);
            if(entity != null) entity.applyPositionUpdate(p.xPos, p.yPos);
        } else if(o instanceof P7_ChunkSnapshot p) {
            //ClientChunkGrid.get().updateChunkViewport(p.activeChunks);
        } else if(o instanceof P8_EntityDeleteStack p) {
            for(int entityId : p.entityList) {
                ClientEntityManager.get().removeEntity(entityId);
            }
        } else if(o instanceof P9_PlayerCreate p) {
            log("Creating Player object " + p.entityId + " " + p.entityType + " " + p.username + " " + p.player);
            log("Held item ids: " + Arrays.toString(p.equippedItemIds));

            ClientPlayer player = new ClientPlayer();
            player.entityId = p.entityId;
            player.username = p.username;
            player.serverPosX = p.serverPosX;
            player.serverPosY = p.serverPosY;
            player.clientPosX = player.serverPosX;
            player.clientPosY = player.serverPosY;
            player.player = p.player;
            player.playerDirection = p.direction;
            applyHeldItemIds(player, p.equippedItemIds);
            player.serverPunchAngle = p.armRotation;
            player.lerpedServerPunchAngle = p.armRotation;
            player.lastLerpedServerPunchAngle = p.armRotation;
            player.playerHealth = p.health;
            player.playerHunger = p.hunger;

            ClientEntityManager.get().addEntity(player);

            if(player.player) {
                ClientPlayer.setLocalPlayer(player);
            }
        } else if(o instanceof P10_PlayerQuit p) {
            ExpoClientContainer.get().notifyPlayerQuit(p.username);
        } else if(o instanceof P11_ChunkData p) {
            ClientChunkGrid.get().updateChunkData(p.chunkX, p.chunkY, p.biomeData, p.tileIndexData, p.waterLoggedData);
        } else if(o instanceof P12_PlayerDirection p) {
            ClientEntity entity = entityFromId(p.entityId);

            if(entity != null) {
                ClientPlayer player = (ClientPlayer) entity;
                player.playerDirection = p.direction;
            }
        } else if(o instanceof P13_EntityMove p) {
            ClientEntity entity = entityFromId(p.entityId);
            if(entity != null) entity.applyPositionUpdate(p.xPos, p.yPos, p.xDir, p.yDir);
        } else if(o instanceof P14_WorldUpdate p) {
            log("Received WORLD UPDATE " + p.worldTime + " " + p.worldWeather + " " + p.weatherStrength);

            ClientWorld w = ExpoClientContainer.get().getClientWorld();

            w.worldTime = p.worldTime;
            w.worldWeather = p.worldWeather;
            w.weatherStrength = p.weatherStrength;

        } else if(o instanceof P15_PingList p) {
            var map = ExpoClientContainer.get().getPlayerOnlineList();

            for(int i = 0; i < p.username.length; i++) {
                map.put(p.username[i], p.ping[i]);
            }
        } else if(o instanceof P17_PlayerPunchData p) {
            ClientEntity entity = entityFromId(p.entityId);

            if(entity != null) {
                ((ClientPlayer) entity).applyServerPunchData(p);
            }
        } else if(o instanceof P19_PlayerInventoryUpdate p) {
            PlayerInventory inv = PlayerInventory.LOCAL_INVENTORY;
            //log("updatedSlots.length " + p.updatedSlots.length);
            //log("updatedSlots -> " + Arrays.toString(p.updatedSlots));

            if(inv == null) {
                //log("Inventory is null, queueing...");
                ClientPlayer.QUEUED_INVENTORY_PACKET = p;
            } else {
                PacketUtils.readInventoryUpdatePacket(p, inv);
            }
        } else if(o instanceof P21_PlayerGearUpdate p) {
            ClientEntity entity = entityFromId(p.entityId);
            ClientPlayer player = ClientPlayer.getLocalPlayer();

            if(entity != null) {
                ClientPlayer c = (ClientPlayer) entity;
                applyHeldItemIds(c, p.heldItemIds);
            } else if(player != null && player.entityId == p.entityId) { // xD
                applyHeldItemIds(player, p.heldItemIds);
            }
        } else if(o instanceof P22_PlayerArmDirection p) {
            ClientEntity entity = entityFromId(p.entityId);

            if(entity != null) {
                ((ClientPlayer) entity).applyServerArmData(p.rotation);
            }
        } else if(o instanceof P23_PlayerLifeUpdate p) {
            ClientPlayer player = ClientPlayer.getLocalPlayer();

            if(player != null) {
                player.playerHealth = p.health;
                player.playerHunger = p.hunger;
            }
        } else if(o instanceof P24_PositionalSound p) {
            AudioEngine.get().playSoundGroupManaged(p.soundName, new Vector2(p.worldX, p.worldY), p.maxSoundRange, false);
        }
    }

    private void applyHeldItemIds(ClientPlayer player, int[] ids) {
        player.holdingItemId = ids[0];

        player.holdingArmorHeadId = ids[1];
        player.holdingArmorChestId = ids[2];
        player.holdingArmorGlovesId = ids[3];
        player.holdingArmorLegsId = ids[4];
        player.holdingArmorFeetId = ids[5];

        if(player.holdingItemId != -1) {
            player.updateHoldingItemSprite();
        }

        if(player.holdingArmorHeadId != -1) {
            player.holdingArmorHeadTexture = new TextureRegion(ItemMapper.get().getMapping(player.holdingArmorHeadId).armorRender.textureRegion);
            if(player.playerDirection == 0) player.holdingArmorHeadTexture.flip(true, false);
        }

        if(player.holdingArmorChestId != -1) {
            player.holdingArmorChestTexture = new TextureRegion(ItemMapper.get().getMapping(player.holdingArmorChestId).armorRender.textureRegion);
            if(player.playerDirection == 0) player.holdingArmorChestTexture.flip(true, false);
        }

        if(player.holdingArmorGlovesId != -1) {
            player.holdingArmorGlovesTexture = new TextureRegion(ItemMapper.get().getMapping(player.holdingArmorGlovesId).armorRender.textureRegion);
            if(player.playerDirection == 0) player.holdingArmorGlovesTexture.flip(true, false);
        }

        if(player.holdingArmorLegsId != -1) {
            player.holdingArmorLegsTexture = new TextureRegion(ItemMapper.get().getMapping(player.holdingArmorLegsId).armorRender.textureRegion);
            if(player.playerDirection == 0) player.holdingArmorLegsTexture.flip(true, false);
        }

        if(player.holdingArmorFeetId != -1) {
            player.holdingArmorFeetTexture = new TextureRegion(ItemMapper.get().getMapping(player.holdingArmorFeetId).armorRender.textureRegion);
            if(player.playerDirection == 0) player.holdingArmorFeetTexture.flip(true, false);
        }
    }

    private ClientEntity entityFromId(int entityId) {
        return ClientEntityManager.get().getEntityById(entityId);
    }

}
