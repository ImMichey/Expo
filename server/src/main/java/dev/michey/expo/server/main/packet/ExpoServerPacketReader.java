package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.config.ExpoServerConfiguration;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.fs.whitelist.ServerWhitelist;
import dev.michey.expo.server.fs.world.player.PlayerSaveFile;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.arch.ExpoServerDedicated;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoServerPacketReader {

    /** Handle an incoming packet by the player directly (local server environment). */
    public void handlePacket(Packet packet) {
        switch (packet) {
            case P0_Connect_Req p0ConnectReq ->
                    ServerPackets.p44ConnectResponse(true, false, "Local server", PacketReceiver.local());
            case P45_Auth_Req req -> {
                // It's fine to load the player save on the main thread for now
                PlayerSaveFile psf = ExpoServerBase.get().getWorldSaveHandler().getPlayerSaveHandler().loadAndGetPlayerFile("LOCAL_PLAYER", -1);

                // Create player entity
                ServerPlayer sp = ServerWorld.get().createPlayerEntity(null, req.username);
                registerPlayer(psf, sp);

                WorldGenSettings s = sp.getDimension().getChunkHandler().getGenSettings();

                ServerPackets.p1AuthResponse(true, "OK", ExpoShared.DEFAULT_LOCAL_TICK_RATE, sp.getDimension().getChunkHandler().getTerrainNoiseHeight().getSeed(), s, PacketReceiver.local());
                ServerPackets.p3PlayerJoin(req.username, PacketReceiver.local());
                ServerPackets.p9PlayerCreate(sp, true, PacketReceiver.local());
                ServerPackets.p14WorldUpdate(sp.getDimension().getDimensionName(), sp.getDimension().dimensionTime, sp.getDimension().dimensionWeather.WEATHER_ID, sp.getDimension().dimensionWeatherStrength, PacketReceiver.local());
                ServerPackets.p19ContainerUpdate(sp, PacketReceiver.local());
            }
            case P5_PlayerVelocity vel -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                sp.xDir = vel.xDir;
                sp.yDir = vel.yDir;
                sp.sprinting = vel.sprinting;
            }
            case P12_PlayerDirection dir -> {
                // Ignored.
            }
            case P16_PlayerPunch punch -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                sp.parsePunchPacket(punch);
            }
            case P18_PlayerInventoryInteraction p ->
                    doInventoryInteraction(ServerPlayer.getLocalPlayer(), p, PacketReceiver.local());
            case P20_PlayerInventorySwitch p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;
                sp.switchToSlot(p.slot);
            }
            case P25_ChatMessage p -> readChatMessage(true, p, null, ServerPlayer.getLocalPlayer());
            case P27_PlayerEntitySelection p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                sp.selectedEntity = p.entityId;
            }
            case P22_PlayerArmDirection p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                sp.serverArmRotation = p.rotation;
            }
            case P31_PlayerDig p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                sp.digAt(p.chunkX, p.chunkY, p.tileArray);
            }
            case P34_PlayerPlace p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                sp.placeAt(p.chunkX, p.chunkY, p.tileArray, p.mouseWorldX, p.mouseWorldY);
            }
            case P35_PlayerCraft p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                sp.playerInventory.craft(CraftingRecipeMapping.get().getRecipeMap().get(p.recipeIdentifier), p.all);
            }
            case P39_PlayerInteractEntity p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                ServerEntity interactionEntity = sp.getDimension().getEntityManager().getEntityById(p.entityId);
                if (interactionEntity == null) return;

                interactionEntity.onInteraction(sp);
            }
            case P41_InventoryViewQuit p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                if (sp.viewingInventory != null) {
                    sp.viewingInventory.removeInventoryViewer(sp);
                    sp.viewingInventory = null;
                }
            }
            case P48_ClientPlayerPosition p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                sp.consumePosition(p);
            }
            case P49_PlayerEntityThrow p -> {
                ServerPlayer sp = ServerPlayer.getLocalPlayer();
                if (sp == null) return;

                sp.throwEntity(p.dstX, p.dstY);
            }
            case null, default -> {}
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars);
    }

    /** Handle an incoming packet by an external player source (dedicated server environment). */
    public void handlePacket(Connection connection, Object packetObject) {
        if(!(packetObject instanceof Packet o)) {
            log(connection.toString() + " sent an invalid object: " + packetObject.toString());
            return;
        }

        switch (o) {
            case P0_Connect_Req p -> {
                ExpoLogger.log("New connection request [" + connection.toString() + "/" + p.protocolVersion + "/" + p.password + "]");
                boolean authorized = true;
                String message = "OK";

                String requiredPassword = ExpoServerConfiguration.get().getPassword();

                // ##################################################### Slot auth
                int maxPlayers = ExpoServerConfiguration.get().getMaxPlayers();

                if (maxPlayers > 0) {
                    if (PlayerConnectionHandler.get().connectionList.size() >= maxPlayers) {
                        authorized = false;
                        message = "Server is full";
                    }
                }
                // ##################################################### Slot auth

                // ##################################################### Password auth
                if (authorized) {
                    if (!requiredPassword.isEmpty()) {
                        if (!requiredPassword.equals(p.password)) {
                            authorized = false;
                            message = "Invalid password";
                        }
                    }
                }
                // ##################################################### Password auth

                // ##################################################### Protocol auth
                if (authorized) {
                    if (p.protocolVersion != ExpoShared.SERVER_PROTOCOL_VERSION) {
                        authorized = false;
                        message = "Wrong protocol version (Client/Server: " + p.protocolVersion + "/" + ExpoShared.SERVER_PROTOCOL_VERSION + ")";
                    }
                }
                // ##################################################### Protocol auth

                boolean requiresSteamTicket = ExpoServerConfiguration.get().isAuthPlayersEnabled();
                if (requiresSteamTicket) {
                    message += ", steam auth ticket required";
                }
                ServerPackets.p44ConnectResponse(authorized, requiresSteamTicket, message, PacketReceiver.connection(connection));
            }
            case P45_Auth_Req p -> {
                String steamTicketString = null;
                boolean authorized = true;
                String authorizationMessage = "OK";
                long steamId = -1;
                boolean steamCheck = ExpoServerConfiguration.get().isAuthPlayersEnabled();

                if (p.steamTicket != null) {
                    steamTicketString = bytesToHex(p.steamTicket);
                }

                ExpoLogger.log("New Auth request [" + connection.toString() + "/" + p.username + "/" + steamTicketString + "]");

                // ##################################################### Steam auth
                if (ExpoServerConfiguration.get().isAuthPlayersEnabled()) {
                    if (steamTicketString == null) {
                        authorized = false;
                        authorizationMessage = "Steam auth failed (no auth ticket provided)";
                    } else {
                        JSONObject response = ((ExpoServerDedicated) ExpoServerDedicated.get()).getSteamHandler().authenticateUserTicket(steamTicketString);

                        if (response != null) {
                            try {
                                ExpoLogger.log("Steam auth response: " + response);
                                JSONObject data = response.getJSONObject("response").getJSONObject("params");

                                if (data.getString("result").equals("OK")) {
                                    steamId = Long.parseLong(data.getString("steamid"));
                                } else {
                                    authorized = false;
                                    authorizationMessage = "Steam auth failed (" + data.getString("result") + ")";
                                }
                            } catch (JSONException e) {
                                authorized = false;
                                authorizationMessage = "Steam auth failed (json invalid)";
                            }
                        } else {
                            authorized = false;
                            authorizationMessage = "Steam auth failed (json == null)";
                        }
                    }
                }
                // ##################################################### Steam auth

                // ##################################################### Whitelist check
                if (authorized && ServerWhitelist.get() != null) {
                    if (steamCheck) {
                        if (!ServerWhitelist.get().isPlayerWhitelisted(steamId)) {
                            authorized = false;
                            authorizationMessage = "Steam ID not whitelisted on this server";
                        }
                    } else {
                        if (!ServerWhitelist.get().isPlayerWhitelisted(p.username)) {
                            authorized = false;
                            authorizationMessage = "Username not whitelisted on this server";
                        }
                    }
                }
                // ##################################################### Whitelist check

                // ##################################################### Duplicate player check
                if (authorized) {
                    for (PlayerConnection con : PlayerConnectionHandler.get().connections()) {
                        if (steamCheck) {
                            if (con.steamId == steamId) {
                                authorized = false;
                                authorizationMessage = "Steam user already online";
                                break;
                            }
                        } else {
                            if (con.player.username.equals(p.username)) {
                                authorized = false;
                                authorizationMessage = "Username already online";
                                break;
                            }
                        }
                    }
                }
                // ##################################################### Duplicate player check

                WorldGenSettings s = ServerWorld.get().getMainDimension().getChunkHandler().getGenSettings();

                if(authorized) {
                    ServerPackets.p1AuthResponse(true, authorizationMessage, ExpoServerConfiguration.get().getServerTps(),
                            ExpoServerBase.get().getWorldSaveHandler().getWorldSeed(),
                            s,
                            PacketReceiver.connection(connection));
                } else {
                    ExpoLogger.log("Authentication for " + connection + " failed: " + authorizationMessage);
                    ServerPackets.p1AuthResponse(false, authorizationMessage, 0, 0, null, PacketReceiver.connection(connection));
                }

                if(authorized) {
                    ExpoLogger.log("Authentication for " + connection + " successful: " + authorizationMessage);
                    long finalSteamId = steamId;

                    CompletableFuture.runAsync(() -> {
                        PlayerSaveFile psf = ExpoServerBase.get().getWorldSaveHandler().getPlayerSaveHandler().loadAndGetPlayerFile(p.username, finalSteamId);

                        // Create player connection class
                        PlayerConnectionHandler handler = PlayerConnectionHandler.get();
                        PlayerConnection pc = handler.addPlayerConnection(connection, p.username, finalSteamId);

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
                        ServerPackets.p14WorldUpdate(sp.getDimension().getDimensionName(), sp.getDimension().dimensionTime, sp.getDimension().dimensionWeather.WEATHER_ID, sp.getDimension().dimensionWeatherStrength, PacketReceiver.connection(connection));
                        ServerPackets.p19ContainerUpdate(sp, PacketReceiver.player(sp));
                    });
                }
            }
            case P5_PlayerVelocity p -> {
                ServerPlayer player = connectionToPlayer(connection);

                if (player != null) {
                    player.xDir = p.xDir;
                    player.yDir = p.yDir;
                    player.sprinting = p.sprinting;
                }
            }
            case P12_PlayerDirection p -> {
                ServerPlayer player = connectionToPlayer(connection);

                if (player != null) {
                    player.playerDirection = p.direction;
                    ServerPackets.p12PlayerDirection(player.entityId, p.direction, PacketReceiver.whoCanSee(player));
                }
            }
            case P16_PlayerPunch p -> {
                ServerPlayer player = connectionToPlayer(connection);

                if (player != null) {
                    player.parsePunchPacket(p);
                }
            }
            case P18_PlayerInventoryInteraction p ->
                    doInventoryInteraction(connectionToPlayer(connection), p, PacketReceiver.connection(connection));
            case P20_PlayerInventorySwitch p -> {
                ServerPlayer player = connectionToPlayer(connection);

                if (player != null) {
                    player.switchToSlot(p.slot);
                }
            }
            case P22_PlayerArmDirection p -> {
                ServerPlayer player = connectionToPlayer(connection);

                if (player != null) {
                    player.applyArmDirection(p.rotation);
                }
            }
            case P25_ChatMessage p -> readChatMessage(false, p, connection, connectionToPlayer(connection));
            case P27_PlayerEntitySelection p -> {
                ServerPlayer player = connectionToPlayer(connection);
                player.selectedEntity = p.entityId;
            }
            case P31_PlayerDig p -> {
                ServerPlayer player = connectionToPlayer(connection);

                if (player != null) {
                    player.digAt(p.chunkX, p.chunkY, p.tileArray);
                }
            }
            case P34_PlayerPlace p -> {
                ServerPlayer player = connectionToPlayer(connection);

                if (player != null) {
                    player.placeAt(p.chunkX, p.chunkY, p.tileArray, p.mouseWorldX, p.mouseWorldY);
                }
            }
            case P35_PlayerCraft p -> {
                ServerPlayer player = connectionToPlayer(connection);

                if (player != null) {
                    CraftingRecipe recipe = CraftingRecipeMapping.get().getRecipeMap().get(p.recipeIdentifier);
                    player.playerInventory.craft(recipe, p.all);
                }
            }
            case P39_PlayerInteractEntity p -> {
                ServerPlayer player = connectionToPlayer(connection);
                if (player == null) return;

                ServerEntity interactionEntity = player.getDimension().getEntityManager().getEntityById(p.entityId);
                if (interactionEntity == null) return;

                interactionEntity.onInteraction(player);
            }
            case P41_InventoryViewQuit p -> {
                ServerPlayer player = connectionToPlayer(connection);
                if (player == null) return;

                if (player.viewingInventory != null) {
                    player.viewingInventory.removeInventoryViewer(player);
                    player.viewingInventory = null;
                }
            }
            case P48_ClientPlayerPosition p -> {
                ServerPlayer player = connectionToPlayer(connection);
                if (player == null) return;

                player.consumePosition(p);
            }
            case P49_PlayerEntityThrow p -> {
                ServerPlayer player = connectionToPlayer(connection);
                if (player == null) return;

                player.throwEntity(p.dstX, p.dstY);
            }
            default -> {}
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
            var change = player.playerInventory.performPlayerAction(p.actionType, p.slotId, p.shift);
            convertInventoryChangeResultToPacket(change, receiver);
        } else {
            if(player.viewingInventory != null) {
                if(player.viewingInventory.getContainerId() == p.containerId) {
                    var change = player.viewingInventory.performPlayerAction(player, p.actionType, p.slotId, p.shift);
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
