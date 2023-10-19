package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.config.ExpoServerConfiguration;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.fs.whitelist.ServerWhitelist;
import dev.michey.expo.server.fs.world.player.PlayerSaveFile;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipe;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipeMapping;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.InventoryChangeResult;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.gen.WorldGenSettings;
import dev.michey.expo.server.packet.*;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONArray;

import java.util.concurrent.CompletableFuture;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoServerPacketReader {

    /** Handle an incoming packet by the player directly (local server environment). */
    public void handlePacket(Packet packet) {
        if(packet instanceof P0_Auth_Req req) {
            // It's fine to load the player save on the main thread for now
            PlayerSaveFile psf = ExpoServerBase.get().getWorldSaveHandler().getPlayerSaveHandler().loadAndGetPlayerFile("LOCAL_PLAYER");

            // Create player entity
            ServerPlayer sp = ServerWorld.get().createPlayerEntity(null, req.username);
            registerPlayer(psf, sp);

            WorldGenSettings s = sp.getDimension().getChunkHandler().getGenSettings();

            ServerPackets.p1AuthResponse(true, "Local server", ExpoShared.DEFAULT_LOCAL_TICK_RATE, sp.getDimension().getChunkHandler().getTerrainNoiseHeight().getSeed(), s, PacketReceiver.local());
            ServerPackets.p3PlayerJoin(req.username, PacketReceiver.local());
            ServerPackets.p9PlayerCreate(sp, true, PacketReceiver.local());
            ServerPackets.p14WorldUpdate(sp.getDimension().dimensionTime, sp.getDimension().dimensionWeather.WEATHER_ID, sp.getDimension().dimensionWeatherStrength, PacketReceiver.local());
            ServerPackets.p19ContainerUpdate(sp, PacketReceiver.local());
            //sp.switchToSlot(0);
        } else if(packet instanceof P5_PlayerVelocity vel) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            sp.xDir = vel.xDir;
            sp.yDir = vel.yDir;
            sp.sprinting = vel.sprinting;
        } else if(packet instanceof P12_PlayerDirection dir) {
            // Ignored.
        } else if(packet instanceof P16_PlayerPunch punch) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            sp.parsePunchPacket(punch);
        } else if(packet instanceof P18_PlayerInventoryInteraction p) {
            doInventoryInteraction(ServerPlayer.getLocalPlayer(), p, PacketReceiver.local());
        } else if(packet instanceof P20_PlayerInventorySwitch p) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;
            sp.switchToSlot(p.slot);
        } else if(packet instanceof P25_ChatMessage p) {
            readChatMessage(true, p, null, ServerPlayer.getLocalPlayer());
        } else if(packet instanceof P27_PlayerEntitySelection p) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            sp.selectedEntity = p.entityId;
        } else if(packet instanceof P22_PlayerArmDirection p) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            sp.serverArmRotation = p.rotation;
        } else if(packet instanceof P31_PlayerDig p) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            sp.digAt(p.chunkX, p.chunkY, p.tileArray);
        } else if(packet instanceof P34_PlayerPlace p) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            sp.placeAt(p.chunkX, p.chunkY, p.tileArray, p.mouseWorldX, p.mouseWorldY);
        } else if(packet instanceof P35_PlayerCraft p) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            sp.playerInventory.craft(CraftingRecipeMapping.get().getRecipeMap().get(p.recipeIdentifier), p.all);
        } else if(packet instanceof P39_PlayerInteractEntity p) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            ServerEntity interactionEntity = sp.getDimension().getEntityManager().getEntityById(p.entityId);
            if(interactionEntity == null) return;

            interactionEntity.onInteraction(sp);
        } else if(packet instanceof P41_InventoryViewQuit p) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            if(sp.viewingInventory != null) {
                sp.viewingInventory.removeInventoryViewer(sp);
                sp.viewingInventory = null;
            }
        }
    }

    /** Handle an incoming packet by an external player source (dedicated server environment). */
    public void handlePacket(Connection connection, Object packetObject) {
        if(!(packetObject instanceof Packet o)) {
            log(connection.toString() + " sent an invalid object: " + packetObject.toString());
            return;
        }

        if(o instanceof P0_Auth_Req p) {
            log("User " + p.username + " attempting authorization");

            boolean authorized = true;
            String authorizationMessage = "OK";

            if(ServerWhitelist.get() != null) {
                if(!ServerWhitelist.get().isPlayerWhitelisted(p.username)) {
                    authorized = false;
                    authorizationMessage = "Not whitelisted on this server";
                }
            }

            if(authorized) {
                if(p.protocolVersion != ExpoShared.SERVER_PROTOCOL_VERSION) {
                    authorized = false;
                    authorizationMessage = "Wrong protocol version (" + p.protocolVersion + " -> " + ExpoShared.SERVER_PROTOCOL_VERSION + ")";
                }
            }

            if(authorized) {
                for(PlayerConnection con : PlayerConnectionHandler.get().connections()) {
                    if(con.player.username.equals(p.username)) {
                        authorized = false;
                        authorizationMessage = "Username already online";
                        break;
                    }
                }
            }

            WorldGenSettings s = ServerWorld.get().getMainDimension().getChunkHandler().getGenSettings();

            if(authorized) {
                ServerPackets.p1AuthResponse(true, authorizationMessage, ExpoServerConfiguration.get().getServerTps(), ExpoServerBase.get().getWorldSaveHandler().getWorldSeed(), s, PacketReceiver.connection(connection));
            } else {
                ServerPackets.p1AuthResponse(false, authorizationMessage, 0, 0, null, PacketReceiver.connection(connection));
            }


            if(authorized) {
                CompletableFuture.runAsync(() -> {
                    PlayerSaveFile psf = ExpoServerBase.get().getWorldSaveHandler().getPlayerSaveHandler().loadAndGetPlayerFile(p.username);

                    // Create player connection class
                    PlayerConnectionHandler handler = PlayerConnectionHandler.get();
                    PlayerConnection pc = handler.addPlayerConnection(connection);

                    // Create player entity
                    ServerPlayer sp = ServerWorld.get().createPlayerEntity(pc, p.username);
                    registerPlayer(psf, sp);

                    // Send every existing player as a packet to joined player
                    for(var con : handler.connections()) {
                        ServerPackets.p3PlayerJoin(con.player.username, PacketReceiver.connection(connection));

                        if(pc.getKryoConnection().getID() != con.getKryoConnection().getID()) {
                            ServerPackets.p3PlayerJoin(p.username, PacketReceiver.connection(con.getKryoConnection()));
                        }
                    }

                    // Player creation packet for joined player
                    ServerPackets.p9PlayerCreate(sp, true, PacketReceiver.connection(connection));
                    ServerPackets.p14WorldUpdate(sp.getDimension().dimensionTime, sp.getDimension().dimensionWeather.WEATHER_ID, sp.getDimension().dimensionWeatherStrength, PacketReceiver.connection(connection));
                    ServerPackets.p19ContainerUpdate(sp, PacketReceiver.player(sp));
                });
            }
        } else if(o instanceof P5_PlayerVelocity p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                player.xDir = p.xDir;
                player.yDir = p.yDir;
                player.sprinting = p.sprinting;
            }
        } else if(o instanceof P12_PlayerDirection p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                player.playerDirection = p.direction;
                ServerPackets.p12PlayerDirection(player.entityId, p.direction, PacketReceiver.whoCanSee(player));
            }
        } else if(o instanceof P16_PlayerPunch p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                player.parsePunchPacket(p);
            }
        } else if(o instanceof P18_PlayerInventoryInteraction p) {
            doInventoryInteraction(connectionToPlayer(connection), p, PacketReceiver.connection(connection));
        } else if(o instanceof P20_PlayerInventorySwitch p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                player.switchToSlot(p.slot);
            }
        } else if(o instanceof P22_PlayerArmDirection p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                player.applyArmDirection(p.rotation);
            }
        } else if(o instanceof P25_ChatMessage p) {
            readChatMessage(false, p, connection, connectionToPlayer(connection));
        } else if(o instanceof P27_PlayerEntitySelection p) {
            ServerPlayer player = connectionToPlayer(connection);
            player.selectedEntity = p.entityId;
        } else if(o instanceof P31_PlayerDig p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                player.digAt(p.chunkX, p.chunkY, p.tileArray);
            }
        } else if(o instanceof P34_PlayerPlace p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                player.placeAt(p.chunkX, p.chunkY, p.tileArray, p.mouseWorldX, p.mouseWorldY);
            }
        } else if(o instanceof P35_PlayerCraft p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                CraftingRecipe recipe = CraftingRecipeMapping.get().getRecipeMap().get(p.recipeIdentifier);
                player.playerInventory.craft(recipe, p.all);
            }
        } else if(o instanceof P39_PlayerInteractEntity p) {
            ServerPlayer player = connectionToPlayer(connection);
            if(player == null) return;

            ServerEntity interactionEntity = player.getDimension().getEntityManager().getEntityById(p.entityId);
            if(interactionEntity == null) return;

            interactionEntity.onInteraction(player);
        } else if(o instanceof P41_InventoryViewQuit p) {
            ServerPlayer player = connectionToPlayer(connection);
            if(player == null) return;

            if(player.viewingInventory != null) {
                player.viewingInventory.removeInventoryViewer(player);
                player.viewingInventory = null;
            }
        }
    }

    private void readChatMessage(boolean local, P25_ChatMessage p, Connection connection, ServerPlayer serverPlayer) {
        if(p.message.startsWith("/")) {
            ExpoServerBase.get().getCommandResolver().resolveCommand(p.message, serverPlayer, false);
        } else {
            if(!local) {
                log("[Chat] " + p.sender + ": " + p.message);
                ServerPackets.p25ChatMessage(p.sender, p.message, PacketReceiver.allExcept(connection));
            }
        }
    }

    private void doInventoryInteraction(ServerPlayer player, P18_PlayerInventoryInteraction p, PacketReceiver receiver) {
        if(p.containerId == ExpoShared.CONTAINER_ID_PLAYER || p.containerId == ExpoShared.CONTAINER_ID_VOID) {
            var change = player.playerInventory.performPlayerAction(p.actionType, p.slotId);
            convertInventoryChangeResultToPacket(change, receiver);
        } else {
            if(player.viewingInventory != null) {
                if(player.viewingInventory.getContainerId() == p.containerId) {
                    var change = player.viewingInventory.performPlayerAction(player, p.actionType, p.slotId);
                    convertInventoryChangeResultToPacket(change, receiver);
                }
            }
        }
    }

    public void convertInventoryChangeResultToPacket(InventoryChangeResult result, PacketReceiver receiver) {
        if(result != null && result.changePresent) {
            for(int containerId : result.changedSlots.keySet()) {
                var changedSlotsList = result.changedSlots.get(containerId);
                int[] changedSlots = changedSlotsList.stream().mapToInt(Integer::intValue).toArray();

                var changedItemsList = result.changedItems.get(containerId);
                ServerInventoryItem[] arr = new ServerInventoryItem[changedItemsList.size()];
                for(int i = 0; i < arr.length; i++) arr[i] = changedItemsList.get(i);

                ServerPackets.p19ContainerUpdate(containerId, changedSlots, arr, receiver);
            }
        }
    }

    private void registerPlayer(PlayerSaveFile psf, ServerPlayer sp) {
        int playerEntityId;

        sp.posX = psf.getFloat("posX");
        sp.posY = psf.getFloat("posY");

        sp.health = psf.getFloat("health");
        sp.hunger = psf.getFloat("hunger");
        sp.hungerCooldown = psf.getFloat("hungerCooldown");
        sp.nextHungerTickDown = psf.getFloat("nextHungerTickDown");
        sp.nextHungerDamageTick = psf.getFloat("nextHungerDamageTick");
        sp.nextHealthRegenTickDown = psf.getFloat("nextHealthRegenTickDown");

        if(psf.getHandler().fileJustCreated) {
            playerEntityId = ServerWorld.get().registerServerEntity(psf.getString("dimensionName"), sp);
        } else {
            playerEntityId = psf.getInt("entityId");
            ServerWorld.get().registerServerEntity(psf.getString("dimensionName"), sp, playerEntityId);

            InventoryFileLoader.loadFromStorage(sp.playerInventory, (JSONArray) psf.getHandler().get("inventory"));
        }

        log("Registered ServerPlayer for " + sp.username + " with entity id " + playerEntityId);
        sp.playerSaveFile = psf;
    }

    public ServerPlayer connectionToPlayer(Connection connection) {
        return PlayerConnectionHandler.get().getPlayerConnection(connection).player;
    }

}
