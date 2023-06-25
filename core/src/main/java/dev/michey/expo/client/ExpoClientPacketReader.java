package dev.michey.expo.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.Expo;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.client.chat.ChatMessage;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.particle.ClientParticleFood;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.packet.*;
import dev.michey.expo.util.*;

import java.util.Arrays;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.*;

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
                if(!local) {
                    ExpoClientContainer.get().getClientWorld().setNoiseSeed(p.worldSeed);
                    ExpoClientContainer.get().getClientWorld().getClientChunkGrid().applyGenSettings(p.noiseSettings, p.biomeDataMap, p.worldSeed);
                }
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
            ClientEntity entity = entityFromId(p.entityId);
            if(entity == null) return;

            entity.removalReason = p.reason;
            ClientEntityManager.get().removeEntity(p.entityId);
        } else if(o instanceof P6_EntityPosition p) {
            ClientEntity entity = entityFromId(p.entityId);
            if(entity != null) entity.applyPositionUpdate(p.xPos, p.yPos);
        } else if(o instanceof P7_ChunkSnapshot p) {
            //ClientChunkGrid.get().updateChunkViewport(p.activeChunks);
        } else if(o instanceof P8_EntityDeleteStack p) {
            for(int i = 0; i < p.entityList.length; i++) {
                ClientEntity entity = entityFromId(p.entityList[i]);

                if(entity == null) {
                    log("Cannot delete " + p.entityList[i]);
                    continue;
                }

                if(p.timestamp < entity.entityTimestamp) {
                    ExpoLogger.log("Possible clash? Timestamp " + entity.entityId + ": " + entity.entityTimestamp + "/" + p.timestamp);
                }

                entity.removalReason = p.reasons[i];
                ClientEntityManager.get().removeEntity(p.entityList[i]);
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
            ClientChunkGrid.get().updateChunkData(p.chunkX, p.chunkY, p.biomes, p.individualTileData);
        } else if(o instanceof P12_PlayerDirection p) {
            ClientEntity entity = entityFromId(p.entityId);

            if(entity != null) {
                ClientPlayer player = (ClientPlayer) entity;
                player.playerDirection = p.direction;
            }
        } else if(o instanceof P13_EntityMove p) {
            ClientEntity entity = entityFromId(p.entityId);
            if(entity != null) {
                entity.applyPositionUpdate(p.xPos, p.yPos, p.xDir, p.yDir, p.sprinting);
            }
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
        } else if(o instanceof P25_ChatMessage p) {
            ExpoClientContainer.get().getPlayerUI().chat.addChatMessage(new ChatMessage(p.message, p.sender, false));
        } else if(o instanceof P26_EntityDamage p) {
            ClientEntity entity = entityFromId(p.entityId);

            if(entity != null) {
                entity.onDamage(p.damage, p.newHealth);
            }
        } else if(o instanceof P28_PlayerFoodParticle p) {
            ClientEntity entity = entityFromId(p.entityId);

            if(entity != null) {
                ClientPlayer player = (ClientPlayer) entity;
                int particles = MathUtils.random(3, 6);
                TextureRegion baseItemFoodTexture = ItemMapper.get().getMapping(player.holdingItemId).heldRender.textureRegion;

                for(int i = 0; i < particles; i++) {
                    ClientParticleFood cpf = new ClientParticleFood();

                    float velocityX = (-7.0f + MathUtils.random(30f)) * (player.direction() == 1 ? 1 : -1);
                    float velocityY = -MathUtils.random(30f, 36f);

                    cpf.depth = player.depth - 0.0001f;
                    cpf.particleTexture = baseItemFoodTexture;
                    cpf.setParticleOriginAndVelocity(player.toMouthX(), player.toMouthY(), velocityX, velocityY);
                    cpf.setParticleLifetime(0.35f);
                    cpf.setParticleFadeout(0.1f);
                    float scale = MathUtils.random(0.5f, 0.9f);
                    cpf.setParticleScale(scale, scale);
                    cpf.setParticleColor(Color.WHITE);
                    cpf.setParticleFadein(0.1f);
                    cpf.setParticleRotation(MathUtils.random(360f));
                    cpf.setParticleConstantRotation(360f);

                    ClientEntityManager.get().addClientSideEntity(cpf);
                }

                AudioEngine.get().playSoundGroupManaged("eat", new Vector2(player.toMouthX(), player.toMouthY()), PLAYER_AUDIO_RANGE, false);
            }
        } else if(o instanceof P29_EntityCreateAdvanced p) {
            ClientEntity entity = ClientEntityManager.get().createFromPacketAdvanced(p);
            ClientEntityManager.get().addEntity(entity);
        } else if(o instanceof P30_EntityDataUpdate p) {
            ClientEntity entity = entityFromId(p.entityId);

            if(entity != null) {
                entity.readEntityDataUpdate(p.payload);
            }
        } else if(o instanceof P32_ChunkDataSingle p) {
            var grid = ClientChunkGrid.get(); if(grid == null) return;
            var chunk = grid.getChunk(p.chunkX, p.chunkY); if(chunk == null) return;

            chunk.updateSingle(p.layer, p.tileArray, p.tile);
        } else if(o instanceof P33_TileDig p) {
            float x = ExpoShared.tileToPos(p.tileX);
            float y = ExpoShared.tileToPos(p.tileY);

            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(8, 16)
                    .scale(0.5f, 0.9f)
                    .lifetime(0.35f, 0.5f)
                    .color(ParticleColorMap.random(p.particleColorId))
                    .position(x + 2, y + 2)
                    .offset(12, 12)
                    .velocity(-24, 24, 8, 32)
                    .fadein(0.10f)
                    .fadeout(0.10f)
                    .textureRange(0, 7)
                    .randomRotation()
                    .rotateWithVelocity()
                    .spawn();
        } else if(o instanceof P36_PlayerReceiveItem p) {
            for(int i = 0; i < p.itemIds.length; i++) {
                int id = p.itemIds[i];
                int amount = p.itemAmounts[i];
                ExpoClientContainer.get().getPlayerUI().addPickupLine(id, amount);
            }
        } else if(o instanceof P37_EntityTeleport p) {
            ClientEntity entity = entityFromId(p.entityId);

            if(entity != null) {
                entity.applyTeleportUpdate(p.x, p.y);
            }
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
