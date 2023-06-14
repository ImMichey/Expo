package dev.michey.expo.logic.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.Expo;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.input.IngameInput;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.misc.ClientSelector;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.light.ExpoLight;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.packet.P17_PlayerPunchData;
import dev.michey.expo.server.packet.P19_PlayerInventoryUpdate;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.PacketUtils;

import static dev.michey.expo.util.ClientStatic.*;
import static dev.michey.expo.util.ExpoShared.*;

public class ClientPlayer extends ClientEntity {

    /** Last known player username. */
    public String username;
    /** Is this the player controlled player entity? */
    public boolean player;
    public ClientSelector selector;

    /** Player velocity */
    private int cachedVelocityX;
    private int cachedVelocityY;
    private boolean cachedSprinting;

    /** Player chunk */
    public int chunkX;
    public int chunkY;
    public int[] clientViewport = new int[4];

    /** Player textures */
    private TextureRegion draw_tex_base = null;
    private TextureRegion draw_tex_arm_right = null;
    private TextureRegion draw_tex_arm_left = null;
    private TextureRegion draw_tex_shadow_base = null;

    private float playerBreatheDelta;
    private float playerBlinkDelta;

    private float playerWalkDelta;
    private int playerWalkIndex;
    private int lastPlayerWalkIndex;

    public int playerDirection = 1; // 0 = Left, 1 = Right
    private int oldPlayerDirection = 1; // for packets

    /** Player textures v2 */
    private TextureRegion tex_base;
    private TextureRegion tex_breathe;
    private TextureRegion tex_arm_left;
    private TextureRegion tex_arm_right;
    private TextureRegion tex_punch_arm;
    private TextureRegion[] textures_walk;

    private TextureRegion tex_blink;

    private TextureRegion tex_shadow_base;
    private TextureRegion tex_shadow_breathe;
    private TextureRegion tex_shadow_arm_left;
    private TextureRegion tex_shadow_arm_right;
    private TextureRegion tex_shadow_punch_arm;
    private TextureRegion[] textures_shadow_walk;

    private final float[] offset_arm_walk_right = new float[] {7, 6, 7, 7, 8, 7, 6, 7, 7, 8};
    private final float[] offset_arm_base = new float[] {6, 7};

    private float offsetXL;
    private float offsetXR;
    public float offsetY;

    /** Player punch */
    public float punchStartAngle;
    public float punchEndAngle;
    private float punchStart;
    private float punchEnd;
    public float currentPunchAngle;
    private float clientPunchEnd;
    private boolean punchAnimation;
    private int punchDirection;
    private boolean punchSound = true;
    private float lastPunchValue;

    public float serverPunchAngle;
    public float lerpedServerPunchAngle;
    public float lastLerpedServerPunchAngle;
    private float lastPunchAngleTimestamp;
    private float lastPunchAngleOnSend;
    private float serverPunchAngleStart;
    private float serverPunchAngleEnd;

    /** Player item */
    public int holdingItemId = -1;
    public Sprite holdingItemSprite = null;

    public int holdingArmorHeadId = -1;
    public TextureRegion holdingArmorHeadTexture;
    public int holdingArmorChestId = -1;
    public TextureRegion holdingArmorChestTexture;
    public int holdingArmorGlovesId = -1;
    public TextureRegion holdingArmorGlovesTexture;
    public int holdingArmorLegsId = -1;
    public TextureRegion holdingArmorLegsTexture;
    public int holdingArmorFeetId = -1;
    public TextureRegion holdingArmorFeetTexture;

    private static final Vector2 NULL_ROTATION_VECTOR = GenerationUtils.circular(0, 1);

    /** World enter animation */
    private boolean finishedWorldEnterAnimation;
    private float worldAnimDelta;

    /** Player inventory */
    public PlayerInventory playerInventory;
    public boolean inventoryOpen;
    public static P19_PlayerInventoryUpdate QUEUED_INVENTORY_PACKET = null;

    public float playerHealth = 100f;
    public float playerHunger = 100f;

    /** Player reach */
    public float playerReachCenterX, playerReachCenterY;

    /** Player night proximity light */
    public ExpoLight proximityLight;

