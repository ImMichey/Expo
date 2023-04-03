package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.server.config.ExpoServerConfiguration;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.fs.whitelist.ServerWhitelist;
import dev.michey.expo.server.fs.world.player.PlayerSaveFile;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.arch.ExpoServerDedicated;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.gen.WorldGenSettings;
import dev.michey.expo.server.packet.*;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONArray;

import java.util.Collection;
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

            ServerPackets.p1AuthResponse(true, "Local server", ExpoShared.DEFAULT_SERVER_TICK_RATE, sp.getDimension().getChunkHandler().getNoise().getSeed(), s, PacketReceiver.local());
            ServerPackets.p3PlayerJoin(req.username, PacketReceiver.local());
            ServerPackets.p9PlayerCreate(sp, true, PacketReceiver.local());
            ServerPackets.p14WorldUpdate(sp.getDimension().dimensionTime, sp.getDimension().dimensionWeather.WEATHER_ID, sp.getDimension().dimensionWeatherStrength, PacketReceiver.local());
            ServerPackets.p19PlayerInventoryUpdate(sp, PacketReceiver.local());
            //sp.switchToSlot(0);
        } else if(packet instanceof P5_PlayerVelocity vel) {
            ServerPlayer sp = ServerPlayer.getLocalPlayer();
            if(sp == null) return;

            sp.xDir = vel.xDir;
            sp.yDir = vel.yDir;
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
                for(PlayerConnection con : PlayerConnectionHandler.get().connections()) {
                    if(con.player.username.equals(p.username)) {
                        authorized = false;
                        authorizationMessage = "Username already online";
                        break;
                    }
                }
            }

            WorldGenSettings s = ServerWorld.get().getMainDimension().getChunkHandler().getGenSettings();
            ServerPackets.p1AuthResponse(authorized, authorizationMessage, ExpoServerConfiguration.get().getServerTps(), ExpoServerBase.get().getWorldSaveHandler().getWorldSeed(), s, PacketReceiver.connection(connection));

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
                    ServerPackets.p19PlayerInventoryUpdate(sp, PacketReceiver.player(sp));
                });
            }
        } else if(o instanceof P5_PlayerVelocity p) {
            ServerPlayer player = connectionToPlayer(connection);

            if(player != null) {
                player.xDir = p.xDir;
                player.yDir = p.yDir;
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
        }
    }

    private void readChatMessage(boolean local, P25_ChatMessage p, Connection connection, ServerPlayer serverPlayer) {
        if(p.message.startsWith("/")) {
            ExpoServerBase.get().getCommandResolver().resolveCommand(p.message, serverPlayer);
        } else {
            if(!local) {
                ServerPackets.p25ChatMessage(p.sender, p.message, PacketReceiver.allExcept(connection));
            }
        }
    }

    private void doInventoryInteraction(ServerPlayer player, P18_PlayerInventoryInteraction p, PacketReceiver receiver) {
        var change = player.playerInventory.performPlayerAction(p.actionType, p.slotId);

        if(change != null) {
            ServerInventoryItem[] arr = new ServerInventoryItem[change.changedItems.size()];

            for(int i = 0; i < arr.length; i++) {
                arr[i] = change.changedItems.get(i);
            }

            ServerPackets.p19PlayerInventoryUpdate(
                    change.changedSlots.stream().mapToInt(Integer::intValue).toArray(),
                    arr,
                    receiver);
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
