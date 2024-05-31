package dev.michey.expo.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.client.chat.ChatMessage;
import dev.michey.expo.console.GameConsole;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.misc.ClientPickupLine;
import dev.michey.expo.logic.entity.particle.ClientParticleFood;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.server.main.logic.inventory.item.FloorType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.packet.*;
import dev.michey.expo.util.*;
import dev.michey.expo.weather.Weather;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ClientStatic.STEAM_INITIALIZED;
import static dev.michey.expo.util.ExpoShared.*;

public class ExpoClientPacketReader {

    public void handlePacket(Packet o, boolean local) {
        switch (o) {
            case P44_Connect_Rsp p -> {
                if (p.credentialsSuccessful) {
                    ExpoClientContainer.get().setLoadingMessage("Connected to server, attempting authentication...");
                    GameConsole.get().addSystemSuccessMessage("Passed initial server connection check: " + p.message);

                    if (p.requiresSteamTicket && STEAM_INITIALIZED) {
                        ClientStatic.STEAM_USER.getAuthTicketForWebApi(null);
                    } else {
                        ClientPackets.p45AuthReq(ClientStatic.PLAYER_USERNAME, null);
                    }
                } else {
                    ExpoClientContainer.get().setLoadingMessage("Connected to server, error: " + p.message);
                    GameConsole.get().addSystemErrorMessage("Failed initial server connection check: " + p.message);
                    Expo.get().switchToExistingScreen(ClientStatic.SCREEN_MENU);
                    Expo.get().disposeAndRemoveInactiveScreen(ClientStatic.SCREEN_GAME);
                }
            }
            case P1_Auth_Rsp p -> {
                if (p.authSuccessful) {
                    ExpoClientContainer.get().setLoadingMessage("Retrieving world data...");
                    GameConsole.get().addSystemSuccessMessage("Successfully joined server: " + p.authMessage + " (" + p.serverTps + " TPS)");
                    if (!local) {
                        ExpoClientContainer.get().getClientWorld().setNoiseSeed(p.worldSeed);
                        ExpoClientContainer.get().getClientWorld().getClientChunkGrid().applyGenSettings(p.noiseSettings, p.biomeDefinitionList, p.worldSeed);

                        float pickTps = Math.max(p.serverTps * 0.5f, 60);
                        PLAYER_ARM_MOVEMENT_SEND_RATE = 1f / pickTps;
                    } else {
                        ExpoShared.PLAYER_ARM_MOVEMENT_SEND_RATE = 1f / (float) p.serverTps;
                    }
                    ExpoClientContainer.get().setServerTickRate(p.serverTps);
                } else {
                    ExpoClientContainer.get().setLoadingMessage("Failed authentication: " + p.authMessage);
                    GameConsole.get().addSystemErrorMessage("Failed to auth with server: " + p.authMessage);
                    Expo.get().switchToExistingScreen(ClientStatic.SCREEN_MENU);
                    Expo.get().disposeAndRemoveInactiveScreen(ClientStatic.SCREEN_GAME);
                }
            }
            case P3_PlayerJoin p -> ExpoClientContainer.get().notifyPlayerJoin(p.username);
            case P2_EntityCreate p -> {
                // log("Creating Entity object " + p.entityId + " " + p.entityType);
                ClientEntity entity = ClientEntityManager.get().createFromPacket(p);
                ClientEntityManager.get().addEntity(entity);
            }
            case P4_EntityDelete p -> ClientEntityManager.get().removeEntity(p.entityId, p.reason);

            //ExpoLogger.log("REMOVING: " + entity.getEntityType().name() + "-> " + p.entityId);
            case P6_EntityPosition p -> {
                ClientEntity entity = entityFromId(p.entityId);
                if (entity != null) entity.applyPositionUpdate(p.xPos, p.yPos);
            }
            case P7_ChunkSnapshot p -> {
                //ClientChunkGrid.get().updateChunkViewport(p.activeChunks);
            }
            case P8_EntityDeleteStack p -> {
                for (int i = 0; i < p.entityList.length; i++) {
                    ClientEntityManager.get().removeEntity(p.entityList[i], p.reasons[i]);
                }
            }
            case P9_PlayerCreate p -> {
                //log("Creating Player object " + p.entityId + " " + p.entityType + " " + p.username + " " + p.player);

                ClientPlayer player = new ClientPlayer();
                player.tileEntityTileArray = -1;
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

                if (player.player) {
                    ClientPlayer.setLocalPlayer(player);
                }
            }
            case P10_PlayerQuit p -> ExpoClientContainer.get().notifyPlayerQuit(p.username);
            case P11_ChunkData p ->
                //ExpoLogger.log("p11 " + p.chunkX + " " + p.chunkY);
                    ExpoClientContainer.get().getClientWorld().getClientChunkGrid().handleChunkData(p);
            case P12_PlayerDirection p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    ClientPlayer player = (ClientPlayer) entity;
                    player.playerDirection = p.direction;
                }
            }
            case P13_EntityMove p -> {
                ClientEntity entity = entityFromId(p.entityId);
                if (entity != null) {
                    entity.applyPositionUpdate(p.xPos, p.yPos, p.xDir, p.yDir, p.sprinting, p.distance);
                }
            }
            case P14_WorldUpdate p -> {
                ClientWorld w = ExpoClientContainer.get().getClientWorld();
                log("Received World update (worldTime/worldWeather/worldStrength): " + p.worldTime + "/" + Weather.idToWeather(p.worldWeather).name() + "/" + p.weatherStrength + " (cl: " + w.worldTime + ")");

                w.dimensionName = p.dimensionName;
                w.worldTime = p.worldTime;
                w.worldWeather = p.worldWeather;
                w.weatherStrength = p.weatherStrength;
            }
            case P15_PingList p -> {
                var map = ExpoClientContainer.get().getPlayerOnlineList();

                for (int i = 0; i < p.username.length; i++) {
                    map.put(p.username[i], p.ping[i]);
                }
            }
            case P17_PlayerPunchData p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    ((ClientPlayer) entity).applyServerPunchData(p);
                }
            }
            case P19_ContainerUpdate p -> {
                if (p.containerId == CONTAINER_ID_PLAYER) {
                    PlayerInventory inv = PlayerInventory.LOCAL_INVENTORY;

                    if (inv == null) {
                        ClientPlayer.QUEUED_INVENTORY_PACKET = p;
                    } else {
                        PacketUtils.readInventoryUpdatePacket(p);
                    }
                } else {
                    PacketUtils.readInventoryUpdatePacket(p);
                }
            }
            case P21_PlayerGearUpdate p -> {
                ClientEntity entity = entityFromId(p.entityId);
                ClientPlayer player = ClientPlayer.getLocalPlayer();

                if (entity != null) {
                    ClientPlayer c = (ClientPlayer) entity;
                    applyHeldItemIds(c, p.heldItemIds);
                } else if (player != null && player.entityId == p.entityId) { // xD
                    applyHeldItemIds(player, p.heldItemIds);
                }
            }
            case P22_PlayerArmDirection p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    ((ClientPlayer) entity).applyServerArmData(p.rotation);
                }
            }
            case P23_PlayerLifeUpdate p -> {
                ClientPlayer player = ClientPlayer.getLocalPlayer();

                if (player != null) {
                    player.applyHealthHunger(p.health, p.hunger);
                }
            }
            case P24_PositionalSound p ->
                    AudioEngine.get().playSoundGroupManaged(p.soundName, new Vector2(p.worldX, p.worldY), p.maxSoundRange, false);
            case P25_ChatMessage p ->
                    ExpoClientContainer.get().getPlayerUI().chat.addChatMessage(new ChatMessage(p.message, p.sender, false));
            case P26_EntityDamage p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    entity.serverHealth = p.newHealth;
                    entity.onDamage(p.damage, p.newHealth, p.damageSourceEntityId);

                /*
                ClientPlayer player = ClientPlayer.getLocalPlayer();
                if(player != null && entity.entityId == player.entityId) {
                    CameraShake.invoke(2.0f, 0.25f);
                }
                */
                }
            }
            case P28_PlayerFoodParticle p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    ClientPlayer player = (ClientPlayer) entity;
                    int particles = MathUtils.random(3, 6);
                    TextureRegion baseItemFoodTexture = ItemMapper.get().getMapping(player.holdingItemId).heldRender[0].textureRegions[0];

                    for (int i = 0; i < particles; i++) {
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
            }
            case P29_EntityCreateAdvanced p -> {
                ClientEntity entity = ClientEntityManager.get().createFromPacketAdvanced(p);
                ClientEntityManager.get().addEntity(entity);
            }
            case P30_EntityDataUpdate p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    entity.applyEntityUpdatePayload(p.payload);
                }
            }
            case P32_ChunkDataSingle p -> {
                var grid = ClientChunkGrid.get();
                if (grid == null) return;
                var chunk = grid.getChunk(p.chunkX, p.chunkY);
                if (chunk == null) return;

                chunk.updateSingle(p);
                PlayerUI.get().playerMinimap.incomplete = true;
            }
            case P33_TileDig p -> {
                float x = ExpoShared.tileToPos(p.tileX);
                float y = ExpoShared.tileToPos(p.tileY);

                new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                        .amount(8, 16)
                        .scale(0.5f, 0.9f)
                        .lifetime(0.35f, 0.5f)
                        .color(ParticleColorMap.of(p.particleColorId))
                        .position(x + 2, y + 2)
                        .offset(12, 12)
                        .velocity(-24, 24, 8, 32)
                        .fadein(0.10f)
                        .fadeout(0.10f)
                        .textureRange(0, 7)
                        .randomRotation()
                        .rotateWithVelocity()
                        .spawn();
            }
            case P36_PlayerReceiveItem p -> {
                ClientPlayer cp = ClientPlayer.getLocalPlayer();
                if (cp == null) return;

                var all = ClientEntityManager.get().getEntitiesByType(ClientEntityType.PICKUP_LINE);
                var add = ClientEntityManager.get().getEntityClientAdditionQueue();

                for (int i = 0; i < p.itemIds.length; i++) {
                    int seekId = p.itemIds[i];
                    ClientPickupLine copyTo = null;

                    for (var existing : all) {
                        ClientPickupLine clientPickupLine = (ClientPickupLine) existing;

                        if (clientPickupLine.id == seekId) {
                            copyTo = clientPickupLine;
                            break;
                        }
                    }

                    for (var existing : add) {
                        if (existing.getEntityType() == ClientEntityType.PICKUP_LINE) {
                            ClientPickupLine clientPickupLine = (ClientPickupLine) existing;

                            if (clientPickupLine.id == seekId) {
                                copyTo = clientPickupLine;
                                break;
                            }
                        }
                    }

                    if (copyTo == null) {
                        ClientPickupLine cpl = new ClientPickupLine();
                        cpl.id = p.itemIds[i];
                        cpl.amount = p.itemAmounts[i];
                        cpl.setMapping();
                        cpl.reset();
                        ClientEntityManager.get().addClientSideEntity(cpl);
                    } else {
                        copyTo.amount += p.itemAmounts[i];
                        copyTo.reset();
                    }
                }
            }
            case P37_EntityTeleport p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    entity.applyTeleportUpdate(p.x, p.y, p.teleportReason);
                }
            }
            case P38_PlayerAnimation p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    ClientPlayer player = (ClientPlayer) entity;

                    if (p.animationId == PLAYER_ANIMATION_ID_PLACE) {
                        player.playPunchAnimation(player.playerDirection, 0.3f, false);
                    }
                }
            }
            case P40_InventoryView p -> {
                ClientPlayer player = ClientPlayer.getLocalPlayer();
                player.getUI().openContainerView(p.type, p.containerId, p.viewSlots);
                AudioEngine.get().playSoundGroup("inv_open");
            }
            case P41_InventoryViewQuit p41InventoryViewQuit -> {
                ClientPlayer player = ClientPlayer.getLocalPlayer();
                player.getUI().closeInventoryView();
            }
            case P42_EntityAnimation p -> {
                ClientEntity entity = entityFromId(p.entityId);
                if (entity != null) entity.playEntityAnimation(p.animationId);
            }
            case P43_EntityDeleteAdvanced p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if (entity != null) {
                    entity.serverHealth = p.newHealth;
                    entity.onDamage(p.damage, p.newHealth, p.damageSourceEntityId);
                }

                ClientEntityManager.get().removeEntity(p.entityId, p.reason);
            }
            case P46_EntityConstruct p -> {
                ItemMapping mapping = ItemMapper.get().getMapping(p.itemId);
                FloorType ft = mapping.logic.placeData.floorType;

                float twx = ExpoShared.tileToPos(p.tileX);
                float twy = ExpoShared.tileToPos(p.tileY);

                if(ft != null) {
                    // Placed thing is a floor
                    ParticleSheet.Common.spawnDustConstructFloorParticles(twx, twy);
                } else {
                    ParticleSheet.Common.spawnDustConstructEntityParticles(p.worldX, p.worldY, ExpoAssets.get().textureRegion(mapping.logic.placeData.previewTextureName));
                }

                String soundName = mapping.logic.placeData.sound != null ? mapping.logic.placeData.sound : "place";
                AudioEngine.get().playSoundGroupManaged(soundName, new Vector2(p.worldX, p.worldY), PLAYER_AUDIO_RANGE, false);
            }
            case P47_ItemConsume p -> {
                ClientEntity entity = entityFromId(p.entityId);

                if(entity != null) {
                    ClientPlayer player = (ClientPlayer) entity;
                    ItemMapping mapping = ItemMapper.get().getMapping(p.itemId);

                    if(mapping.logic.isFood() && ClientPlayer.getLocalPlayer() == player) {
                        ClientPickupLine cpl = new ClientPickupLine();
                        cpl.id = p.itemId;
                        cpl.amount = 1;
                        cpl.setMapping();
                        cpl.reset();

                        if(mapping.logic.foodData.hungerRestore > 0) {
                            cpl.setCustomDisplayText("+" + ExpoShared.toDisplayNumber(mapping.logic.foodData.hungerRestore) + " Hunger");
                        } else {
                            cpl.setCustomDisplayText("+" + ExpoShared.toDisplayNumber(mapping.logic.foodData.healthRestore) + " Health");
                        }

                        cpl.setCustomDisplayColor(PlayerUI.get().COLOR_GREEN);
                        ClientEntityManager.get().addClientSideEntity(cpl);
                    }
                }
            }
            case null, default -> {}
        }
    }

    private void applyHeldItemIds(ClientPlayer player, int[] ids) {
        if(ids[0] != -1) {
            player.updateHoldingItemSprite(ids[0]);
            player.resetSelector = true;
        }
        player.holdingItemId = ids[0];

        player.holdingArmorHeadId = ids[1];
        player.holdingArmorChestId = ids[2];
        player.holdingArmorGlovesId = ids[3];
        player.holdingArmorLegsId = ids[4];
        player.holdingArmorFeetId = ids[5];

        if(player.holdingArmorHeadId != -1) {
            player.holdingHeadRender = ItemMapper.get().getMapping(player.holdingArmorHeadId).armorRender.copy();
            Expo.get().updateArmor(player.holdingHeadRender);

            if(player.playerDirection == 0) {
                player.holdingHeadRender.flip();
            }
        }

        /*
        if(player.holdingArmorHeadId != -1) {
            if(player.playerDirection == 0) player.holdingHeadRender

            player.holdingHeadRender = ItemMapper.get().getMapping(player.holdingArmorHeadId).armorRender;
            if(player.playerDirection == 0) for(ItemRender ir : player.holdingHeadRender) ir.flip();
        }

        if(player.holdingArmorChestId != -1) {
            player.holdingChestRender = ItemMapper.get().getMapping(player.holdingArmorChestId).armorRender;
            if(player.playerDirection == 0) for(ItemRender ir : player.holdingChestRender) ir.flip();
        }

        if(player.holdingArmorGlovesId != -1) {
            player.holdingGlovesRender = ItemMapper.get().getMapping(player.holdingArmorGlovesId).armorRender;
            if(player.playerDirection == 0) for(ItemRender ir : player.holdingGlovesRender) ir.flip();
        }

        if(player.holdingArmorLegsId != -1) {
            player.holdingLegsRender = ItemMapper.get().getMapping(player.holdingArmorLegsId).armorRender;
            if(player.playerDirection == 0) for(ItemRender ir : player.holdingLegsRender) ir.flip();
        }

        if(player.holdingArmorFeetId != -1) {
            player.holdingFeetRender = ItemMapper.get().getMapping(player.holdingArmorFeetId).armorRender;
            if(player.playerDirection == 0) for(ItemRender ir : player.holdingFeetRender) ir.flip();
        }
        */
    }

    private ClientEntity entityFromId(int entityId) {
        return ClientEntityManager.get().getEntityById(entityId);
    }

}