    @Override
    public void onCreation() {
        visibleToRenderEngine = true; // player objects are always drawn by default, there is no visibility check
        disableTextureCentering = true;

        if(player) {
            RenderContext.get().expoCamera.center(clientPosX, clientPosY);
            selector = new ClientSelector();
            entityManager().addClientSideEntity(selector);
        }

        TextureRegion baseSheet = trn("player_sheet");

        { // Base textures
            tex_base = new TextureRegion(baseSheet, 0, 6, 10, 26);
            tex_breathe = new TextureRegion(baseSheet, 24, 7, 10, 25);

            tex_arm_left = new TextureRegion(baseSheet, 48, 22, 2, 10);
            tex_arm_right = new TextureRegion(baseSheet, 72, 23, 1, 9);

            tex_punch_arm = new TextureRegion(baseSheet, 120, 22, 2, 10);

            textures_walk = trArrayFromSheet(baseSheet, 0, 69, 10, 27, 10, 24);

            tex_blink = trn("player_blink");
        }

        { // Shadow textures
            tex_shadow_base = new TextureRegion(baseSheet, 0, 38, 10, 26);
            tex_shadow_breathe = new TextureRegion(baseSheet, 24, 39, 10, 25);

            tex_shadow_arm_left = new TextureRegion(baseSheet, 48, 54, 2, 10);
            tex_shadow_arm_right = new TextureRegion(baseSheet, 72, 55, 1, 9);

            tex_shadow_punch_arm = new TextureRegion(baseSheet, 120, 54, 2, 10);

            textures_shadow_walk = shadowSheet(baseSheet, 0, 101, 10, 24, 10, 27, new int[] {
                    26, 25, 26, 26, 27, 26, 25, 26, 26, 27
            });
        }

        if(player) {
            playerInventory = new PlayerInventory(this);

            if(QUEUED_INVENTORY_PACKET != null) {
                PacketUtils.readInventoryUpdatePacket(QUEUED_INVENTORY_PACKET, playerInventory);
                QUEUED_INVENTORY_PACKET = null;
            }

            proximityLight = new ExpoLight(64.0f, 32, 1f, 0.0f);
            proximityLight.color(0.75f, 0.75f, 0.75f, 1.0f);

            finishedWorldEnterAnimation = ExpoServerBase.get() != null && ExpoServerBase.get().getWorldSaveHandler().getWorldName().startsWith("dev-world-");
        }
    }

    @Override
    public void onDeletion() {

    }

    public void applyServerPunchData(P17_PlayerPunchData p) {
        punchStartAngle = p.punchAngleStart;
        punchEndAngle = p.punchAngleEnd;
        punchStart = RenderContext.get().deltaTotal;
        punchEnd = punchStart + p.punchDuration;
        punchDirection = punchEndAngle > 0 ? 1 : 0;
    }

