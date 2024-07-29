package dev.michey.expo.render.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.client.chat.ExpoClientChat;
import dev.michey.expo.lang.Lang;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.ClientInventorySlot;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.font.GradientFont;
import dev.michey.expo.render.ui.container.UIContainer;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.render.ui.notification.UINotification;
import dev.michey.expo.render.ui.notification.UINotificationPiece;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.inventory.InventoryViewType;
import dev.michey.expo.server.main.logic.inventory.ServerInventorySlot;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ClientUtils;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.weather.Weather;

import java.util.LinkedList;

import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public class PlayerUI {

    /** The scale for all UI elements (will be changeable in the options). */
    public float uiScale; // default = 2.0f
    private float uiWidth, uiHeight;

    /** Loading screen */
    public boolean loadingScreen = true;

    /** Tab list */
    public boolean tablistOpen = false;
    private float pthW, pthH;
    private float pingW, pingH;
    public float cornerWH;
    private float borderWH;
    public GlyphLayout glyphLayout;

    /** Notifications */
    private final LinkedList<UINotification> notificationList;
    private final Object notificationLock = new Object();

    /** Hotbar */
    private float hotbarW, hotbarH;
    public float slotW, slotH;
    public final Color COLOR_GREEN = new Color(127f / 255f, 237f / 255f, 51f / 255f, 1.0f);
    public final String COLOR_GREEN_HEX = COLOR_GREEN.toString();
    private final Color COLOR_YELLOW  = new Color(251f / 255f, 242f / 255f, 54f / 255f, 1.0f);
    private final Color COLOR_RED = new Color(210f / 255f, 27f / 255f, 27f / 255f, 1.0f);
    public static final String COLOR_DESCRIPTOR_HEX = "[#5875b0]";
    public static final String COLOR_DESCRIPTOR2_HEX = "[#bdc4d4]";
    public static final Color COLOR_TEX_DESCRIPTOR = Color.valueOf("#9ec07f");
    private final float[] COLOR_GRADIENTS = new float[] {100f, 80f, 60f, 40f, 20f, 0f};

    public InteractableItemSlot[] hotbarSlots;
    public InteractableUIElement hoveredSlot = null;

    /** Fade in */
    public float fadeInDelta;
    public float fadeInDuration = 3.0f;

    public float fadeRainDelta;
    public float fadeRainDuration = 1.0f;
    public float fadeRainTarget;

    /** Minimap */
    public final PlayerMinimap playerMinimap;

    /** Multiplayer chat */
    public final ExpoClientChat chat;

    /** Textures */
    public final TextureRegion invSlot;             // Regular Item Slot
    public final TextureRegion invSlotS;            // Regular Item Slot (hovered)

    private final TextureRegion hotbarBase;

    public final TextureRegion playerTabHead;

    private final TextureRegion tabTopLeft;
    private final TextureRegion tabTopRight;
    private final TextureRegion tabBottomLeft;
    private final TextureRegion tabBottomRight;
    private final TextureRegion tabBorder3x1;
    private final TextureRegion tabBorder1x3;
    private final TextureRegion tabPingIcon;

    public final TextureRegion whiteSquare;

    private final TextureRegion tooltipTopLeft;
    private final TextureRegion tooltipTopRight;
    private final TextureRegion tooltipBottomLeft;
    private final TextureRegion tooltipBottomRight;
    private final TextureRegion tooltipBorder7x1;
    private final TextureRegion tooltipBorder1x7;
    public final TextureRegion tooltipFiller;
    public final TextureRegion tooltipFillerLight;
    public final TextureRegion tooltipFillerCrafting;

    public final TextureRegion slotSelectionMarker;

    public final TextureRegion iconAttackDamage;
    public final TextureRegion iconHealthRestore;
    public final TextureRegion iconHungerRestore;
    public final TextureRegion iconHungerCooldownRestore;

    public final TextureRegion[] tooltipDescriptor;

    public UIContainer currentContainer = null;

    /** Singleton instance */
    private static PlayerUI INSTANCE;

    public PlayerUI() {
        invSlot = tr("inv_slot");
        invSlotS = tr("inv_slotS");

        hotbarBase = tr("hotbar_base");
        slotSelectionMarker = tr("selected_slot_indicator");

        TextureRegion tab = tr("tab");

        tabTopLeft = new TextureRegion(tab, 0, 0, 4, 4);
        tabTopRight = new TextureRegion(tab, 5, 0, 4, 4);
        tabBottomLeft = new TextureRegion(tab, 0, 5, 4, 4);
        tabBottomRight = new TextureRegion(tab, 5, 5, 4, 4);
        playerTabHead = tr("ui_minimap_player");
        tabPingIcon = new TextureRegion(tab, 0, 16, 4, 4);
        tabBorder3x1 = new TextureRegion(tab, 0, 14, 3, 1);
        tabBorder1x3 = new TextureRegion(tab, 0, 10, 1, 3);

        whiteSquare = tr("square16x16");

        playerMinimap = new PlayerMinimap(this, tr("ui_minimap"), tr("ui_minimap_arrow"), tr("ui_minimap_player"));

        glyphLayout = new GlyphLayout();

        notificationList = new LinkedList<>();

        // tooltip begin
        TextureRegion tooltip = tr("ui_tooltip");

        tooltipTopLeft = new TextureRegion(tooltip, 0, 16, 6, 6);
        tooltipTopRight = new TextureRegion(tooltip, 8, 16, 6, 6);
        tooltipBottomLeft = new TextureRegion(tooltip, 0, 8+16, 6, 6);
        tooltipBottomRight = new TextureRegion(tooltip, 8, 8+16, 6, 6);
        tooltipBorder1x7 = new TextureRegion(tooltip, 16, 16, 1, 4);
        tooltipBorder7x1 = new TextureRegion(tooltip, 18, 16, 4, 1);
        tooltipFiller = new TextureRegion(tooltip, 18, 2+16, 1, 1);
        tooltipFillerLight = new TextureRegion(tooltip, 20, 2+16, 1, 1);
        tooltipFillerCrafting = new TextureRegion(tooltip, 22, 2+16, 1, 1);
        // tooltip end

        TextureRegion desc = tr("tooltip_desc");
        tooltipDescriptor = new TextureRegion[7];
        tooltipDescriptor[0] = new TextureRegion(desc, 0, 0, 17, 5);
        tooltipDescriptor[1] = new TextureRegion(desc, 0, 6, 19, 5);
        tooltipDescriptor[2] = new TextureRegion(desc, 0, 12, 37, 5);
        tooltipDescriptor[3] = new TextureRegion(desc, 0, 18, 48, 5);
        tooltipDescriptor[4] = new TextureRegion(desc, 0, 24, 25, 5);
        tooltipDescriptor[5] = new TextureRegion(desc, 0, 30, 36, 5);
        tooltipDescriptor[6] = new TextureRegion(desc, 0, 36, 20, 5);

        TextureRegion icons = tr("icons");
        iconAttackDamage = new TextureRegion(icons, 0, 0, 9, 9);
        iconHungerRestore = new TextureRegion(icons, 10, 0, 9, 9);
        iconHungerCooldownRestore = new TextureRegion(icons, 20, 0, 9, 9);
        iconHealthRestore = new TextureRegion(icons, 30, 0, 9, 9);

        chat = new ExpoClientChat(this);

        INSTANCE = this;
        UIContainerInventory.PLAYER_INVENTORY_CONTAINER = new UIContainerInventory();

        hotbarSlots = new InteractableItemSlot[9];

        for(int i = 0; i < 9; i++) {
            hotbarSlots[i] = new InteractableItemSlot(ExpoShared.CONTAINER_ID_PLAYER, i);
            hotbarSlots[i].visible = true;
        }
        hotbarSlots[0].selected = true;

        changeUiScale();
    }

    public TextureRegion tr(String name) {
        return ExpoAssets.get().textureRegion(name);
    }

    public void drawTooltip(int x, int y, String text) {
        drawTooltipColored(x, y, text, Color.WHITE, null);
    }

    public void drawTooltip(String text) {
        drawTooltipColored(text, Color.WHITE);
    }

    public void drawTooltipColored(String text, Color color, TextureRegion[] icons, String[] lines) {
        RenderContext rc = RenderContext.get();
        drawTooltipColored((int) rc.mouseX, (int) rc.mouseY, text, color, icons, lines);
    }

    public void drawTooltipColored(String text, Color color, String... extraLines) {
        RenderContext rc = RenderContext.get();
        drawTooltipColored((int) rc.mouseX, (int) rc.mouseY, text, color, null, extraLines);
    }

    public void drawTooltipColored(int x, int y, String text, Color color, TextureRegion[] icons, String... extraLines) {
        RenderContext rc = RenderContext.get();
        x += (int) (4 * uiScale); // offset
        y += (int) (4 * uiScale); // offset

        glyphLayout.setText(rc.m5x7_use, text);
        float tw = glyphLayout.width;

        for(int i = 0; i < extraLines.length; i++) {
            String str = extraLines[i];
            glyphLayout.setText(rc.m5x7_use, str);

            float pl = glyphLayout.width;

            if(icons != null) {
                int spacing = 2;
                pl += icons[i].getRegionWidth() * uiScale + spacing * 3 * uiScale;
            }

            if(pl > tw) tw = pl;
        }

        float th = glyphLayout.height;
        float titleHeight = glyphLayout.height;

        if(extraLines.length > 0) {
            th += 9 * uiScale;

            for(int i = 0; i < extraLines.length; i++) {
                glyphLayout.setText(rc.m5x7_use, extraLines[i]);
                th += glyphLayout.height;
                if(i > 0) th += 4 * uiScale;
            }
        }

        float cornerSize = tooltipBottomLeft.getRegionWidth() * uiScale;
        float borderSize = 1 * uiScale * 2;
        tw += borderSize;
        th += borderSize;

        rc.hudBatch.draw(tooltipFiller, x + 4 * uiScale, y + 4 * uiScale, tw + 4 * uiScale, th + 4 * uiScale);

        drawBorderAt(rc, x, y, tw, th);

        rc.m5x7_use.setColor(color);
        rc.m5x7_use.draw(rc.hudBatch, text, x + cornerSize + uiScale, y + cornerSize + th - uiScale);
        rc.m5x7_use.setColor(Color.WHITE);

        if(extraLines.length > 0) {
            rc.hudBatch.draw(tooltipFillerLight, x + cornerSize + uiScale, y + cornerSize + th - uiScale - titleHeight - 5 * uiScale, tw, uiScale);

            for(int i = 0; i < extraLines.length; i++) {
                String c = extraLines[i];

                int txtX = (int) (x + cornerSize + uiScale);
                int txtY = (int) (y + cornerSize + th - uiScale - titleHeight - 9 * uiScale - i * (4 * uiScale + titleHeight));
                int bonusX = 0;

                if(icons != null && icons[i] != null) {
                    bonusX = (int) (6 * uiScale + icons[i].getRegionWidth() * uiScale);
                }

                rc.m5x7_use.draw(rc.hudBatch, c, txtX + bonusX, txtY);

                if(icons != null && icons[i] != null) {
                    rc.hudBatch.draw(icons[i], txtX + 2 * uiScale, txtY - titleHeight - uiScale, icons[i].getRegionWidth() * uiScale, icons[i].getRegionHeight() * uiScale);
                }
            }
        }
    }

    public void drawBorderAt(RenderContext r, int x, int y, float tw, float th) {
        float cornerSize = tooltipBottomLeft.getRegionWidth() * uiScale;

        r.hudBatch.draw(tooltipBottomLeft, x, y, cornerSize, cornerSize);
        r.hudBatch.draw(tooltipBottomRight, x + tw + cornerSize, y, cornerSize, cornerSize);
        r.hudBatch.draw(tooltipBorder1x7, x + cornerSize, y, tw, 4 * uiScale);

        r.hudBatch.draw(tooltipBorder7x1, x, y + cornerSize, 4 * uiScale, th);
        r.hudBatch.draw(tooltipBorder7x1, x + cornerSize + tw + (cornerSize - 4 * uiScale), y + cornerSize, 4 * uiScale, th);

        r.hudBatch.draw(tooltipTopLeft, x, y + cornerSize + th, cornerSize, cornerSize);
        r.hudBatch.draw(tooltipTopRight, x + tw + cornerSize, y + cornerSize + th, cornerSize, cornerSize);
        r.hudBatch.draw(tooltipBorder1x7, x + cornerSize, y + cornerSize + th + (cornerSize - 4 * uiScale), tw, 4 * uiScale);
    }

    public void updateHotbarPosition() {
        float startX = center(hotbarW);
        float startY = 2;

        for(int i = 0; i < hotbarSlots.length; i++) {
            hotbarSlots[i].update(startX + 4 * uiScale + (i * slotW + i * uiScale), startY + 4 * uiScale, slotW, slotH, uiScale, 0);
        }
    }

    public void hoverCheck(InteractableUIElement slot) {
        if(slot.visible && uiElementInBounds(slot)) {
            if(!slot.hovered) {
                slot.hovered = true;
                slot.onHoverBegin();
                hoveredSlot = slot;
            }
        } else {
            if(slot.hovered) {
                slot.hovered = false;
                slot.onHoverEnd();
                if(hoveredSlot != null && hoveredSlot.equals(slot)) {
                    hoveredSlot = null;
                }
            }
        }
    }

    public void updateInventoryElements() {
        for(InteractableItemSlot slot : hotbarSlots) {
            hoverCheck(slot);
        }
    }

    public void update() {
        RenderContext rc = RenderContext.get();

        rc.blurActive = currentContainer != null && currentContainer.visible;

        if(rc.mouseMoved) {
            updateInventoryElements();

            if(currentContainer != null && currentContainer.visible) {
                currentContainer.onMouseMove();
            }
        }

        playerMinimap.updateMinimap();

        if(fadeInDelta < fadeInDuration) {
            fadeInDelta += rc.delta;
            if(fadeInDelta > fadeInDuration) fadeInDelta = fadeInDuration;
        }

        fadeRainTarget = ExpoClientContainer.get().getClientWorld().worldWeather == Weather.RAIN.WEATHER_ID ? 1.0f : 0.0f;

        if(fadeRainDelta != fadeRainTarget) {
            if(fadeRainDelta < fadeRainTarget) {
                fadeRainDelta += rc.delta / fadeRainDuration;
                if(fadeRainDelta > fadeRainTarget) fadeRainDelta = fadeRainTarget;
            } else {
                fadeRainDelta -= rc.delta / fadeRainDuration;
                if(fadeRainDelta < fadeRainTarget) fadeRainDelta = fadeRainTarget;
            }
        }
    }

    public void setFade(float duration) {
        fadeInDelta = 0;
        fadeInDuration = duration;
    }

    private boolean uiElementInBounds(InteractableUIElement slot) {
        RenderContext rc = RenderContext.get();
        return rc.mouseX >= slot.x && rc.mouseX < slot.ex && rc.mouseY >= slot.y && rc.mouseY < slot.ey;
    }

    public void render() {
        RenderContext rc = RenderContext.get();

        rc.hudBatch.begin();

        {
            /*
            if(fadeRainDelta != 0) {
                Color COLOR_RAIN = ExpoClientContainer.get().getClientWorld().COLOR_RAIN;
                rc.hudBatch.setColor(COLOR_RAIN.r, COLOR_RAIN.g, COLOR_RAIN.b, 0.05f * fadeRainDelta);
                rc.hudBatch.draw(whiteSquare, -1, -1, Gdx.graphics.getWidth() + 2, Gdx.graphics.getHeight() + 2);
                rc.hudBatch.setColor(Color.WHITE);
            }
            */
        }

        if(PlayerInventory.LOCAL_INVENTORY == null) {
            rc.hudBatch.end();
            return;
        }

        // Draw player names
        if(DEV_MODE || ExpoServerBase.get() == null) {
            var players = ClientEntityManager.get().getEntitiesByType(ClientEntityType.PLAYER);

            for(ClientEntity entity : players) {
                ClientPlayer player = (ClientPlayer) entity;
                BitmapFont useFont = rc.m5x7_border_use;
                glyphLayout.setText(useFont, player.username);

                Vector2 hudPos = ClientUtils.entityPosToHudPos(player.clientPosX, player.clientPosY + 32);
                int usx = (int) (hudPos.x - glyphLayout.width * 0.5f);
                int usy = (int) (hudPos.y + glyphLayout.height);
                // useFont.draw(rc.hudBatch, player.username, usx, usy);

                var pair = ExpoClientChat.get().playerHistoryMap.get(player.username);

                if(pair != null) {
                    float totalDisplayTime = pair.value.key;
                    float remainingDisplayTime = pair.value.value;

                    if(remainingDisplayTime > 0) {
                        float alpha = 1.0f;
                        float FADE_IN_TIME = 0.125f;
                        float FADE_OUT_TIME = 0.125f;

                        float diff = totalDisplayTime - remainingDisplayTime; // 0->5

                        if(diff <= FADE_IN_TIME) { // diff <= 0.25
                            alpha = diff / FADE_IN_TIME;
                        } else if(diff >= (totalDisplayTime - FADE_OUT_TIME)) { // diff >= (4.75)
                            alpha = 1f - (diff - (totalDisplayTime - FADE_OUT_TIME)) / FADE_OUT_TIME;
                        }

                        BitmapFont useChatDisplayFont = ExpoClientChat.get().chatUseFont;//rc.m5x7_border_use;
                        glyphLayout.setText(useChatDisplayFont, pair.key);
                        float cmx = (hudPos.x - glyphLayout.width * 0.5f);
                        float cmy = (usy + glyphLayout.height + 6 * uiScale);
                        int padding = 2;
                        float MAX_ALPHA = 0.4f;

                        rc.hudBatch.setColor(0.0f, 0.0f, 0.0f, alpha * MAX_ALPHA);
                        rc.drawSquareRounded((int) (cmx - padding * uiScale),
                                (int) (cmy - padding * uiScale - glyphLayout.height),
                                (int) (glyphLayout.width + padding * 2 * uiScale), (int) (glyphLayout.height + padding * 2 * uiScale));
                        rc.hudBatch.setColor(Color.WHITE);

                        useChatDisplayFont.setColor(1.0f, 1.0f, 1.0f, alpha);
                        useChatDisplayFont.draw(rc.hudBatch, pair.key, (int) cmx, (int) cmy);
                        useChatDisplayFont.setColor(Color.WHITE);

                        pair.value.value -= rc.delta;
                    }
                }
            }
        }

        glyphLayout.setText(rc.m5x7_border_use, "U");

        if(currentContainer != null && currentContainer.visible) {
            currentContainer.draw(rc, this);
        } else {
            drawHotbar(rc);
        }

        playerMinimap.draw(rc);

        if(hoveredSlot != null && PlayerInventory.LOCAL_INVENTORY.cursorItem == null) {
            hoveredSlot.onTooltip();
        }

        synchronized (notificationLock) {
            if(!notificationList.isEmpty()) {
                float offsetY = 0;

                for(UINotification not : notificationList) {
                    if(!not.playedSound && not.sound != null) {
                        not.playedSound = true;
                        AudioEngine.get().playSoundGroup(not.sound);
                    }

                    not.delta += rc.delta;
                    if(not.delta > not.lifetime) not.delta = not.lifetime;

                    float iconWidth = not.icon.getRegionWidth() * uiScale;
                    float iconHeight = not.icon.getRegionHeight() * uiScale;

                    float fullFontWidth = 0;
                    float fullFontHeight = 0;

                    BitmapFont useFont = rc.pickupFont;
                    useFont.getData().setScale(uiScale * 0.5f);

                    for(var piece : not.pieces) {
                        rc.globalGlyph.setText(useFont, piece.text);
                        fullFontWidth += rc.globalGlyph.width;

                        if(rc.globalGlyph.height > fullFontHeight) {
                            fullFontHeight = rc.globalGlyph.height;
                        }
                    }

                    int iconSpacing = 4;
                    float totalWidth = iconWidth + iconSpacing * uiScale + fullFontWidth;
                    float totalHeight = iconHeight;
                    if(fullFontHeight > totalHeight) totalHeight = fullFontHeight;

                    float cx = (Gdx.graphics.getWidth() - totalWidth) * 0.5f;
                    float cy = (Gdx.graphics.getHeight() - totalHeight) - 16 * uiScale;

                    float X_SHIFT_THRESHOLD = 0.2f;
                    float xAmount = 64f * uiScale;
                    if(not.delta <= X_SHIFT_THRESHOLD) {
                        float interpolated = Interpolation.exp10Out.apply(not.delta / X_SHIFT_THRESHOLD);
                        cx -= xAmount;
                        cx += xAmount * interpolated;
                    }

                    float ALPHA_THRESHOLD = 0.5f;
                    float alpha = 1.0f;
                    if(not.delta <= ALPHA_THRESHOLD) {
                        alpha = Interpolation.fade.apply(not.delta / ALPHA_THRESHOLD);
                    } else if(not.delta >= (not.lifetime - ALPHA_THRESHOLD)) {
                        alpha = Interpolation.fade.apply((not.lifetime - not.delta) / ALPHA_THRESHOLD);
                    }

                    rc.hudBatch.setColor(0.0f, 0.0f, 0.0f, 0.25f * alpha);
                    int spacing = 4;
                    rc.drawSquareRoundedDouble(cx - spacing * uiScale, cy - spacing * uiScale - offsetY, totalWidth + spacing * uiScale * 2, totalHeight + spacing * uiScale * 2);

                    rc.hudBatch.setColor(1.0f, 1.0f, 1.0f, alpha);

                    rc.hudBatch.draw(not.icon, cx, cy - offsetY, iconWidth, iconHeight);

                    float offsetText = 0;
                    for(var piece : not.pieces) {
                        rc.globalGlyph.setText(useFont, piece.text);
                        piece.color.a = alpha;

                        Color col = new Color(piece.color.r, piece.color.g, piece.color.b, alpha);

                        GradientFont.drawGradient(useFont, rc.hudBatch, piece.text,
                                cx + iconWidth + iconSpacing * uiScale + offsetText,
                                cy + rc.globalGlyph.height + (totalHeight - rc.globalGlyph.height) * 0.5f - offsetY, col);

                        offsetText += rc.globalGlyph.width;
                    }

                    offsetY += totalHeight + spacing * 2 * uiScale + 2 * uiScale;
                    useFont.getData().setScale(1.0f);
                }

                rc.hudBatch.setColor(Color.WHITE);
                rc.m5x7_border_use.setColor(Color.WHITE);
                notificationList.removeIf(x -> x.delta >= x.lifetime);
            }
        }

        if(tablistOpen) {
            drawTablist(rc);
        }

        if(PlayerInventory.LOCAL_INVENTORY.cursorItem != null) {
            drawCursor(PlayerInventory.LOCAL_INVENTORY.cursorItem);
        }

        /*
        if(Expo.get().getImGuiExpo().entityBrainStates.get() && ServerWorld.get() != null) {
            var crabs = ServerWorld.get().getMainDimension().getEntityManager().getEntitiesOf(ServerEntityType.CRAB);

            for(ServerEntity se : crabs) {
                ServerCrab crab = (ServerCrab) se;

                Vector2 drawPos = ClientUtils.entityPosToHudPos(crab.posX, crab.posY + 14);

                rc.m5x7_border_use.draw(rc.hudBatch, crab.brain.getActiveModule(), drawPos.x, drawPos.y);
            }
        }
        */

        /*
        if(ClientEntityManager.get().selectedEntity != null && currentContainer == null) {
            var e = ClientEntityManager.get().selectedEntity;
            var type = e.getEntityType();

            TextureRegion indicator = null;
            int item = ClientPlayer.getLocalPlayer().holdingItemId;
            ItemMapping mapping = item != -1 ? ItemMapper.get().getMapping(item) : null;

            if(type == ClientEntityType.BOULDER) {
                if(mapping != null && mapping.logic.isTool(ToolType.PICKAXE)) {
                    indicator = tr("ui_indicator_pickaxe");
                } else {
                    indicator = tr("ui_indicator_pickaxe_bad");
                }
            } else if(type == ClientEntityType.OAK_TREE) {
                if(mapping != null && mapping.logic.isTool(ToolType.AXE)) {
                    indicator = tr("ui_indicator_axe");
                } else {
                    indicator = tr("ui_indicator_axe_bad");
                }
            }

            if(indicator != null) {
                rc.hudBatch.draw(indicator, (int) rc.mouseX + 21, (int) rc.mouseY - 49, indicator.getRegionWidth() * uiScale, indicator.getRegionHeight() * uiScale);
            }


            String t = e.getEntityType().ENTITY_NAME;

            glyphLayout.setText(rc.m5x7_border_use, t);
            Vector2 pos = ClientUtils.entityPosToHudPos(e.clientPosX, e.clientPosY + e.textureHeight);

            rc.m5x7_border_use.draw(rc.hudBatch, t, pos.x - glyphLayout.width * 0.5f, pos.y + glyphLayout.height);

        }
        */

        rc.hudBatch.end();

        if(DEV_MODE || ExpoServerBase.get() == null) {
            chat.draw();
        }

        {
            // World enter animation black fade out
            if(fadeInDelta != fadeInDuration) {
                rc.hudBatch.begin();
                rc.hudBatch.setColor(0.0f, 0.0f, 0.0f, 1.0f - fadeInDelta / fadeInDuration);
                rc.hudBatch.draw(whiteSquare, -1, -1, Gdx.graphics.getWidth() + 2, Gdx.graphics.getHeight() + 2);
                rc.hudBatch.setColor(Color.WHITE);
                rc.hudBatch.end();
            }
        }
    }

    public void addNotification(float lifetime, String text) {
        synchronized (notificationLock) {
            UINotification notification = new UINotification();
            notification.icon = ItemMapper.get().getMapping("item_maggot").uiRender[0].useTextureRegion;
            notification.lifetime = lifetime;
            notification.sound = "notification";
            notification.addPiece(text, Color.WHITE);
            notificationList.add(notification);
        }
    }

    public void addNotification(TextureRegion icon, float lifetime, String sound, String text) {
        synchronized (notificationLock) {
            UINotification notification = new UINotification();
            notification.icon = icon;
            notification.lifetime = lifetime;
            notification.sound = sound;
            notification.addPiece(text, Color.WHITE);
            notificationList.add(notification);
        }
    }

    public void addNotification(TextureRegion icon, float lifetime, String sound, UINotificationPiece[] pieces) {
        synchronized (notificationLock) {
            UINotification notification = new UINotification();
            notification.icon = icon;
            notification.lifetime = lifetime;
            notification.sound = sound;
            notification.addPieces(pieces);
            notificationList.add(notification);
        }
    }

    public void drawSlots(ClientInventorySlot[] itemSlots, InteractableItemSlot[] uiSlots) {
        for(InteractableItemSlot slot : uiSlots) {
            slot.drawBase();
        }

        for(int i = 0; i < uiSlots.length; i++) {
            uiSlots[i].drawContents(itemSlots[i]);
        }

        if(DEV_MODE && Expo.get().getImGuiExpo().shouldDrawSlotIndices()) {
            for(InteractableItemSlot uiSlot : uiSlots) {
                uiSlot.drawSlotIndices();
            }
        }
    }

    public void drawHotbarSlots() {
        for(InteractableItemSlot hotbarSlot : hotbarSlots) {
            hotbarSlot.drawBase();
        }

        for(int i = 0; i < hotbarSlots.length; i++) {
            hotbarSlots[i].drawContents(ClientPlayer.getLocalPlayer().playerInventory.getSlotAt(i));
        }

        if(DEV_MODE && Expo.get().getImGuiExpo().shouldDrawSlotIndices()) {
            for(InteractableItemSlot hotbarSlot : hotbarSlots) {
                hotbarSlot.drawBase();
            }
        }
    }

    public void drawCursor(ClientInventoryItem item) {
        ItemMapping mapping = ItemMapper.get().getMapping(item.itemId);
        RenderContext rc = RenderContext.get();

        if(mapping.logic.maxStackSize > 1) {
            rc.drawItemTexturesWithNumber(mapping.uiRender, rc.mouseX, rc.mouseY, 0, 0, item.itemAmount);
        } else {
            rc.drawItemTextures(mapping.uiRender, rc.mouseX, rc.mouseY, 0, 0);
        }

        if(mapping.logic.durability != -1) {
            // Has durability.
            boolean drawDurability = mapping.logic.durability > item.itemMetadata.durability;

            if(drawDurability) {
                float percentage = (float) item.itemMetadata.durability / mapping.logic.durability;
                int space = 5;
                int thickness = 1;
                float yOffset = 4 * uiScale;
                float fullW = slotW - space * uiScale * 2;

                int dbx = (int) (rc.mouseX - slotW * 0.5f + space * uiScale);
                int dby = (int) (rc.mouseY - slotH * 0.5f + yOffset);

                rc.hudBatch.setColor(26f / 255f, 16f / 255f, 16f / 255f, 1.0f);
                rc.hudBatch.draw(whiteSquare, dbx - 1, dby, fullW + 2, thickness * uiScale);
                rc.hudBatch.draw(whiteSquare, dbx, dby - 1, fullW, thickness * uiScale + 2);
                rc.hudBatch.setColor(39f / 255f, 24f / 255f, 24f / 255f, 1.0f);
                rc.hudBatch.draw(whiteSquare, dbx, dby, fullW, thickness * uiScale);

                if(percentage > 0.66f) {
                    rc.hudBatch.setColor(35f / 255f, 187f / 255f, 67f / 255f, 1f);
                } else if(percentage > 0.33f) {
                    rc.hudBatch.setColor(230f / 255f, 230f / 255f, 21f / 255f, 1f);
                } else {
                    rc.hudBatch.setColor(238f / 255f, 26f / 255f, 26f / 255f, 1f);
                }

                float percToPx = fullW * percentage;
                rc.hudBatch.draw(whiteSquare, dbx, dby, percToPx, thickness * uiScale);

                rc.hudBatch.setColor(Color.WHITE);
            }
        }
    }

    private void drawTablist(RenderContext rc) {
        var map = ExpoClientContainer.get().getPlayerOnlineList();
        float muW = 0, muH = 0; // Max Username width + height
        float mpW = 0; // Max ping width + height
        int lines = map.size();

        // Determine max width + height per line
        for(String username : map.keySet()) {
            glyphLayout.setText(rc.m5x7_shadow_use, username);
            if(glyphLayout.width > muW) muW = glyphLayout.width;
            if(glyphLayout.height > muH) muH = glyphLayout.height;

            glyphLayout.setText(rc.m5x7_shadow_use, "999+"); // max ping to display = 999
            if(glyphLayout.width > mpW) mpW = glyphLayout.width;
        }

        float rowWidth, rowHeight, totalWidth, totalHeight;

        rowWidth = pthW + 2 * uiScale // player head picture tab box
                + 2 * uiScale // between player head & name
                + muW + 4 * uiScale // player name box
                + 2 * uiScale // between player name & ping
                + 2 * uiScale + pingW + 2 * uiScale + mpW + 2 * uiScale; // player ping box

        if(pthH > muH) {
            rowHeight = pthH + 2 * uiScale;
        } else {
            rowHeight = muH + 4 * uiScale;
        }

        totalWidth = (borderWH * 2) + 4 * uiScale + rowWidth;
        totalHeight = (borderWH * 2) + 4 * uiScale + rowHeight * lines + 2 * uiScale * (lines - 1);

        float startDrawX, startDrawY;

        startDrawX = center(totalWidth);
        startDrawY = Gdx.graphics.getHeight() - 16 - totalHeight;

        // Base background
        rc.hudBatch.setColor(64f / 255f, 64f / 255f, 64f / 255f, 1.0f);
        rc.hudBatch.draw(whiteSquare, startDrawX + 1 * uiScale, startDrawY + 1 * uiScale, totalWidth - 2 * uiScale, totalHeight - 2 * uiScale);
        rc.hudBatch.setColor(Color.WHITE);

        // Bottom row
        rc.hudBatch.draw(tabBottomLeft, startDrawX, startDrawY, cornerWH, cornerWH);
        rc.hudBatch.draw(tabBottomRight, startDrawX + totalWidth - cornerWH, startDrawY, cornerWH, cornerWH);
        rc.hudBatch.draw(tabBorder1x3, startDrawX + cornerWH, startDrawY, totalWidth - cornerWH * 2, borderWH);

        // Top row
        rc.hudBatch.draw(tabTopLeft, startDrawX, startDrawY + totalHeight - cornerWH, cornerWH, cornerWH);
        rc.hudBatch.draw(tabTopRight, startDrawX + totalWidth - cornerWH, startDrawY + totalHeight - cornerWH, cornerWH, cornerWH);
        rc.hudBatch.draw(tabBorder1x3, startDrawX + cornerWH, startDrawY + totalHeight - borderWH, totalWidth - cornerWH * 2, borderWH);

        // Sides
        rc.hudBatch.draw(tabBorder3x1, startDrawX, startDrawY + cornerWH, borderWH, totalHeight - cornerWH * 2);
        rc.hudBatch.draw(tabBorder3x1, startDrawX + totalWidth - borderWH, startDrawY + cornerWH, borderWH, totalHeight - cornerWH * 2);

        // For each element
        float rowOffsetX = borderWH + 2 * uiScale;
        float rowOffsetY = borderWH + 2 * uiScale;

        for(String username : map.keySet()) {
            float _x = startDrawX + rowOffsetX;
            float _w = pthW + 2 * uiScale;

            rc.hudBatch.setColor(43f / 255f, 43f / 255f, 43f / 255f, 1.0f);
            rc.hudBatch.draw(whiteSquare, _x, startDrawY + rowOffsetY, _w, rowHeight);

            _x += _w + 2 * uiScale;
            _w = muW + 4 * uiScale;
            rc.hudBatch.draw(whiteSquare, _x, startDrawY + rowOffsetY, _w, rowHeight);

            _x += _w + 2 * uiScale;
            _w = mpW + 6 * uiScale + pingW;
            rc.hudBatch.draw(whiteSquare, _x, startDrawY + rowOffsetY, _w, rowHeight);

            rc.hudBatch.setColor(Color.WHITE);
            rc.hudBatch.draw(playerTabHead, startDrawX + rowOffsetX + 1 * uiScale, startDrawY + rowOffsetY + 1 * uiScale, pthW, pthH);
            rc.m5x7_shadow_use.draw(rc.hudBatch, username, startDrawX + rowOffsetX + pthW + 2 * uiScale + 2 * uiScale + 2 * uiScale, startDrawY + rowOffsetY + 2 * uiScale + muH + 1 * uiScale);

            long ping = map.get(username);

            if(ping < 80) {
                rc.m5x7_shadow_use.setColor(COLOR_GREEN);
                rc.hudBatch.setColor(COLOR_GREEN);
            } else if(ping < 200) {
                rc.m5x7_shadow_use.setColor(COLOR_YELLOW);
                rc.hudBatch.setColor(COLOR_YELLOW);
            } else {
                rc.m5x7_shadow_use.setColor(COLOR_RED);
                rc.hudBatch.setColor(COLOR_RED);
            }

            String pingAsString = (ping > 999 ? "999+" : String.valueOf(ping));
            glyphLayout.setText(rc.m5x7_shadow_use, pingAsString);

            float pingBoxWidth = pingW + 6 * uiScale + mpW;
            float pingIconX = _x + (pingBoxWidth - (pingW + 2 * uiScale + glyphLayout.width)) * 0.5f;
            rc.hudBatch.draw(tabPingIcon, pingIconX, startDrawY + rowOffsetY + (rowHeight - pingH) * 0.5f, pingW, pingH);
            rc.m5x7_shadow_use.draw(rc.hudBatch, pingAsString, pingIconX + 2 * uiScale + pingW, startDrawY + rowOffsetY + 2 * uiScale + muH + 1 * uiScale);

            rc.m5x7_shadow_use.setColor(Color.WHITE);
            rc.hudBatch.setColor(Color.WHITE);

            rowOffsetY += rowHeight + 2 * uiScale;
        }
    }

    private void drawHotbar(RenderContext r) {
        float startX = center(hotbarW);
        float startY = 2;

        // Hotbar base
        r.hudBatch.draw(hotbarBase, startX, startY, hotbarW, hotbarH);

        // Hotbar slots
        drawHotbarSlots();

        // Health status
        status(r, ClientPlayer.getLocalPlayer().playerHealth, startX + 23 * uiScale, startY + 38 * uiScale);

        // Hunger status
        status(r, ClientPlayer.getLocalPlayer().playerHunger, startX + 196 * uiScale, startY + 38 * uiScale);

        // Current item
        var local = PlayerInventory.LOCAL_INVENTORY.currentItem();
        itemText(r, local == null ? null : local.toMapping(), startX + 57 * uiScale, startY + 38 * uiScale);
    }

    private void itemText(RenderContext rc, ItemMapping mapping, float x, float y) {
        String text;
        Color c;

        if(mapping == null) {
            text = Lang.str("item.none");
            c = Color.LIGHT_GRAY;
        } else {
            text = mapping.displayName;
            c = mapping.color;
        }

        glyphLayout.setText(rc.m5x7_border_use, text);
        rc.m5x7_border_use.setColor(c);
        rc.m5x7_border_use.draw(rc.hudBatch, text, x + (137 * uiScale - glyphLayout.width) * 0.5f, y + glyphLayout.height + (11 * uiScale - glyphLayout.height) * 0.5f);
        rc.m5x7_border_use.setColor(Color.WHITE);
    }

    public float[] percentageToColor(float percentage) {
        float r, g, b;

        if(percentage > COLOR_GRADIENTS[1]) {
            r = COLOR_GREEN.r;
            g = COLOR_GREEN.g;
            b = COLOR_GREEN.b;
        } else if(percentage > COLOR_GRADIENTS[2]) {
            float d = COLOR_GRADIENTS[1] - COLOR_GRADIENTS[2]; // =20
            float s = COLOR_GRADIENTS[1] - percentage; // 0->20
            float n = s / d;

            float rD = COLOR_YELLOW.r - COLOR_GREEN.r;
            r = COLOR_GREEN.r + rD * n;
            float gD = COLOR_YELLOW.g - COLOR_GREEN.g;
            g = COLOR_GREEN.g + gD * n;
            float bD = COLOR_YELLOW.b - COLOR_GREEN.b;
            b = COLOR_GREEN.b + bD * n;
        } else if(percentage > COLOR_GRADIENTS[3]) {
            r = COLOR_YELLOW.r;
            g = COLOR_YELLOW.g;
            b = COLOR_YELLOW.b;
        } else if(percentage > COLOR_GRADIENTS[4]) {
            float d = COLOR_GRADIENTS[3] - COLOR_GRADIENTS[4]; // =20
            float s = COLOR_GRADIENTS[3] - percentage; // 0->20
            float n = s / d;

            float rD = COLOR_YELLOW.r - COLOR_RED.r;
            r = COLOR_YELLOW.r - rD * n;
            float gD = COLOR_YELLOW.g - COLOR_RED.g;
            g = COLOR_YELLOW.g - gD * n;
            float bD = COLOR_YELLOW.b - COLOR_RED.b;
            b = COLOR_YELLOW.b - bD * n;
        } else {
            r = COLOR_RED.r;
            g = COLOR_RED.g;
            b = COLOR_RED.b;
        }

        return new float[] {r, g, b};
    }

    private void status(RenderContext rc, float status, float x, float y) {
        float[] rgb = percentageToColor(status);

        String text = (int) status + "%";
        glyphLayout.setText(rc.m5x7_border_use, text);

        rc.m5x7_border_use.setColor(rgb[0], rgb[1], rgb[2], 1.0f);
        rc.m5x7_border_use.draw(rc.hudBatch, text, x + (32 * uiScale - glyphLayout.width) * 0.5f, y + glyphLayout.height + (11 * uiScale - glyphLayout.height) * 0.5f);
    }

    public void togglePlayerInventoryView() {
        if(currentContainer != null) {
            closeInventoryView();
            ClientPackets.p41InventoryViewQuit();
        } else {
            openPlayerInventoryView();
        }
    }

    public void closeInventoryView() {
        if(currentContainer != null) {
            currentContainer.visible = false;
            currentContainer.onHide();
            currentContainer = null;
        }
    }

    public void openPlayerInventoryView() {
        if(currentContainer != null) {
            currentContainer.visible = false;
            currentContainer.onHide();
        }

        currentContainer = UIContainerInventory.PLAYER_INVENTORY_CONTAINER;
        currentContainer.visible = true;
        currentContainer.onShow();
    }

    public void openContainerView(InventoryViewType type, int containerId, ServerInventorySlot[] serverSlots) {
        if(currentContainer != null) {
            currentContainer.visible = false;
            currentContainer.onHide();
        }

        currentContainer = UIContainer.fromType(type, containerId, serverSlots);
        currentContainer.visible = true;
        currentContainer.onShow();
    }

    public void changeUiScale() {
        uiWidth = Gdx.graphics.getWidth();
        uiHeight = Gdx.graphics.getHeight();
        uiScale = GameSettings.get().uiScale;

        pthW = playerTabHead.getRegionWidth() * uiScale;
        pthH = playerTabHead.getRegionHeight() * uiScale;
        cornerWH = tabTopLeft.getRegionWidth() * uiScale;
        pingW = tabPingIcon.getRegionWidth() * uiScale;
        pingH = tabPingIcon.getRegionHeight() * uiScale;
        borderWH = tabBorder3x1.getRegionWidth() * uiScale;

        hotbarW = hotbarBase.getRegionWidth() * uiScale;
        hotbarH = hotbarBase.getRegionHeight() * uiScale;

        slotW = invSlot.getRegionWidth() * uiScale;
        slotH = invSlot.getRegionHeight() * uiScale;

        playerMinimap.updateWH(uiScale);

        if(currentContainer != null) {
            currentContainer.updatePosition(RenderContext.get(), this);
        } else {
            updateHotbarPosition();
        }

        int lines = 12;
        int total = 19 + lines * 11;

        float _uiScale = uiScale - 1;
        if(_uiScale <= 0) _uiScale = 1;

        chat.resize((int) (180 * (_uiScale + 1)), (int) (total * (_uiScale)), _uiScale);
    }

    public void onResize() {
        uiWidth = Gdx.graphics.getWidth();
        uiHeight = Gdx.graphics.getHeight();

        if(currentContainer != null) {
            currentContainer.updatePosition(RenderContext.get(), this);
        } else {
            updateHotbarPosition();
        }

        chat.readjust();
    }

    public static PlayerUI get() {
        return INSTANCE;
    }

    private float center(float w) {
        return (uiWidth - w) * 0.5f;
    }

    private float centerY(float h) {
        return (uiHeight - h) * 0.5f;
    }

    public void toggleTablist() {
        tablistOpen = !tablistOpen;
    }

    public void onPlayerJoin(String username) {
        if(ClientPlayer.getLocalPlayer() == null) return;
        boolean notPlayer = !ClientPlayer.getLocalPlayer().username.equals(username);

        if((Expo.get().isMultiplayer() || DEV_MODE) && notPlayer) {
            String msg = Lang.str("chat.player.join", username);
            chat.addServerMessage(msg);
            addNotification(playerTabHead, 5.0f, "notification", msg);

            /*
            addNotification(playerTabHead, 5.0f, "notification", new UINotificationPiece[] {
                    new UINotificationPiece(username, Color.YELLOW),
                    new UINotificationPiece(" joined the server.")
            });
            */
        }
    }

    public void onPlayerQuit(String username) {
        if(ClientPlayer.getLocalPlayer() == null) return;
        boolean notPlayer = !ClientPlayer.getLocalPlayer().username.equals(username);

        if((Expo.get().isMultiplayer() || DEV_MODE) && notPlayer) {
            String msg = Lang.str("chat.player.quit", username);
            chat.addServerMessage(msg);
            addNotification(playerTabHead, 5.0f, "notification", msg);

            /*
            addNotification(playerTabHead, 5.0f, "notification", new UINotificationPiece[] {
                    new UINotificationPiece(username, Color.YELLOW),
                    new UINotificationPiece(" left the server.")
            });
            */
        }
    }

}