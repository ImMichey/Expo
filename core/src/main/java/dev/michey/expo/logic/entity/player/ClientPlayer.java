package dev.michey.expo.logic.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.audio.TrackedSoundData;
import dev.michey.expo.client.chat.ExpoClientChat;
import dev.michey.expo.input.IngameInput;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.misc.ClientSelector;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.logic.world.clientphysics.ClientPhysicsBody;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.font.GradientFont;
import dev.michey.expo.render.light.ExpoLight;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.SelectorType;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.render.ui.notification.UINotificationPiece;
import dev.michey.expo.render.visbility.TopVisibilityEntity;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.inventory.item.PlaceAlignment;
import dev.michey.expo.server.main.logic.inventory.item.PlaceData;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.packet.P17_PlayerPunchData;
import dev.michey.expo.server.packet.P19_ContainerUpdate;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.TeleportReason;
import dev.michey.expo.util.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static dev.michey.expo.util.ClientStatic.*;
import static dev.michey.expo.util.ExpoShared.*;

public class ClientPlayer extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity, TopVisibilityEntity {

    /** Last known player username. */
    public String username;
    /** Is this the player controlled player entity? */
    public boolean player;
    public ClientSelector selector;
    public boolean resetSelector;

    /** Player velocity */
    private boolean cachedSprinting;
    private boolean localSprinting;
    public float playerSpeed = 64f;
    public final float sprintMultiplier = 1.6f;
    public boolean noclip = false;
    public int clientDirX, clientDirY;              // Set by client

    public float movementPacketCooldown;
    public boolean queueMovementPacket;

    /** Player chunk */
    public int chunkX;
    public int chunkY;
    public int[] clientViewport = new int[] {Integer.MAX_VALUE, 0, 0, 0};

    /** Player textures */
    private static final float PLAYER_BLINK_COOLDOWN = 3.0f;
    private static final float PLAYER_BREATHE_COOLDOWN = 1.0f;
    private float playerBreatheDelta = MathUtils.random(PLAYER_BREATHE_COOLDOWN);
    private float playerBlinkDelta = MathUtils.random(PLAYER_BLINK_COOLDOWN);

    private float playerWalkDelta;
    private int playerWalkIndex;
    private int lastPlayerWalkIndex;

    public int playerDirection = 1; // 0 = Left, 1 = Right
    private int oldPlayerDirection = 1; // for packets

    /** Player textures v3 */
    private TextureRegion[] idle_body;
    private TextureRegion[] idle_eyes;
    private TextureRegion[] idle_frontArm;
    private TextureRegion[] idle_backArm;
    private TextureRegion[] idle_frontLeg;
    private TextureRegion[] idle_backLeg;

    private TextureRegion[] walk_body;
    private TextureRegion[] walk_eyes;
    private TextureRegion[] walk_frontArm;
    private TextureRegion[] walk_backArm;
    private TextureRegion[] walk_frontLeg;
    private TextureRegion[] walk_backLeg;

    private TextureRegion[] idle_armor_legs_frontLeg;
    private TextureRegion[] idle_armor_legs_backLeg;
    private TextureRegion[] walk_armor_legs_frontLeg;
    private TextureRegion[] walk_armor_legs_backLeg;

    private TextureRegion[] idle_armor_chest_body;
    private TextureRegion[] idle_armor_chest_frontArm;
    private TextureRegion[] idle_armor_chest_backArm;
    private TextureRegion[] walk_armor_chest_body;
    private TextureRegion[] walk_armor_chest_frontArm;
    private TextureRegion[] walk_armor_chest_backArm;
    private TextureRegion backArm_armor_chest;

    private TextureRegion blink;
    private TextureRegion backArm;
    private TextureRegion hair;

    private TextureRegion use_body, use_eyes, use_frontArm, use_backArm, use_frontLeg, use_backLeg,
            use_armor_legs_frontLeg, use_armor_legs_backLeg,
            use_armor_chest_body, use_armor_chest_backArm, use_armor_chest_frontArm,
            use_backArm_Detached;
    private float motionOffset;
    private static final float backArmOffsetY = 7;

    /** Player punch */
    public float punchStartAngle;
    public float punchEndAngle;
    private float punchStart;
    private float punchEnd;
    public float currentPunchAngle;
    private boolean punchAnimation;
    private int punchDirection;
    private boolean punchSound = true;
    private boolean dontUsePunchSound = false;
    private float lastPunchValue;

    public float serverPunchAngle;
    public float lerpedServerPunchAngle;
    public float lastLerpedServerPunchAngle;
    private float lastPunchAngleTimestamp;
    private float lastPunchAngleOnSend;
    private float serverPunchAngleStart;
    private float serverPunchAngleEnd;

    private float punchResetStartTimestamp;
    private float punchResetEndTimestamp;
    private float punchResetStartAngle;
    private float punchResetEndAngle;

    private boolean lastLeftClickSend;

    /** Player item */
    public int holdingItemId = -1;
    public Sprite[] holdingItemSprites = null;
    public List<ExpoLight> itemLightList = null;
    public int lightHoldingItemId = -1;

    public HashMap<Integer, TrackedSoundData> itemSoundMap = null;

    public int holdingArmorHeadId = -1;
    public ItemRender[] holdingHeadRender;
    public int holdingArmorChestId = -1;
    public ItemRender[] holdingChestRender;
    public int holdingArmorGlovesId = -1;
    public ItemRender[] holdingGlovesRender;
    public int holdingArmorLegsId = -1;
    public ItemRender[] holdingLegsRender;
    public int holdingArmorFeetId = -1;
    public ItemRender[] holdingFeetRender;

    private static final Vector2 NULL_ROTATION_VECTOR = GenerationUtils.circular(0, 1);

    /** World enter animation */
    public boolean finishedWorldEnterAnimation;
    private float worldAnimDelta;

    /** Player inventory */
    public PlayerInventory playerInventory;
    public static P19_ContainerUpdate QUEUED_INVENTORY_PACKET = null;

    public float playerHealth = 100f;
    public float playerHunger = 100f;
    private boolean notifiedHunger;

    /** Player reach */
    public float playerReachCenterX, playerReachCenterY;

    /** Client-side physics */
    private ClientPhysicsBody physicsBody;