    public void applyServerArmData(float rotation) {
        lastLerpedServerPunchAngle = lerpedServerPunchAngle;
        serverPunchAngle = rotation;
        serverPunchAngleStart = RenderContext.get().deltaTotal;
        serverPunchAngleEnd = serverPunchAngleStart + PLAYER_ARM_MOVEMENT_SEND_RATE;
    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        updateChunkAndTile();
        updateTexturePositionData();

        float now = RenderContext.get().deltaTotal;

        if(player) {
            // Light update
            proximityLight.update(clientPosX + 5.0f, clientPosY + 22.0f);

            // Selector update
            if(holdingItemId != -1) {
                ItemMapping mapping = ItemMapper.get().getMapping(holdingItemId);

                boolean shovel = mapping.logic.isSpecialType() && mapping.logic.toolType == ToolType.SHOVEL;
                boolean placeable = holdingItemId >= 9 && holdingItemId <= 11;

                if(entityManager().selectedEntity == null && (shovel || placeable)) {
                    float tx = RenderContext.get().mouseWorldGridX;
                    float ty = RenderContext.get().mouseWorldGridY;
                    float range = mapping.logic.range;

                    float d1 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx, ty);
                    float d2 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx + 16, ty);
                    float d3 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx + 16, ty + 16);
                    float d4 = Vector2.dst(playerReachCenterX, playerReachCenterY, tx, ty + 16);

                    if(d1 <= range || d2 <= range || d3 <= range || d4 <= range) {
                        selector.tx = tx;
                        selector.ty = ty;
                        selector.tix = RenderContext.get().mouseTileX;
                        selector.tiy = RenderContext.get().mouseTileY;
                    } else {
                        Vector2 dst = GenerationUtils.circular(RenderContext.get().mouseRotation + 270, range);
                        float ntx = playerReachCenterX + dst.x;
                        float nty = playerReachCenterY + dst.y;

                        int _tix = ExpoShared.posToTile(ntx);
                        int _tiy = ExpoShared.posToTile(nty);

                        selector.tx = ExpoShared.tileToPos(_tix);
                        selector.ty = ExpoShared.tileToPos(_tiy);
                        selector.tix = _tix;
                        selector.tiy = _tiy;
                    }

                    selector.visible = true;
                    selector.selectionType = shovel ? 0 : 1;
                } else {
                    selector.visible = false;
                }
            } else {
                selector.visible = false;
            }

            // Player direction
            playerDirection = RenderContext.get().mouseDirection;

            if(oldPlayerDirection != playerDirection) {
                ClientPackets.p12PlayerDirection(playerDirection);
                oldPlayerDirection = playerDirection;
            }

            // Client-sided inventory check
            if(IngameInput.get().keyJustPressed(Input.Keys.ESCAPE) && inventoryOpen) {
                inventoryOpen = false;
                AudioEngine.get().playSoundGroup("inv_open");
            } else if(IngameInput.get().keyJustPressed(Input.Keys.E)) {
                inventoryOpen = !inventoryOpen;
                AudioEngine.get().playSoundGroup("inv_open");
            }

            // World enter animation
            if(!finishedWorldEnterAnimation) {
                worldAnimDelta += delta;

                if(worldAnimDelta >= 1.0f) {
                    finishedWorldEnterAnimation = true;
                    worldAnimDelta = 1.0f;
                }

                RenderContext.get().expoCamera.camera.zoom = CAMERA_ANIMATION_MIN_ZOOM + (DEFAULT_CAMERA_ZOOM - CAMERA_ANIMATION_MIN_ZOOM) * Interpolation.pow5.apply(worldAnimDelta);
            }

            // Player input + movement
            int xDir = 0, yDir = 0;
            boolean sprinting = false;

            if(IngameInput.get().keyPressed(Input.Keys.W)) yDir += 1;
            if(IngameInput.get().keyPressed(Input.Keys.S)) yDir -= 1;
            if(IngameInput.get().keyPressed(Input.Keys.A)) xDir -= 1;
            if(IngameInput.get().keyPressed(Input.Keys.D)) xDir += 1;
            if(IngameInput.get().keyPressed(Input.Keys.SHIFT_LEFT)) sprinting = true;

            int numberPressed = IngameInput.get().pressedNumber();

            if(numberPressed != -1) {
                playerInventory.modifySelectedSlot(numberPressed);
            }

            if(cachedVelocityX != xDir || cachedVelocityY != yDir || cachedSprinting != sprinting) {
                cachedVelocityX = xDir;
                cachedVelocityY = yDir;
                cachedSprinting = sprinting;
                ClientPackets.p5PlayerVelocity(xDir, yDir, sprinting);
            }

            boolean canSendPunchPacket = (punchEnd - now) < 0 && (clientPunchEnd - now) < 0;

            if(canSendPunchPacket && IngameInput.get().leftPressed() && !inventoryOpen) {
                clientPunchEnd = now + 0.1f;
                ClientPackets.p16PlayerPunch(RenderContext.get().mouseRotation);
            }

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
                if(ClientPlayer.getLocalPlayer() != null) ClientPlayer.getLocalPlayer().updateHoldingItemSprite();
            }

            // Sending arm rotation packet if needed
            float currentRotation = RenderContext.get().mouseRotation;
            float timeSinceLastSend = RenderContext.get().deltaTotal - lastPunchAngleTimestamp;

            if(/*holdingItemId != -1 && */timeSinceLastSend >= PLAYER_ARM_MOVEMENT_SEND_RATE && (currentRotation != lastPunchAngleOnSend)) {
                lastPunchAngleTimestamp = RenderContext.get().deltaTotal;
                lastPunchAngleOnSend = currentRotation;
                ClientPackets.p22PlayerArmDirection(currentRotation);
            }

            if(selector.visible && selector.canPlace && IngameInput.get().rightJustPressed()) {
                ClientPackets.p34PlayerPlace(selector.svChunkX, selector.svChunkY, selector.svTileX, selector.svTileY, selector.svTileArray);
            }
        } else {
            // Sync arm rotation if needed
            float n = RenderContext.get().deltaTotal;

            if(n >= serverPunchAngleEnd) {
                lerpedServerPunchAngle = serverPunchAngle;
            } else {
                float norm = (n - serverPunchAngleStart) / PLAYER_ARM_MOVEMENT_SEND_RATE;
                lerpedServerPunchAngle = lastLerpedServerPunchAngle + (serverPunchAngle - lastLerpedServerPunchAngle) * norm;
            }
        }

        punchAnimation = (punchEnd - now) > 0;

        if(punchAnimation) {
            float norm = 1f / (punchEnd - punchStart);
            float progress = (now - punchStart) * norm;
            float interpolationValue = Interpolation.circle.apply(progress);

            if(punchDirection == 0) {
                currentPunchAngle = punchStartAngle - (punchStartAngle - punchEndAngle) * interpolationValue;
            } else {
                currentPunchAngle = punchEndAngle - (punchEndAngle - punchStartAngle) * interpolationValue;
            }

            if(punchSound && interpolationValue >= 0.1f) {
                punchSound = false;

                if(player) {
                    AudioEngine.get().playSoundGroup("punch");

                    if(selector.visible) {
                        if(selector.canDig) {
                            ClientPackets.p31PlayerDig(selector.svChunkX, selector.svChunkY, selector.svTileX, selector.svTileY, selector.svTileArray);
                        }
                    }
                } else {
                    AudioEngine.get().playSoundGroupManaged("punch", new Vector2(finalTextureCenterX, finalTextureRootY), PLAYER_AUDIO_RANGE, false);
                }
            } else {
                if(lastPunchValue > interpolationValue) {
                    punchSound = true;
                }
            }

            lastPunchValue = interpolationValue;
        } else {
            punchSound = true;
        }

        // Player footstep sounds
        if((playerWalkIndex == 2 || playerWalkIndex == 7) && (playerWalkIndex != lastPlayerWalkIndex)) {
            String group = getFootstepSound();

            if(player) {
                // Don't need dynamic volume + panning
                AudioEngine.get().playSoundGroup(group);
            } else {
                AudioEngine.get().playSoundGroupManaged(group, new Vector2(finalTextureCenterX, finalTextureRootY), PLAYER_AUDIO_RANGE, false);
            }
        }

        lastPlayerWalkIndex = playerWalkIndex;
    }

    @Override
    public void applyPositionUpdate(float xPos, float yPos, int xDir, int yDir, boolean sprinting) {
        super.applyPositionUpdate(xPos, yPos, xDir, yDir, sprinting);

        if(!player) {
            cachedSprinting = sprinting;
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        updateDepth();

        { // Updating breathe + blink
            playerBlinkDelta += delta;
            playerBreatheDelta += delta;

            float PLAYER_BLINK_COOLDOWN = 3.0f;
            float PLAYER_BLINK_DURATION = 0.25f;

            float PLAYER_BREATHE_DURATION = 0.5f;
            float PLAYER_BREATHE_COOLDOWN = 1.0f;

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

                tex_blink.flip(true, false);

                tex_base.flip(true, false);
                tex_breathe.flip(true, false);
                tex_arm_left.flip(true, false);
                tex_arm_right.flip(true, false);
                tex_punch_arm.flip(true, false);

                if(holdingArmorHeadTexture != null) holdingArmorHeadTexture.flip(true, false);
                if(holdingArmorChestTexture != null) holdingArmorChestTexture.flip(true, false);
                if(holdingArmorGlovesTexture != null) holdingArmorGlovesTexture.flip(true, false);
                if(holdingArmorLegsTexture != null) holdingArmorLegsTexture.flip(true, false);
                if(holdingArmorFeetTexture != null) holdingArmorFeetTexture.flip(true, false);

                for(TextureRegion tex : textures_walk) {
                    tex.flip(true, false);
                }

                tex_shadow_base.flip(true, false);
                tex_shadow_breathe.flip(true, false);
                tex_shadow_arm_right.flip(true, false);
                tex_shadow_arm_left.flip(true, false);
                tex_shadow_punch_arm.flip(true, false);

                for(TextureRegion tex : textures_shadow_walk) {
                    tex.flip(true, false);
                }
            }
        }

        rc.useRegularBatch();

        // Draw player
        boolean moving = isMoving();

        if(moving) {
            playerWalkDelta += delta * (isSprinting() ? 1.25f : 1.0f);
            float PLAYER_WALK_PER_FRAME_DURATION = 0.10f;

            if(playerWalkDelta >= PLAYER_WALK_PER_FRAME_DURATION) {
                playerWalkDelta -= PLAYER_WALK_PER_FRAME_DURATION;
                playerWalkIndex++;

                if(playerWalkIndex == textures_walk.length) {
                    playerWalkIndex = 0;
                }
            }

            draw_tex_base = textures_walk[playerWalkIndex];
            draw_tex_shadow_base = textures_shadow_walk[playerWalkIndex];
        } else {
            draw_tex_base = playerBreatheDelta < 0 ? tex_breathe : tex_base;
            draw_tex_shadow_base = playerBreatheDelta < 0 ? tex_shadow_breathe : tex_shadow_base;

            playerWalkDelta = 0;
            playerWalkIndex = 0;
        }

        updateTextureBounds(draw_tex_base);

        playerReachCenterX = finalTextureCenterX;
        playerReachCenterY = clientPosY + 7;

        draw_tex_arm_left = tex_arm_left;
        draw_tex_arm_right = punchAnimation ? tex_punch_arm : tex_arm_right;

        // Draw player username
        //rc.m5x7_bordered.draw(rc.currentBatch, username, clientPosX - 12, clientPosY + 48);

        if(moving) {
            offsetY = offset_arm_walk_right[playerWalkIndex];
        } else {
            offsetY = offset_arm_base[playerBreatheDelta < 0 ? 0 : 1];
        }

        if(!flipped) {
            offsetXL = 1;
            offsetXR = 9;
        } else {
            offsetXL = 7;
            offsetXR = 0;
        }

        rc.useRegularBatch();

        drawHeldItem(rc, false);
        boolean drawLooseArm = holdingItemId != -1;

        // Draw punch (debug for now)
        if(punchAnimation || drawLooseArm) {
            int px = punchAnimation ? (punchDirection == 1 ? 8 : 0) : (playerDirection == 1 ? 8 : 0);
            float x = clientPosX + px;
            float y = clientPosY + offsetY;
            float originX = tex_punch_arm.getRegionWidth() * 0.5f;
            float originY = tex_punch_arm.getRegionHeight() - 1;
            float width = tex_punch_arm.getRegionWidth();
            float height = tex_punch_arm.getRegionHeight();
            float scaleX = 1.0f;
            float scaleY = 1.0f;
            float rotation = getFinalArmRotation();

            rc.batch.draw(tex_punch_arm, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        } else {
            rc.batch.draw(draw_tex_arm_right, clientPosX + offsetXR, clientPosY + offsetY);
        }

        if(!Gdx.input.isKeyPressed(Input.Keys.U) || !DEV_MODE) {
            rc.batch.draw(draw_tex_base, clientPosX, clientPosY);
            rc.batch.draw(draw_tex_arm_left, clientPosX + offsetXL, clientPosY + offsetY);

            if(holdingArmorHeadId != -1) {
                ItemMapping map = ItemMapper.get().getMapping(holdingArmorHeadId);
                int dir = punchAnimation ? punchDirection : playerDirection;
                rc.batch.draw(holdingArmorHeadTexture, clientPosX + (dir == 1 ? 0 : -1) + map.armorRender.offsetX, clientPosY + 13 + offsetY + map.armorRender.offsetY);
            }

            drawHeldItem(rc, true);
        }

        { // Draw player blink
            if(playerBlinkDelta < 0) {
                rc.batch.draw(tex_blink, clientPosX + 5 - (direction() == 0 ? 4 : 0), clientPosY + offsetY + 13);
            }
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        if(draw_tex_shadow_base != null) {
            if(rc.arraySpriteBatch.getShader() != rc.DEFAULT_GLES3_ARRAY_SHADER) rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
            rc.useArrayBatch();

            { // Base body shadow
                Affine2 shadowBase = ShadowUtils.createSimpleShadowAffine(clientPosX, clientPosY);
                rc.arraySpriteBatch.drawGradient(draw_tex_shadow_base, draw_tex_shadow_base.getRegionWidth(), draw_tex_shadow_base.getRegionHeight(), shadowBase);          // check
            }

            float n = 1f / draw_tex_shadow_base.getRegionHeight();

            { // Armor shadow
                if(holdingArmorHeadId != -1) {
                    float t = 0f;
                    float b = 1f - n * 21f;

                    float topColor = new Color(0f, 0f, 0f, t).toFloatBits();
                    float bottomColor = new Color(0f, 0f, 0f, b).toFloatBits();

                    int dir = punchAnimation ? punchDirection : playerDirection;
                    Affine2 shadowBase = ShadowUtils.createSimpleShadowAffineInternalOffset(clientPosX, clientPosY, dir == 1 ? 0 : -1, 13 + offsetY);
                    rc.arraySpriteBatch.drawGradientCustomColor(holdingArmorHeadTexture, holdingArmorHeadTexture.getRegionWidth(), holdingArmorHeadTexture.getRegionHeight(), shadowBase, topColor, bottomColor);
                }
            }

            { // Item shadow
                if(holdingItemId != -1 && holdingItemSprite != null) {
                    // position
                    ItemMapping map = ItemMapper.get().getMapping(holdingItemId);
                    int dirCheck = direction();
                    Vector2 v = (punchAnimation || (holdingItemId != -1)) ? GenerationUtils.circular(getFinalArmRotation(), 1) : NULL_ROTATION_VECTOR;

                    float xshift = dirCheck == 0 ? 1 : 9;
                    float armHeight = 9;
                    float ox = map.heldRender.offsetX * map.heldRender.scaleX;
                    float oy = map.heldRender.offsetY * map.heldRender.scaleY;
                    float inverse = dirCheck == 0 ? -1 : 1;

                    // rotation fix
                    float w = map.heldRender.textureRegion.getRegionWidth();
                    float h = map.heldRender.textureRegion.getRegionHeight();
                    float rfx = w * 0.5f;
                    float rfy = h * 0.5f;

                    float shadowFixX = w * 0.5f * (1f - map.heldRender.scaleX);
                    float shadowFixY = h * 0.5f * (1f - map.heldRender.scaleY);

                    Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(
                            clientPosX,
                            clientPosY,
                            shadowFixX + xshift - (rfx) + (v.y * armHeight) + (v.y * ox) + (v.x * oy * inverse),
                            shadowFixY + armHeight - (rfy) - (v.x * armHeight) + offsetY - (v.x * ox) + (v.y * oy * inverse),
                            holdingItemSprite.getOriginX() - shadowFixX,
                            holdingItemSprite.getOriginY() - shadowFixY,
                            holdingItemSprite.getRotation()
                    );

                    float t = 0f + n * 9;
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
                        b = 0 + norm * n * 9;
                        t = 0 + norm * n * 9;
                    } else {
                        norm -= 0.5f;
                        norm *= 2;
                        b = (1f - n * offsetY) - n * 9 * (1f - norm);
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

            { // Left arm
                float t = 0f + n * 9;
                float b = 1f - n * offsetY;

                float topColor = new Color(0f, 0f, 0f, t).toFloatBits();
                float bottomColor = new Color(0f, 0f, 0f, b).toFloatBits();

                Affine2 shadowLeftArm = ShadowUtils.createSimpleShadowAffineInternalOffset(clientPosX, clientPosY, offsetXL, offsetY);
                rc.arraySpriteBatch.drawGradientCustomColor(tex_shadow_arm_left, tex_shadow_arm_left.getRegionWidth(), tex_shadow_arm_left.getRegionHeight(), shadowLeftArm, topColor, bottomColor);
            }

            { // Right arm
                if(punchAnimation || (holdingItemId != -1)) {
                    float t = 0f + n * 9;
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
                        b = 0 + norm * n * 9;
                    } else {
                        norm -= 0.5f;
                        norm *= 2;
                        b = (1f - n * offsetY) - n * 9 * (1f - norm);
                    }

                    float topColor = new Color(0f, 0f, 0f, t).toFloatBits();
                    float bottomColor = new Color(0f, 0f, 0f, b).toFloatBits();

                    float originX = tex_punch_arm.getRegionWidth() * 0.5f;
                    float originY = tex_punch_arm.getRegionHeight() - 1;

                    Affine2 shadowRightArm = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(clientPosX, clientPosY, direction() == 1 ? 8 : 0, offsetY, originX, originY, getFinalArmRotation());
                    rc.arraySpriteBatch.drawGradientCustomColor(tex_shadow_punch_arm, tex_shadow_punch_arm.getRegionWidth(), tex_shadow_punch_arm.getRegionHeight(), shadowRightArm, topColor, bottomColor);
                } else {
                    float t = 0f + n * 10;
                    float b = 1f - n * offsetY;

                    float topColor = new Color(0f, 0f, 0f, t).toFloatBits();
                    float bottomColor = new Color(0f, 0f, 0f, b).toFloatBits();

                    Affine2 shadowRightArm = ShadowUtils.createSimpleShadowAffineInternalOffset(clientPosX, clientPosY, offsetXR, offsetY);
                    rc.arraySpriteBatch.drawGradientCustomColor(tex_shadow_arm_right, tex_shadow_arm_right.getRegionWidth(), tex_shadow_arm_right.getRegionHeight(), shadowRightArm, topColor, bottomColor);
                }
            }
        }
    }

    private void drawHeldItem(RenderContext rc, boolean postArm) {
        if(holdingItemId != -1 && holdingItemSprite != null) {
            ItemMapping map = ItemMapper.get().getMapping(holdingItemId);

            if((map.heldRender.renderPriority && postArm) || (!map.heldRender.renderPriority && !postArm)) {
                // rotation
                if(punchAnimation) {
                    if(punchDirection == 0) { // left
                        holdingItemSprite.setRotation(currentPunchAngle + 90f - map.heldRender.rotations[0]);
                    } else { // right
                        holdingItemSprite.setRotation(currentPunchAngle + map.heldRender.rotations[1]);
                    }

                    if(map.heldRender.requiresFlip) {
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

                    desiredAngle += (playerDirection == 0 ? (90f - map.heldRender.rotations[0]) : (map.heldRender.rotations[1]));

                    if(holdingItemSprite.getRotation() != desiredAngle) {
                        holdingItemSprite.setRotation(desiredAngle);
                    }

                    if(map.heldRender.requiresFlip) {
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

                float xshift = dirCheck == 0 ? 1 : 9;
                float armHeight = 9;
                float ox = map.heldRender.offsetX * map.heldRender.scaleX;
                float oy = map.heldRender.offsetY * map.heldRender.scaleY;
                float inverse = dirCheck == 0 ? -1 : 1;

                // rotation fix
                float w = map.heldRender.textureRegion.getRegionWidth();
                float h = map.heldRender.textureRegion.getRegionHeight();
                float rfx = w * 0.5f;
                float rfy = h * 0.5f;

                holdingItemSprite.setPosition(
                        clientPosX + xshift - (rfx) + (v.y * armHeight) + (v.y * ox) + (v.x * oy * inverse),
                        clientPosY + armHeight - (rfy) - (v.x * armHeight) + offsetY - (v.x * ox) + (v.y * oy * inverse)
                );

                holdingItemSprite.draw(rc.batch);
            }
        }
    }

    @Override
    public void applyTeleportUpdate(float xPos, float yPos) {
        super.applyTeleportUpdate(xPos, yPos);
        if(player) {
            RenderContext.get().expoCamera.resetLerp();
            updateTexturePositionData();
            RenderContext.get().expoCamera.centerToEntity(this);
        }
    }

    public void updateHoldingItemSprite() {
        if(holdingItemId == -1) return;
        ItemMapping mapping = ItemMapper.get().getMapping(holdingItemId);
        holdingItemSprite = new Sprite(mapping.heldRender.textureRegion);
        holdingItemSprite.setPosition(clientPosX, clientPosY);
        holdingItemSprite.setScale(mapping.heldRender.scaleX, mapping.heldRender.scaleY);
        holdingItemSprite.setOrigin(
                holdingItemSprite.getWidth() * 0.5f,
                holdingItemSprite.getHeight() * 0.5f
        );
    }

    public float toMouthX() {
        return clientPosX + (direction() == 1 ? 6.5f : 2.5f);
    }

    public float toMouthY() {
        return clientPosY + 10.5f + offsetY;
    }

    private float getFinalArmRotation() {
        if(punchAnimation) return currentPunchAngle;
        return player ? RenderContext.get().mouseRotation : lerpedServerPunchAngle;
    }

    private TextureRegion[] shadowSheet(TextureRegion base, int x, int y, int frames, int cellWidth, int width, int maxHeight, int[] heightArray) {
        TextureRegion[] array = new TextureRegion[frames];

        for(int i = 0; i < array.length; i++) {
            int py = maxHeight - heightArray[i];
            array[i] = new TextureRegion(base, x + (i * cellWidth), y + py, width, heightArray[i]);
        }

        return array;
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
        if(player) return cachedVelocityX != 0 || cachedVelocityY != 0;
        return super.isMoving();
    }

    public boolean isSprinting() {
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