    @Override
    public void onCreation() {
        visibleToRenderEngine = true; // player objects are always drawn by default, there is no visibility check
        drawReflection = true;

        physicsBody = new ClientPhysicsBody(this, -3, 0, 6, 6);

        if(player) {
            selector = new ClientSelector();
            selector.clientPosX = clientPosX;
            selector.clientPosY = clientPosY;
            entityManager().addClientSideEntity(selector);

            updateTexturePositionData();
            RenderContext.get().expoCamera.centerToPlayer(this);
        }

        { // Player textures v3
            idle_body = tra("player_idle_body", 2);
            idle_eyes = tra("player_idle_eyes", 2);
            idle_backArm = tra("player_idle_backarm", 2);
            idle_frontArm = tra("player_idle_frontarm", 2);
            idle_backLeg = tra("player_idle_backleg", 2);
            idle_frontLeg = tra("player_idle_frontleg", 2);

            walk_body = tra("player_walk_body", 8);
            walk_eyes = tra("player_walk_eyes", 8);
            walk_backArm = tra("player_walk_backarm", 8);
            walk_frontArm = tra("player_walk_frontarm", 8);
            walk_backLeg = tra("player_walk_backleg", 8);
            walk_frontLeg = tra("player_walk_frontleg", 8);

            blink = trn("player_blink");
            backArm = trn("player_arm_back");
            hair = trn("player_hair_var1");

            idle_armor_legs_frontLeg = tra("armor_legs_var1_idle_frontleg", 2);
            idle_armor_legs_backLeg = tra("armor_legs_var1_idle_backleg", 2);
            walk_armor_legs_frontLeg = tra("armor_legs_var1_walk_frontleg", 8);
            walk_armor_legs_backLeg = tra("armor_legs_var1_walk_backleg", 8);

            idle_armor_chest_body = tra("armor_chest_var1_idle_body", 2);
            idle_armor_chest_frontArm = tra("armor_chest_var1_idle_frontarm", 2);
            idle_armor_chest_backArm = tra("armor_chest_var1_idle_backarm", 2);
            walk_armor_chest_body = tra("armor_chest_var1_walk_body", 8);
            walk_armor_chest_frontArm = tra("armor_chest_var1_walk_frontarm", 8);
            walk_armor_chest_backArm = tra("armor_chest_var1_walk_backarm", 8);

            backArm_armor_chest = trn("armor_chest_var1_backarm");
        }

        if(player) {
            playerInventory = new PlayerInventory(this);

            if(QUEUED_INVENTORY_PACKET != null) {
                PacketUtils.readInventoryUpdatePacket(QUEUED_INVENTORY_PACKET);
                QUEUED_INVENTORY_PACKET = null;
            }

            finishedWorldEnterAnimation = ExpoServerBase.get() != null && ExpoServerBase.get().getWorldSaveHandler().getWorldName().startsWith("dev-world-");

            if(!finishedWorldEnterAnimation) {
                AudioEngine.get().playSoundGroup("woosh", 0.05f);
            }

            if(Expo.get().isMultiplayer()) {
                PlayerUI.get().chat.addServerMessage("You joined a multiplayer server.");
            } else {
                if(DEV_MODE) {
                    PlayerUI.get().chat.addServerMessage("You joined a singleplayer world in Dev Mode.");
                }
            }
        }
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    public void playPunchAnimation(int direction, float duration, boolean sound) {
        float sa = direction == 1 ? 0 : -180;
        float ea = direction == 1 ? 180 : 0;
        punchStartAngle = sa;
        punchEndAngle = ea;
        punchStart = RenderContext.get().deltaTotal;
        punchEnd = punchStart + duration;
        punchDirection = direction;

        punchResetStartTimestamp = punchEnd;
        punchResetEndTimestamp = punchResetStartTimestamp + 0.3f;
        punchResetStartAngle = 0;
        punchResetEndAngle = getFinalArmRotation();

        if(!sound) {
            dontUsePunchSound = true;
        }
    }

    public void applyServerPunchData(P17_PlayerPunchData p) {
        punchStartAngle = p.punchAngleStart;
        punchEndAngle = p.punchAngleEnd;
        punchStart = RenderContext.get().deltaTotal;
        punchEnd = punchStart + p.punchDuration;
        punchDirection = punchEndAngle > 0 ? 1 : 0;

        punchResetStartTimestamp = punchEnd;
        punchResetEndTimestamp = punchResetStartTimestamp + 0.3f;
        punchResetStartAngle = 0;
        punchResetEndAngle = getFinalArmRotation();

        dontUsePunchSound = false;
    }

    public void applyServerArmData(float rotation) {
        lastLerpedServerPunchAngle = lerpedServerPunchAngle;
        serverPunchAngle = rotation;
        serverPunchAngleStart = RenderContext.get().deltaTotal;
        serverPunchAngleEnd = serverPunchAngleStart + PLAYER_ARM_MOVEMENT_SEND_RATE;
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        setBlink();
        ParticleSheet.Common.spawnBloodParticles(this, 0, 0);

        if(!player) spawnHealthBar(damage);
        spawnDamageIndicator(damage, clientPosX, clientPosY + textureHeight + 28, entityManager().getEntityById(damageSourceEntityId));
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        updateChunkAndTile();
        updateTexturePositionData();

        float now = RenderContext.get().deltaTotal;

        if(player) {
            // Cloud stuff
            /*
            List<ClientEntity> clouds = entityManager().getEntitiesByType(ClientEntityType.CLOUD);
            int s = clouds.size();

            if(clientViewport[0] != Integer.MAX_VALUE && ExpoClientContainer.get().getClientWorld().dimensionName.equals(DIMENSION_OVERWORLD)) {
                if(s == 0) {
                    // Just created player.
                    float x = ExpoShared.chunkToPos(clientViewport[0]);
                    float x2 = ExpoShared.chunkToPos(clientViewport[1]);
                    float y = ExpoShared.chunkToPos(clientViewport[2]);
                    float y2 = ExpoShared.chunkToPos(clientViewport[3]);

                    List<Point> poisson = new PoissonDiskSampler(x, y, x2, y2, 256).sample();

                    for(Point p : poisson) {
                        if(MathUtils.random() <= 0.5f) continue;

                        ClientCloud cloud = new ClientCloud();
                        cloud.clientPosX = p.x;
                        cloud.clientPosY = p.y;
                        entityManager().addClientSideEntity(cloud);
                    }
                } else if(s < 16) {
                    // Respawn clouds on demand.
                    int toSpawn = 16 - s;

                    float x = ExpoShared.chunkToPos(clientViewport[0]);
                    float x2 = ExpoShared.chunkToPos(clientViewport[1]);
                    float y = ExpoShared.chunkToPos(clientViewport[2]);
                    float y2 = ExpoShared.chunkToPos(clientViewport[3]);

                    List<Point> poisson = new PoissonDiskSampler(x, y, x2, y2, 256).sample();
                    Collections.shuffle(poisson);

                    nextPoint: for(Point p : poisson) {
                        // Check for collision.
                        int cloudSize = 50;

                        for(var cloud : clouds) {
                            boolean overlaps = ExpoShared.overlap(
                                    new float[] {p.x - cloudSize, p.y - cloudSize, p.x + cloudSize, p.y + cloudSize},
                                    new float[] {cloud.finalDrawPosX, cloud.finalDrawPosY, cloud.textureWidth, cloud.textureHeight}
                            );

                            if(overlaps) {
                                continue nextPoint;
                            }
                        }

                        ClientCloud cloud = new ClientCloud();
                        cloud.clientPosX = p.x;
                        cloud.clientPosY = p.y;
                        entityManager().addClientSideEntity(cloud);
                        toSpawn--;

                        if(toSpawn <= 0) {
                            break;
                        }
                    }
                }
            }
            */

            // Selector update
            selector.blockSelection = false;

            if(holdingItemId != -1) {
                ItemMapping mapping = ItemMapper.get().getMapping(holdingItemId);

                boolean scanTile = false;
                boolean scanFreely = false;

                if(mapping.logic.isSpecialType()) {
                    ToolType tt = mapping.logic.toolType;

                    if(tt == ToolType.SHOVEL) {
                        scanTile = true;
                        selector.currentSelectorType = SelectorType.DIG_SHOVEL;
                    } else if(tt == ToolType.SCYTHE) {
                        scanTile = true;
                        selector.currentSelectorType = SelectorType.DIG_SCYTHE;
                    }

                    selector.currentEntityPlacementTexture = null;
                }
                if(mapping.logic.placeData != null) {
                    PlaceData placeData = mapping.logic.placeData;

                    if(placeData.floorType != null) {
                        selector.currentSelectorType = SelectorType.PLACE_TILE;
                    } else {
                        selector.currentSelectorType = SelectorType.PLACE_ENTITY;
                    }
                    selector.currentEntityPlacementTexture = placeData.previewTextureName;

                    if(placeData.alignment == PlaceAlignment.TILE) {
                        scanTile = true;
                    } else {
                        scanFreely = true;
                    }
                }

                // Scan process
                float tx = RenderContext.get().mouseWorldGridX;
                float ty = RenderContext.get().mouseWorldGridY;
                float range = mapping.logic.range + 8;
                float pvtAdjustmentX = 0, pvtAdjustmentY = 0;

                if(mapping.logic.placeData != null) {
                    TextureRegion pvt = tr(mapping.logic.placeData.previewTextureName);
                    pvtAdjustmentX = mapping.logic.placeData.previewOffsetX - pvt.getRegionWidth() * 0.5f;
                    pvtAdjustmentY = mapping.logic.placeData.previewOffsetY;
                }

                if(scanTile) {
                    selector.currentlyVisible = true;
                    selector.useTileCheck = true;

                    float d1 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx, ty);
                    float d2 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx + 16, ty);
                    float d3 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx + 16, ty + 16);
                    float d4 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx, ty + 16);

                    float d5 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx + 8, ty);
                    float d6 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx, ty + 8);
                    float d7 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx + 16, ty + 8);
                    float d8 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx + 8, ty + 16);

                    if(d1 <= range || d2 <= range || d3 <= range || d4 <= range || d5 <= range || d6 <= range || d7 <= range || d8 <= range) {
                        // Selected tile is in range, pick it
                        selector.tileGridX = ExpoShared.posToTile(tx);
                        selector.tileGridY = ExpoShared.posToTile(ty);
                        selector.worldX = (int) tx;
                        selector.worldY = (int) ty;
                        selector.drawPosX = selector.worldX + 8 + pvtAdjustmentX;
                        selector.drawPosY = selector.worldY + pvtAdjustmentY;
                    } else {
                        // Selected tile is not in range, pick the closest tile
                        Vector2 dst = GenerationUtils.circular(RenderContext.get().mouseRotation + 270, range);
                        float ntx = playerReachCenterX + dst.x;
                        float nty = playerReachCenterY + dst.y;

                        int _tix = ExpoShared.posToTile(ntx);
                        int _tiy = ExpoShared.posToTile(nty);

                        selector.tileGridX = _tix;
                        selector.tileGridY = _tiy;
                        selector.worldX = ExpoShared.tileToPos(_tix);
                        selector.worldY = ExpoShared.tileToPos(_tiy);
                        selector.drawPosX = selector.worldX + 8 + pvtAdjustmentX;
                        selector.drawPosY = selector.worldY + pvtAdjustmentY;

                        if(selector.currentSelectorType == SelectorType.DIG_SCYTHE || selector.currentSelectorType == SelectorType.DIG_SHOVEL) {
                            selector.blockSelection = true;
                        }
                    }
                } else if(scanFreely) {
                    selector.currentlyVisible = true;
                    selector.useFreeCheck = true;

                    // Revisit later.
                    float mx = RenderContext.get().mouseWorldX;
                    float my = RenderContext.get().mouseWorldY;
                    float d = Vector2.dst(playerReachCenterX, playerReachCenterY, mx, my);

                    if(d > range) {
                        Vector2 temp = new Vector2(mx, my).sub(playerReachCenterX, playerReachCenterY).nor().scl(range);
                        mx = temp.x + playerReachCenterX;
                        my = temp.y + playerReachCenterY;
                    }

                    selector.worldX = mx;
                    selector.worldY = my;
                    selector.drawPosX = mx + pvtAdjustmentX;
                    selector.drawPosY = my + pvtAdjustmentY;
                } else {
                    selector.currentlyVisible = false;
                }
            } else {
                selector.currentlyVisible = false;
            }

            if(resetSelector) {
                resetSelector = false;
                selector.reset0 = true;
            }

            // Player direction
            playerDirection = RenderContext.get().mouseDirection;

            if(oldPlayerDirection != playerDirection) {
                ClientPackets.p12PlayerDirection(playerDirection);
                oldPlayerDirection = playerDirection;
            }

            // Client-sided inventory check
            if(IngameInput.get().keyJustPressed(Input.Keys.ESCAPE) && PlayerUI.get().currentContainer != null) {
                PlayerUI.get().closeInventoryView();
                ClientPackets.p41InventoryViewQuit();
                AudioEngine.get().playSoundGroup("inv_open");
            } else if(IngameInput.get().keyJustPressed(Input.Keys.E)) {
                PlayerUI.get().togglePlayerInventoryView();
                AudioEngine.get().playSoundGroup("inv_open");
            }

            // World enter animation
            if(!finishedWorldEnterAnimation) {
                if(getUI().loadingScreen) {
                    int loadedChunks = ClientChunkGrid.get().getAllChunks().size();
                    int requiredChunks = ExpoShared.PLAYER_CHUNK_VIEW_RANGE_X * ExpoShared.PLAYER_CHUNK_VIEW_RANGE_Y;
                    int entitiesInQueue = ClientEntityManager.get().getEntitiesInAdditionQueue().size();

                    ClientUtils.log("-> " + loadedChunks + "/" + requiredChunks + "/" + entitiesInQueue, Input.Keys.X);

                    if(entitiesInQueue > 0) {
                        if(Gdx.input.isKeyJustPressed(Input.Keys.X)) {
                            for(var x : ClientEntityManager.get().getEntitiesInAdditionQueue()) {
                                ClientUtils.log(".." + x.getEntityType().name(), Input.Keys.X);
                            }
                        }
                    }

                    if(loadedChunks >= requiredChunks && entitiesInQueue == 0) { // entitiesInQueue check might cause issues in the future but for now it's a simple workaround
                        getUI().loadingScreen = false;
                    }
                } else {
                    worldAnimDelta += delta;

                    if(worldAnimDelta >= 1.0f) {
                        finishedWorldEnterAnimation = true;
                        worldAnimDelta = 1.0f;
                    }

                    RenderContext.get().expoCamera.camera.zoom = CAMERA_ANIMATION_MIN_ZOOM + (DEFAULT_CAMERA_ZOOM - CAMERA_ANIMATION_MIN_ZOOM) * Interpolation.pow5.apply(worldAnimDelta);
                }
            }

            // Player input + movement
            int xDir = 0, yDir = 0;
            boolean sprinting;

            if(IngameInput.get().keyPressed(Input.Keys.W)) yDir += 1;
            if(IngameInput.get().keyPressed(Input.Keys.S)) yDir -= 1;
            if(IngameInput.get().keyPressed(Input.Keys.A)) xDir -= 1;
            if(IngameInput.get().keyPressed(Input.Keys.D)) xDir += 1;

            boolean defaultSprint = GameSettings.get().runDefault;
            boolean sprintKey = IngameInput.get().keyPressed(Input.Keys.SHIFT_LEFT);

            if(sprintKey) {
                sprinting = !defaultSprint;
            } else {
                sprinting = defaultSprint;
            }

            localSprinting = sprinting;

            int numberPressed = IngameInput.get().pressedNumber();

            if(numberPressed != -1) {
                playerInventory.modifySelectedSlot(numberPressed);
            }

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

                ClientChunk chunk = chunkGrid().getChunk(ExpoShared.posToChunk(clientPosX + toMoveX), ExpoShared.posToChunk(clientPosY + toMoveY));

                if(chunk != null) {
                    var result = physicsBody.moveAbsolute(toMoveX + clientPosX, toMoveY + clientPosY, noclip ? PhysicsBoxFilters.noclipFilter : ClientPhysicsBody.clientPlayerFilter);

                    clientPosX = result.goalX - physicsBody.xOffset;
                    clientPosY = result.goalY - physicsBody.yOffset;

                    if(movementPacketCooldown <= 0) {
                        ClientPackets.p48ClientPlayerPosition(xDir, yDir, clientPosX, clientPosY, sprinting);
                        movementPacketCooldown += ((1f / (float) ExpoClientContainer.get().getServerTickRate()) * 0.5f);
                    } else {
                        movementPacketCooldown -= delta;
                        queueMovementPacket = true;
                    }
                }
            } else {
                if(movementPacketCooldown <= 0 && queueMovementPacket) {
                    ClientPackets.p48ClientPlayerPosition(xDir, yDir, clientPosX, clientPosY, sprinting);
                    movementPacketCooldown += ((1f / (float) ExpoClientContainer.get().getServerTickRate()) * 0.5f);
                    queueMovementPacket = false;
                } else {
                    movementPacketCooldown -= delta;
                }
            }

            clientDirX = xDir;
            clientDirY = yDir;

            // Update local player chunks
            chunkX = ExpoShared.posToChunk(clientPosX);
            chunkY = ExpoShared.posToChunk(clientPosY);

            clientViewport[0] = chunkX - PLAYER_CHUNK_VIEW_RANGE_DIR_X; // x start
            clientViewport[1] = chunkX + PLAYER_CHUNK_VIEW_RANGE_DIR_X; // x end
            clientViewport[2] = chunkY - PLAYER_CHUNK_VIEW_RANGE_DIR_Y; // y start
            clientViewport[3] = chunkY + PLAYER_CHUNK_VIEW_RANGE_DIR_Y; // y end

            if(Gdx.input.isKeyJustPressed(Input.Keys.R) && DEV_MODE) {
                new ItemMapper(true, true);
                Expo.get().loadItemMapperTextures();

                if(ClientPlayer.getLocalPlayer() != null && ClientPlayer.getLocalPlayer().holdingItemId != -1) {
                    ClientPlayer.getLocalPlayer().updateHoldingItemSprite(holdingItemId);
                }

                // This is a fix for UI ItemRender animations.
                if(UIContainerInventory.PLAYER_INVENTORY_CONTAINER != null) {
                    UIContainerInventory.PLAYER_INVENTORY_CONTAINER.setCraftGroupCategory(UIContainerInventory.PLAYER_INVENTORY_CONTAINER.selectedCraftGroupCategory);
                }
            }

            // Sending arm rotation packet if needed
            float currentRotation = RenderContext.get().mouseRotation;
            float timeSinceLastSend = RenderContext.get().deltaTotal - lastPunchAngleTimestamp;

            if(/*holdingItemId != -1 && */timeSinceLastSend >= PLAYER_ARM_MOVEMENT_SEND_RATE && (currentRotation != lastPunchAngleOnSend)) {
                lastPunchAngleTimestamp = RenderContext.get().deltaTotal;
                lastPunchAngleOnSend = currentRotation;
                ClientPackets.p22PlayerArmDirection(currentRotation);
            }

            if(timeSinceLastSend >= PLAYER_ARM_MOVEMENT_SEND_RATE) {
                boolean containerClosed = getUI().currentContainer == null;
                boolean holdingLeft = IngameInput.get().leftPressed() && ExpoClientContainer.get().getPlayerUI().hoveredSlot == null;
                boolean differs = lastLeftClickSend != holdingLeft;

                if(holdingLeft) {
                    // Is now clicking left.
                    if(differs && containerClosed) {
                        ClientPackets.p16PlayerPunch(currentRotation, true);
                        lastLeftClickSend = true;
                    }
                } else {
                    // Released left.
                    if(differs) {
                        ClientPackets.p16PlayerPunch(currentRotation, false);
                        lastLeftClickSend = false;
                    }
                }
            }

            if(IngameInput.get().rightJustPressed()) {
                if(holdingItemId != -1) {
                    ItemMapping mapping = ItemMapper.get().getMapping(holdingItemId);

                    if(mapping.logic.throwData != null) {
                        ClientPackets.p49PlayerThrowEntity(RenderContext.get().mouseWorldX, RenderContext.get().mouseWorldY);
                    } else {
                        handleRightClickNonItem();
                    }
                } else {
                    handleRightClickNonItem();
                }
            }
        } else {
            // MULTIPLAYER-ONLY code
            float n = RenderContext.get().deltaTotal;

            if(n >= serverPunchAngleEnd) {
                lerpedServerPunchAngle = serverPunchAngle;
            } else {
                float norm = (n - serverPunchAngleStart) / PLAYER_ARM_MOVEMENT_SEND_RATE;
                lerpedServerPunchAngle = lastLerpedServerPunchAngle + (serverPunchAngle - lastLerpedServerPunchAngle) * norm;
            }
        }

        boolean regularPunchAnimation = (punchEnd - now) > 0;
        boolean resetPunch = (punchResetEndTimestamp - now) > 0 && !regularPunchAnimation && holdingItemId != -1;
        punchAnimation = regularPunchAnimation || resetPunch;

        if(punchAnimation) {
            if(resetPunch) {
                float rt = player ? RenderContext.get().mouseRotation : serverPunchAngle;
                int dir = direction();

                if(dir == 1 && rt >= 0) {
                    punchResetEndAngle = rt;
                } else if(dir == 0 && rt <= 0) {
                    punchResetEndAngle = rt;
                }
            }

            float norm;
            float progress;

            if(resetPunch) {
                norm = 1f / (punchResetEndTimestamp - punchResetStartTimestamp);
                progress = (now - punchResetStartTimestamp) * norm;
            } else {
                norm = 1f / (punchEnd - punchStart);
                progress = (now - punchStart) * norm;
            }

            float interpolationValue = Interpolation.circle.apply(progress);

            if(punchDirection == 0) {
                if(resetPunch) {
                    currentPunchAngle = punchResetStartAngle - (punchResetStartAngle - punchResetEndAngle) * interpolationValue;
                } else {
                    currentPunchAngle = punchStartAngle - (punchStartAngle - punchEndAngle) * interpolationValue;
                }
            } else {
                if(resetPunch) {
                    currentPunchAngle = punchResetStartAngle + (punchResetEndAngle - punchResetStartAngle) * interpolationValue;
                } else {
                    currentPunchAngle = punchEndAngle - (punchEndAngle - punchStartAngle) * interpolationValue;
                }
            }

            if(!resetPunch && punchSound && interpolationValue >= 0.1f) {
                punchSound = false;

                if(player) {
                    if(!dontUsePunchSound) {
                        AudioEngine.get().playSoundGroup("punch");
                    }

                    if(selector.canDoAction()) {
                        selector.playPulseAnimation();
                        ClientPackets.p31PlayerDig(selector.toChunkX, selector.toChunkY, selector.tileGridX, selector.tileGridY, selector.toTileArray);
                    }
                } else {
                    if(!dontUsePunchSound) {
                        AudioEngine.get().playSoundGroupManaged("punch", new Vector2(finalTextureCenterX, finalTextureRootY), PLAYER_AUDIO_RANGE, false);
                    }
                }
            } else {
                if(lastPunchValue > interpolationValue) {
                    punchSound = true;
                }
            }

            if(resetPunch) {
                dontUsePunchSound = false;
            }

            lastPunchValue = interpolationValue;
        } else {
            punchSound = true;
            dontUsePunchSound = false;
        }

        // Player footstep sounds
        if((playerWalkIndex == 1 || playerWalkIndex == 5) && (playerWalkIndex != lastPlayerWalkIndex)) {
            if(getCurrentChunk() != null) {
                onFootstep();
            }
        }

        lastPlayerWalkIndex = playerWalkIndex;
    }

    private void handleRightClickNonItem() {
        if(selector.canDoAction()) {
            selector.playPulseAnimation();
            ClientPackets.p34PlayerPlace(selector.toChunkX, selector.toChunkY, selector.tileGridX, selector.tileGridY, selector.toTileArray,
                    selector.worldX, selector.worldY);
        } else {
            if(ClientEntityManager.get().selectedEntity != null) {
                ClientPackets.p39PlayerInteractEntity();
            }
        }
    }

    @Override
    public void applyPositionUpdate(float xPos, float yPos, int xDir, int yDir, boolean sprinting, float distance) {
        super.applyPositionUpdate(xPos, yPos, xDir, yDir, sprinting, distance);

        if(!player) {
            cachedSprinting = sprinting;
            physicsBody.moveAbsolute(xPos, yPos, PhysicsBoxFilters.noclipFilter);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        if(use_body == null) return;
        boolean drawLooseArm = holdingItemId != -1;

        drawHeldItemReflection(rc, false);

        if(punchAnimation || drawLooseArm) {
            float x = finalDrawPosX + (direction() == 1 ? 8 : 0);
            float originX = backArm.getRegionWidth() * 0.5f;
            float originY = backArm.getRegionHeight() - 1;
            float width = backArm.getRegionWidth();
            float height = backArm.getRegionHeight();
            float scaleX = 1.0f;
            float scaleY = 1.0f;
            float rotation = getFinalArmRotation();

            rc.arraySpriteBatch.draw(backArm, x, finalDrawPosY - motionOffset - backArmOffsetY - 20, originX, originY, width, height, scaleX, -scaleY, -rotation);
            rc.arraySpriteBatch.draw(use_backArm_Detached, x, finalDrawPosY - motionOffset - backArmOffsetY - 20, originX, originY, width, height, scaleX, -scaleY, -rotation);
        } else {
            rc.arraySpriteBatch.draw(use_backArm, finalDrawPosX, finalDrawPosY, use_backArm.getRegionWidth(), use_backArm.getRegionHeight() * -1);
            rc.arraySpriteBatch.draw(use_armor_chest_backArm, finalDrawPosX, finalDrawPosY, use_armor_chest_backArm.getRegionWidth(), use_armor_chest_backArm.getRegionHeight() * -1);
        }

        float w = use_body.getRegionWidth();
        float h = use_body.getRegionHeight() * -1;
        rc.arraySpriteBatch.draw(use_body, finalDrawPosX, finalDrawPosY, w, h);
        rc.arraySpriteBatch.draw(use_eyes, finalDrawPosX, finalDrawPosY, w, h);

        rc.arraySpriteBatch.draw(use_backLeg, finalDrawPosX, finalDrawPosY, w, h);
        rc.arraySpriteBatch.draw(use_frontLeg, finalDrawPosX, finalDrawPosY, w, h);
        rc.arraySpriteBatch.draw(use_armor_legs_backLeg, finalDrawPosX, finalDrawPosY, w, h);
        rc.arraySpriteBatch.draw(use_armor_legs_frontLeg, finalDrawPosX, finalDrawPosY, w, h);

        rc.arraySpriteBatch.draw(use_armor_chest_body, finalDrawPosX, finalDrawPosY, w, h);
        rc.arraySpriteBatch.draw(use_frontArm, finalDrawPosX, finalDrawPosY, w, h);
        rc.arraySpriteBatch.draw(use_armor_chest_frontArm, finalDrawPosX, finalDrawPosY, w, h);

        if(playerBlinkDelta < 0) {
            float dur = 0.25f;
            float norm = Math.abs(playerBlinkDelta) / dur * 2; // -> [2->0]
            float value; // [0->1->0]

            if(norm > 1) {
                // First half.
                value = Interpolation.smooth.apply(Math.abs(norm - 2));
            } else {
                // Second half.
                value = Interpolation.smooth.apply(norm);
            }

            rc.arraySpriteBatch.draw(blink, finalDrawPosX + (direction() == 1 ? 4 : 2), finalDrawPosY - 21 - motionOffset, blink.getRegionWidth(), blink.getRegionHeight() * value);
        }

        { // Draw player hair
            rc.arraySpriteBatch.draw(hair, finalDrawPosX + (direction() == 1 ? 1 : 0), finalDrawPosY - motionOffset - 20, hair.getRegionWidth(), hair.getRegionHeight() * -1);
        }

        drawHeldItemReflection(rc, true);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        updateDepth();
        float interpolatedBlink = tickBlink(delta, 7.5f);

        if(holdingItemSprites != null && holdingItemId != -1) {
            ItemRender[] ir = ItemMapper.get().getMapping(holdingItemId).heldRender;

            for(ItemRender irr : ir) {
                if(irr.updatedAnimation) {
                    updateHoldingItemSprite(holdingItemId);
                    break;
                }
            }
        }

        if(holdingItemId == -1 && itemSoundMap != null) {
            for(var x : itemSoundMap.values()) {
                AudioEngine.get().killSound(x.id);
            }

            itemSoundMap.clear();
        }

        if((holdingItemId == -1 || (holdingItemId != lightHoldingItemId)) && itemLightList != null) {
            clearItemLights();
        }

        if(holdingItemId != -1 && ((holdingItemId != lightHoldingItemId) || itemLightList == null)) {
            for(ItemRender ir : ItemMapper.get().getMapping(holdingItemId).heldRender) {
                spawnItemLights(ir);
            }
        }

        { // Updating breathe + blink
            playerBlinkDelta += delta;
            playerBreatheDelta += delta;

            float PLAYER_BLINK_DURATION = 0.25f;
            float PLAYER_BREATHE_DURATION = 0.5f;

            if(playerBlinkDelta >= PLAYER_BLINK_COOLDOWN) playerBlinkDelta = -PLAYER_BLINK_DURATION;
            if(playerBreatheDelta >= PLAYER_BREATHE_COOLDOWN) playerBreatheDelta = -PLAYER_BREATHE_DURATION;
        }

        { // Flip if necessary
            boolean flipX;

            if(punchAnimation) {
                flipX = (punchDirection == 0 && !flipped) || (punchDirection == 1 && flipped);
            } else {
                flipX = (playerDirection == 0 && !flipped) || (playerDirection == 1 && flipped);
            }

            if(flipX) {
                flipped = !flipped;

                if(holdingHeadRender != null) for(ItemRender ir : holdingHeadRender) ir.flip();
                if(holdingChestRender != null) for(ItemRender ir : holdingChestRender) ir.flip();
                if(holdingGlovesRender != null) for(ItemRender ir : holdingGlovesRender) ir.flip();
                if(holdingLegsRender != null) for(ItemRender ir : holdingLegsRender) ir.flip();
                if(holdingFeetRender != null) for(ItemRender ir : holdingFeetRender) ir.flip();

                flip(
                        walk_body, walk_eyes, walk_backArm, walk_frontArm, walk_backLeg, walk_frontLeg,
                        idle_body, idle_eyes, idle_backArm, idle_frontArm, idle_backLeg, idle_frontLeg,
                        idle_armor_legs_backLeg, idle_armor_legs_frontLeg, walk_armor_legs_backLeg, walk_armor_legs_frontLeg,
                        idle_armor_chest_body, idle_armor_chest_frontArm, idle_armor_chest_backArm,
                        walk_armor_chest_body, walk_armor_chest_frontArm, walk_armor_chest_backArm
                );

                backArm.flip(true, false);
                blink.flip(true, false);
                hair.flip(true, false);
            }
        }

        // Draw player
        boolean moving = isMoving();

        if(moving) {
            playerWalkDelta += delta * (isSprinting() ? 1.6f : 1.0f);
            float PLAYER_WALK_PER_FRAME_DURATION = 0.1f;

            if(playerWalkDelta >= PLAYER_WALK_PER_FRAME_DURATION) {
                playerWalkDelta -= PLAYER_WALK_PER_FRAME_DURATION;
                playerWalkIndex++;

                if(playerWalkIndex == walk_body.length) {
                    playerWalkIndex = 0;
                }
            }

            use_body = walk_body[playerWalkIndex];
            use_eyes = walk_eyes[playerWalkIndex];
            use_frontArm = walk_frontArm[playerWalkIndex];
            use_backLeg = walk_backLeg[playerWalkIndex];
            use_frontLeg = walk_frontLeg[playerWalkIndex];

            use_armor_legs_frontLeg = walk_armor_legs_frontLeg[playerWalkIndex];
            use_armor_legs_backLeg = walk_armor_legs_backLeg[playerWalkIndex];

            use_armor_chest_body = walk_armor_chest_body[playerWalkIndex];
            use_armor_chest_frontArm = walk_armor_chest_frontArm[playerWalkIndex];
            use_armor_chest_backArm = walk_armor_chest_backArm[playerWalkIndex];

            use_backArm = punchAnimation ? backArm : walk_backArm[playerWalkIndex];

            if(playerWalkIndex == 1 || playerWalkIndex == 5) {
                motionOffset = -1;
            } else if(playerWalkIndex == 3 || playerWalkIndex == 7) {
                motionOffset = 1;
            } else {
                motionOffset = 0;
            }
        } else {
            int playerIdleIndex = playerBreatheDelta < 0 ? 1 : 0;
            use_body = idle_body[playerIdleIndex];
            use_eyes = idle_eyes[playerIdleIndex];
            use_frontArm = idle_frontArm[playerIdleIndex];
            use_backLeg = idle_backLeg[playerIdleIndex];
            use_frontLeg = idle_frontLeg[playerIdleIndex];

            use_armor_legs_frontLeg = idle_armor_legs_frontLeg[playerIdleIndex];
            use_armor_legs_backLeg = idle_armor_legs_backLeg[playerIdleIndex];

            use_armor_chest_body = idle_armor_chest_body[playerIdleIndex];
            use_armor_chest_frontArm = idle_armor_chest_frontArm[playerIdleIndex];
            use_armor_chest_backArm = idle_armor_chest_backArm[playerIdleIndex];

            use_backArm = punchAnimation ? backArm : idle_backArm[playerIdleIndex];

            playerWalkDelta = 0;
            playerWalkIndex = 0;

            motionOffset = playerIdleIndex == 1 ? -1 : 0;
        }

        use_backArm_Detached = backArm_armor_chest;

        updateTextureBounds(use_body.getRegionWidth(), use_body.getRegionHeight(), 0.5f, 0);

        playerReachCenterX = finalTextureCenterX;
        playerReachCenterY = clientPosY + 14;

        chooseArrayBatch(rc, interpolatedBlink);
        rc.useArrayBatch();

        drawHeldItem(rc, false);
        boolean drawLooseArm = holdingItemId != -1;

        // Draw punch
        if(punchAnimation || drawLooseArm) {
            int px = punchAnimation ? (punchDirection == 1 ? 8 : 1) : (playerDirection == 1 ? 8 : 1);
            float x = finalDrawPosX + px;
            float y = finalDrawPosY + motionOffset + backArmOffsetY;
            float originX = backArm.getRegionWidth() * 0.5f;
            float originY = backArm.getRegionHeight() - 1;
            float width = backArm.getRegionWidth();
            float height = backArm.getRegionHeight();
            float scaleX = 1.0f;
            float scaleY = 1.0f;
            float rotation = getFinalArmRotation();

            rc.arraySpriteBatch.draw(backArm, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
            rc.arraySpriteBatch.draw(use_backArm_Detached, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        } else {
            rc.arraySpriteBatch.draw(use_backArm, finalDrawPosX, finalDrawPosY);
            rc.arraySpriteBatch.draw(use_armor_chest_backArm, finalDrawPosX, finalDrawPosY);
        }

        if(!Gdx.input.isKeyPressed(Input.Keys.U) || !DEV_MODE) {
            rc.arraySpriteBatch.draw(use_body, finalDrawPosX, finalDrawPosY);
            rc.arraySpriteBatch.draw(use_eyes, finalDrawPosX, finalDrawPosY);

                rc.arraySpriteBatch.draw(use_backLeg, finalDrawPosX, finalDrawPosY);
                rc.arraySpriteBatch.draw(use_frontLeg, finalDrawPosX, finalDrawPosY);
                rc.arraySpriteBatch.draw(use_armor_legs_backLeg, finalDrawPosX, finalDrawPosY);
                rc.arraySpriteBatch.draw(use_armor_legs_frontLeg, finalDrawPosX, finalDrawPosY);

            rc.arraySpriteBatch.draw(use_armor_chest_body, finalDrawPosX, finalDrawPosY);
            rc.arraySpriteBatch.draw(use_frontArm, finalDrawPosX, finalDrawPosY);
            rc.arraySpriteBatch.draw(use_armor_chest_frontArm, finalDrawPosX, finalDrawPosY);

            if(holdingArmorHeadId != -1) {
                int dir = punchAnimation ? punchDirection : playerDirection;

                for(ItemRender ir : holdingHeadRender) {
                    rc.arraySpriteBatch.draw(ir.useTextureRegion, finalDrawPosX + (dir == 1 ? 0 : -1) + ir.offsetX, finalDrawPosY + 13 + ir.offsetY);
                }
            }

            drawHeldItem(rc, true);
        }

        { // Draw player blink
            if(playerBlinkDelta < 0) {
                float dur = 0.25f;
                float norm = Math.abs(playerBlinkDelta) / dur * 2; // -> [2->0]
                float value; // [0->1->0]

                if(norm > 1) {
                    // First half.
                    value = Interpolation.smooth.apply(Math.abs(norm - 2));
                } else {
                    // Second half.
                    value = Interpolation.smooth.apply(norm);
                }

                rc.arraySpriteBatch.draw(blink, finalDrawPosX + (direction() == 1 ? 4 : 2), finalDrawPosY + 20 + motionOffset, blink.getRegionWidth(), blink.getRegionHeight() * value);
            }
        }

        { // Draw player hair
            rc.arraySpriteBatch.draw(hair, finalDrawPosX + (direction() == 1 ? 1 : 0), finalDrawPosY + motionOffset + 20);
        }

        rc.useRegularArrayShader();
    }

    private void clearItemLights() {
        for(var l : itemLightList) {
            l.delete();
        }

        itemLightList.clear();
        itemLightList = null;
    }

    @Override
    public void calculateReflection() {
        drawReflection = true;
    }

    private void onFootstep() {
        if(isInWater()) {
            spawnPuddle(isSprinting());
        }

        String group = getFootstepSound();
        if(!TileLayerType.isWater(getCurrentTileLayer()) && isSprinting()) {
            ParticleSheet.Common.spawnPlayerFootstepParticles(this);
        }

        if(player) {
            // Don't need dynamic volume + panning
            AudioEngine.get().playSoundGroup(group, isSprinting() ? 1.0f : 0.75f);
        } else {
            AudioEngine.get().playSoundGroupManaged(group, new Vector2(finalTextureCenterX, finalTextureRootY), PLAYER_AUDIO_RANGE * (isSprinting() ? 1.0f : 0.75f), false, isSprinting() ? 1.0f : 0.75f);
        }
    }

    @Override
    public float movementSpeedMultiplicator() {
        if(noclip) return 1.0f;
        return super.movementSpeedMultiplicator();
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        if(use_body != null) {
            rc.useRegularArrayShader();
            rc.useArrayBatch();

            { // Base body shadow
                Affine2 shadowBase = ShadowUtils.createSimpleShadowAffine(finalDrawPosX, finalDrawPosY);
                rc.arraySpriteBatch.drawGradient(use_body, use_body.getRegionWidth(), use_body.getRegionHeight(), shadowBase);          // check
            }

            float n = 1f / use_body.getRegionHeight();

            { // Armor shadow
                if(holdingArmorHeadId != -1) {
                    for(ItemRender ir : holdingHeadRender) {
                        float t = 0f;
                        float b = 1f - n * 21f;

                        float topColor = new Color(0f, 0f, 0f, t).toFloatBits();
                        float bottomColor = new Color(0f, 0f, 0f, b).toFloatBits();

                        int dir = punchAnimation ? punchDirection : playerDirection;
                        Affine2 shadowBase = ShadowUtils.createSimpleShadowAffineInternalOffset(finalDrawPosX, finalDrawPosY, dir == 1 ? 0 : -1, 13);
                        rc.arraySpriteBatch.drawGradientCustomColor(ir.useTextureRegion,
                                ir.useTextureRegion.getRegionWidth(), ir.useTextureRegion.getRegionHeight(), shadowBase, topColor, bottomColor);
                    }
                }
            }

            { // Item shadow
                if(holdingItemId != -1 && holdingItemSprites != null) {
                    ItemMapping map = ItemMapper.get().getMapping(holdingItemId);
                    int dirCheck = direction();
                    Vector2 v = (punchAnimation || (holdingItemId != -1)) ? GenerationUtils.circular(getFinalArmRotation(), 1) : NULL_ROTATION_VECTOR;

                    for(int i = 0; i < holdingItemSprites.length; i++) {
                        Sprite holdingItemSprite = holdingItemSprites[i];
                        ItemRender ir = map.heldRender[i];
                        if(ir.hideShadow) continue;

                        float armHeight = 10;
                        float ox = ir.offsetX * ir.scaleX;
                        float oy = ir.offsetY * ir.scaleY;
                        float inverse = dirCheck == 0 ? -1 : 1;

                        // rotation fix
                        float w = ir.useTextureRegion.getRegionWidth();
                        float h = ir.useTextureRegion.getRegionHeight();
                        float rfx = w * 0.5f;
                        float rfy = h * 0.5f;

                        if(ir.rotationLock) {
                            rfy = 0;
                        }

                        float shadowFixX = w * 0.5f * (1f - ir.scaleX);
                        float shadowFixY = h * 0.5f * (1f - ir.scaleY);

                        float _px = direction() == 1 ? 8 : 1;

                        float _x = _px + backArm.getRegionWidth() * 0.5f;
                        float _y = motionOffset + backArmOffsetY + backArm.getRegionHeight() - 1;

                        _x += (v.y * armHeight);
                        _y -= (v.x * armHeight);

                        _x -= rfx;
                        _y -= rfy;

                        _x += (v.y * ox) + (v.x * oy * inverse);
                        _y += -(v.x * ox) + (v.y * oy * inverse);

                        _x += shadowFixX;
                        _y += shadowFixY;

                        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(
                                finalDrawPosX,
                                finalDrawPosY,
                                _x, _y,
                                holdingItemSprite.getOriginX() - shadowFixX,
                                holdingItemSprite.getOriginY() - shadowFixY,
                                holdingItemSprite.getRotation()
                        );

                        float t = 0f + n * 10;
                        float b; //1f - n * offsetY; // calc
                        float norm;
                        int dir = direction();

                        if(dir == 1) {
                            // Right side.
                            norm = Math.abs(getFinalArmRotation() - 180) / 180;
                        } else {
                            norm = (getFinalArmRotation() + 180) / 180;
                        }

                        if(norm < 0.5f) {
                            // Upper half.
                            norm *= 2;
                            b = 0 + norm * n * 10;
                            t = 0 + norm * n * 10;
                        } else {
                            norm -= 0.5f;
                            norm *= 2;
                            b = (1f - n * (motionOffset + backArmOffsetY)) - n * 10 * (1f - norm);
                        }

                        float topColor = new Color(0f, 0f, 0f, t).toFloatBits();
                        float bottomColor = new Color(0f, 0f, 0f, b).toFloatBits();

                        rc.arraySpriteBatch.drawGradientCustomColor(
                                holdingItemSprite,
                                holdingItemSprite.getRegionWidth() * holdingItemSprite.getScaleX(),
                                holdingItemSprite.getRegionHeight() * holdingItemSprite.getScaleY(),
                                shadow,
                                topColor,
                                bottomColor);
                    }
                }
            }

            Affine2 shadowLeftArm = ShadowUtils.createSimpleShadowAffine(finalDrawPosX, finalDrawPosY);

            { // body parts
                rc.arraySpriteBatch.drawGradient(use_frontArm, use_frontArm.getRegionWidth(), use_frontArm.getRegionHeight(), shadowLeftArm);
                rc.arraySpriteBatch.drawGradient(use_backLeg, use_backLeg.getRegionWidth(), use_backLeg.getRegionHeight(), shadowLeftArm);
                rc.arraySpriteBatch.drawGradient(use_frontLeg, use_frontLeg.getRegionWidth(), use_frontLeg.getRegionHeight(), shadowLeftArm);

                Affine2 hairAffine = ShadowUtils.createSimpleShadowAffineInternalOffset(finalDrawPosX, finalDrawPosY, (direction() == 1 ? 1 : 0), motionOffset + 20);
                float hairBottomColor = new Color(0f, 0f, 0f, 1f - n * (motionOffset + 20)).toFloatBits();
                rc.arraySpriteBatch.drawGradientCustomColor(hair, hair.getRegionWidth(), hair.getRegionHeight(), hairAffine, 0f, hairBottomColor);
            }

            { // Right arm
                if(punchAnimation || (holdingItemId != -1)) {
                    float t = 0f + n * 10;
                    float b; //1f - n * offsetY; // calc
                    float norm;
                    int dir = direction();

                    if(dir == 1) {
                        // Right side.
                        norm = Math.abs(getFinalArmRotation() - 180) / 180;
                    } else {
                        norm = (getFinalArmRotation() + 180) / 180;
                    }

                    if(norm < 0.5f) {
                        // Upper half.
                        norm *= 2;
                        b = 0 + norm * n * 10;
                    } else {
                        norm -= 0.5f;
                        norm *= 2;
                        b = (1f - n * (backArmOffsetY + motionOffset)) - n * 10 * (1f - norm);
                    }

                    float topColor = new Color(0f, 0f, 0f, t).toFloatBits();
                    float bottomColor = new Color(0f, 0f, 0f, b).toFloatBits();

                    float originX = backArm.getRegionWidth() * 0.5f;
                    float originY = backArm.getRegionHeight() - 1;

                    Affine2 shadowRightArm = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(finalDrawPosX, finalDrawPosY,
                            direction() == 1 ? 8 : 1, backArmOffsetY + motionOffset, originX, originY, getFinalArmRotation());
                    rc.arraySpriteBatch.drawGradientCustomColor(backArm, backArm.getRegionWidth(), backArm.getRegionHeight(), shadowRightArm, topColor, bottomColor);
                } else {
                    rc.arraySpriteBatch.drawGradient(use_backArm, use_backArm.getRegionWidth(), use_backArm.getRegionHeight(), shadowLeftArm);
                }
            }
        }
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {
        if(Expo.get().isMultiplayer() || DEV_MODE) {
            BitmapFont use = rc.pickupFont;
            use.getData().setScale(0.5f);
            rc.globalGlyph.setText(use, username);
            GradientFont.drawGradient(use, rc.arraySpriteBatch, username, clientPosX - rc.globalGlyph.width * 0.5f, clientPosY + 32 + rc.globalGlyph.height);
            use.getData().setScale(1.0f);
        }
    }

    private void drawHeldItemReflection(RenderContext rc, boolean postArm) {
        if(holdingItemId != -1 && holdingItemSprites != null) {
            ItemMapping map = ItemMapper.get().getMapping(holdingItemId);

            for(int i = 0; i < holdingItemSprites.length; i++) {
                Sprite holdingItemSprite = holdingItemSprites[i];
                ItemRender ir = map.heldRender[i];

                if((postArm && ir.renderPriority) || (!postArm && !ir.renderPriority)) {
                    if(punchAnimation) {
                        if(!ir.rotationLock) {
                            if(punchDirection == 0) { // left
                                holdingItemSprite.setRotation(currentPunchAngle + 90f - ir.rotations[0]);
                            } else { // right
                                holdingItemSprite.setRotation(currentPunchAngle + ir.rotations[1]);
                            }
                        }

                        if(ir.requiresFlip) {
                            if(punchDirection == 0) {
                                if(!holdingItemSprite.isFlipX()) {
                                    holdingItemSprite.flip(true, false);
                                }
                            } else {
                                if(holdingItemSprite.isFlipX()) {
                                    holdingItemSprite.flip(true, false);
                                }
                            }
                        }
                    } else {
                        float desiredAngle = 0;
                        if(holdingItemId != -1) desiredAngle += getFinalArmRotation();

                        desiredAngle += (playerDirection == 0 ? (90f - ir.rotations[0]) : (ir.rotations[1]));

                        if(holdingItemSprite.getRotation() != desiredAngle && !ir.rotationLock) {
                            holdingItemSprite.setRotation(desiredAngle);
                        }

                        if(ir.requiresFlip) {
                            if(playerDirection == 0) {
                                if(!holdingItemSprite.isFlipX()) {
                                    holdingItemSprite.flip(true, false);
                                }
                            } else {
                                if(holdingItemSprite.isFlipX()) {
                                    holdingItemSprite.flip(true, false);
                                }
                            }
                        }
                    }

                    holdingItemSprite.setRotation(holdingItemSprite.getRotation() * -1);

                    // position
                    int dirCheck = direction();
                    Vector2 v;

                    if(punchAnimation || (holdingItemId != -1)) {
                        v = GenerationUtils.circular(getFinalArmRotation(), 1);
                    } else {
                        v = NULL_ROTATION_VECTOR;
                    }

                    float armHeight = 10f;
                    float ox = ir.offsetX * ir.scaleX;
                    float oy = ir.offsetY * ir.scaleY;
                    float inverse = dirCheck == 0 ? -1 : 1;

                    // rotation fix
                    float w = ir.useTextureRegion.getRegionWidth();
                    float h = ir.useTextureRegion.getRegionHeight();
                    float rfx = w * 0.5f;
                    float rfy = h * 0.5f;

                    if(ir.rotationLock) {
                        rfy *= 2;
                    }

                    float _px = direction() == 1 ? 8 : 1;

                    float _x = finalDrawPosX + _px + backArm.getRegionWidth() * 0.5f;
                    float _y = finalDrawPosY - motionOffset - backArmOffsetY - backArm.getRegionHeight() + 1;

                    _x += (v.y * armHeight) - rfx;
                    _y += (v.x * armHeight) - rfy;

                    _x += ((v.y * ox) + (v.x * oy * inverse));
                    _y -= (-(v.x * ox) + (v.y * oy * inverse));

                    holdingItemSprite.setPosition(_x, _y);
                    holdingItemSprite.setScale(1, -1);
                    holdingItemSprite.draw(rc.arraySpriteBatch);
                    holdingItemSprite.setScale(1, 1);
                }
            }
        }
    }

    private void drawHeldItem(RenderContext rc, boolean postArm) {
        if(holdingItemId != -1 && holdingItemSprites != null) {
            ItemMapping map = ItemMapper.get().getMapping(holdingItemId);

            for(int i = 0; i < holdingItemSprites.length; i++) {
                Sprite holdingItemSprite = holdingItemSprites[i];
                ItemRender ir = map.heldRender[i];

                if((postArm && ir.renderPriority) || (!postArm && !ir.renderPriority)) {
                    if(punchAnimation) {
                        if(!ir.rotationLock) {
                            if(punchDirection == 0) { // left
                                holdingItemSprite.setRotation(currentPunchAngle + 90f - ir.rotations[0]);
                            } else { // right
                                holdingItemSprite.setRotation(currentPunchAngle + ir.rotations[1]);
                            }
                        }

                        if(ir.requiresFlip) {
                            if(punchDirection == 0) {
                                if(!holdingItemSprite.isFlipX()) {
                                    holdingItemSprite.flip(true, false);
                                }
                            } else {
                                if(holdingItemSprite.isFlipX()) {
                                    holdingItemSprite.flip(true, false);
                                }
                            }
                        }
                    } else {
                        float desiredAngle = 0;
                        if(holdingItemId != -1) desiredAngle += getFinalArmRotation();

                        desiredAngle += (playerDirection == 0 ? (90f - ir.rotations[0]) : (ir.rotations[1]));

                        if(holdingItemSprite.getRotation() != desiredAngle && !ir.rotationLock) {
                            holdingItemSprite.setRotation(desiredAngle);
                        }

                        if(ir.requiresFlip) {
                            if(playerDirection == 0) {
                                if(!holdingItemSprite.isFlipX()) {
                                    holdingItemSprite.flip(true, false);
                                }
                            } else {
                                if(holdingItemSprite.isFlipX()) {
                                    holdingItemSprite.flip(true, false);
                                }
                            }
                        }
                    }

                    // position
                    int dirCheck = direction();
                    Vector2 v;

                    if(punchAnimation || (holdingItemId != -1)) {
                        v = GenerationUtils.circular(getFinalArmRotation(), 1);
                    } else {
                        v = NULL_ROTATION_VECTOR;
                    }

                    float armHeight = 10f;
                    float ox = ir.offsetX * ir.scaleX;
                    float oy = ir.offsetY * ir.scaleY;
                    float inverse = dirCheck == 0 ? -1 : 1;

                    // rotation fix
                    float w = ir.useTextureRegion.getRegionWidth();
                    float h = ir.useTextureRegion.getRegionHeight();
                    float rfx = w * 0.5f;
                    float rfy = h * 0.5f;

                    if(ir.rotationLock) {
                        rfy = 0;
                    }

                    float _px = direction() == 1 ? 8 : 1;

                    float _x = finalDrawPosX + _px + backArm.getRegionWidth() * 0.5f;
                    float _y = finalDrawPosY + motionOffset + backArmOffsetY + backArm.getRegionHeight() - 1;

                    _x += (v.y * armHeight);
                    _y -= (v.x * armHeight);

                    _x -= rfx;
                    _y -= rfy;

                    _x += (v.y * ox) + (v.x * oy * inverse);
                    _y += -(v.x * ox) + (v.y * oy * inverse);

                    holdingItemSprite.setPosition(_x, _y);
                    holdingItemSprite.draw(rc.arraySpriteBatch);

                    if(ir.particleEmitter != null) {
                        if(ir.particleEmitter.spawnParticlesThisTick) {
                            String st = ir.particleEmitter.emitterName;

                            if(st.equals("torch")) {
                                ParticleSheet.Common.spawnTorchParticles(depth, holdingItemSprite.getX() + rfx, holdingItemSprite.getY() + h * 0.5f);
                            }
                        }
                    }

                    if(itemLightList != null) {
                        for(ExpoLight light : itemLightList) {
                            light.update(holdingItemSprite.getX() + rfx, holdingItemSprite.getY() + rfy, rc.delta);
                        }
                    }

                    if(itemSoundMap != null) {
                        for(TrackedSoundData tsd : itemSoundMap.values()) {
                            tsd.worldPosition.set(holdingItemSprite.getX() + rfx, holdingItemSprite.getY() + rfy);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void applyEntityUpdatePayload(Object[] payload) {
        playerSpeed = (float) payload[0];
        noclip = (boolean) payload[1];
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO50(rc, 0.4f, 0.4f, 0, 0);
    }

    @Override
    public void applyTeleportUpdate(float xPos, float yPos, TeleportReason reason) {
        float x = clientPosX;
        float y = clientPosY;
        super.applyTeleportUpdate(xPos, yPos, reason);

        physicsBody.moveAbsolute(xPos, yPos, PhysicsBoxFilters.noclipFilter);

        if(player) {
            updateTexturePositionData();
            RenderContext.get().expoCamera.centerToPlayer(this);

            if(reason == TeleportReason.RESPAWN) {
                ExpoClientChat.get().addServerMessage("You died at [YELLOW]" + x + " [CYAN]" + y);
                PlayerUI.get().setFade(1.5f);
            }
        }
    }

    public void updateHoldingItemSprite(int itemId) {
        if(itemSoundMap != null) {
            List<Integer> remove = null;

            for(Map.Entry<Integer, TrackedSoundData> entrySet : itemSoundMap.entrySet()) {
                int iid = entrySet.getKey();

                if(iid != itemId) {
                    AudioEngine.get().killSound(entrySet.getValue().id);

                    if(remove == null) remove = new LinkedList<>();
                    remove.add(iid);
                }
            }

            if(remove != null) {
                for(int iid : remove) {
                    itemSoundMap.remove(iid);
                }
            }
        }

        ItemMapping mapping = ItemMapper.get().getMapping(itemId);
        holdingItemSprites = new Sprite[mapping.heldRender.length];

        for(int i = 0; i < holdingItemSprites.length; i++) {
            ItemRender ir = mapping.heldRender[i];

            Sprite s = new Sprite(ir.useTextureRegion);
            s.setPosition(finalDrawPosX, finalDrawPosY);
            s.setScale(ir.scaleX, ir.scaleY);
            s.setOrigin(s.getWidth() * 0.5f, s.getHeight() * 0.5f);
            holdingItemSprites[i] = s;

            if(ir.soundEmitter != null) {
                if(ir.soundEmitter.persistent) {
                    if(itemSoundMap == null) itemSoundMap = new HashMap<>();
                    boolean play = true;

                    for(int iid : itemSoundMap.keySet()) {
                        if(itemSoundMap.get(iid).qualifiedName.startsWith(ir.soundEmitter.soundGroup)) {
                            play = false;
                            break;
                        }
                    }

                    if(play) {
                        itemSoundMap.put(itemId, AudioEngine.get().playSoundGroupManaged(ir.soundEmitter.soundGroup,
                                new Vector2(clientPosX, clientPosY), ir.soundEmitter.volumeRange, true, ir.soundEmitter.volumeMultiplier));
                    }
                } else {
                    AudioEngine.get().playSoundGroupManaged(ir.soundEmitter.soundGroup, new Vector2(clientPosX, clientPosY), ir.soundEmitter.volumeRange, false, ir.soundEmitter.volumeMultiplier);
                }
            }
        }
    }

    private void spawnItemLights(ItemRender ir) {
        if(ir.renderLight != null && itemLightList == null) {
            lightHoldingItemId = holdingItemId;
            itemLightList = new LinkedList<>();

            ExpoLight expoLight = new ExpoLight(ir.renderLight.distanceMin, ir.renderLight.rayCount, ir.renderLight.emissionConstant, ir.renderLight.emissionQuadratic, false);
            expoLight.color(ir.renderLight.color);

            if(ir.renderLight.pulsating) {
                expoLight.setPulsating(ir.renderLight.pulsatingSpeed, ir.renderLight.distanceMin, ir.renderLight.distanceMax);
            }

            if(ir.renderLight.flicker) {
                expoLight.setFlickering(ir.renderLight.flickerStrength, ir.renderLight.flickerCooldown);
            }

            itemLightList.add(expoLight);
        }
    }

    public float toMouthX() {
        return finalDrawPosX + (direction() == 1 ? 7f : 4f);
    }

    public float toMouthY() {
        return finalDrawPosY + 19f + motionOffset;
    }

    private float getFinalArmRotation() {
        if(punchAnimation) return currentPunchAngle;
        return player ? RenderContext.get().mouseRotation : lerpedServerPunchAngle;
    }

    public void applyHealthHunger(float health, float hunger) {
        playerHealth = health;
        playerHunger = hunger;

        if(playerHunger <= 50f && !notifiedHunger) {
            notifiedHunger = true;
            PlayerUI.get().addNotification(tr("icon_hungry"), 5.0f, "stomach", new UINotificationPiece[] {
                    new UINotificationPiece("You are hungry!", Color.YELLOW)
            });
        } else if(playerHunger > 50) {
            notifiedHunger = false;
        }
    }

    public int direction() {
        return punchAnimation ? punchDirection : playerDirection;
    }

    public float getPlayerRange() {
        if(holdingItemId == -1) return ExpoShared.PLAYER_DEFAULT_RANGE;
        return ItemMapper.get().getMapping(holdingItemId).logic.range;
    }

    @Override
    public boolean isMoving() {
        if(player) {
            return clientDirX != 0 || clientDirY != 0;
        }
        return super.isMoving();
    }

    public boolean isSprinting() {
        if(player) {
            return localSprinting;
        }

        return cachedSprinting;
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.PLAYER;
    }

    private static ClientPlayer LOCAL_PLAYER;

    public static void setLocalPlayer(ClientPlayer localPlayer) {
        LOCAL_PLAYER = localPlayer;
    }

    public static ClientPlayer getLocalPlayer() {
        return LOCAL_PLAYER;
    }

    public PlayerUI getUI() {
        return ExpoClientContainer.get().getPlayerUI();
    }

}