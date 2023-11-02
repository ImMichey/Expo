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
import dev.michey.expo.client.chat.ExpoClientChat;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.ClientInventorySlot;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.container.UIContainer;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.inventory.InventoryViewType;
import dev.michey.expo.server.main.logic.inventory.ServerInventorySlot;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ClientUtils;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.weather.Weather;

import java.util.LinkedList;
import java.util.List;

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

    /** Hotbar */
    private float hotbarW, hotbarH;
    public float slotW, slotH;
    public final Color COLOR_GREEN = new Color(127f / 255f, 237f / 255f, 51f / 255f, 1.0f);
    public final String COLOR_GREEN_HEX = COLOR_GREEN.toString();
    private final Color COLOR_YELLOW  = new Color(251f / 255f, 242f / 255f, 54f / 255f, 1.0f);
    private final Color COLOR_RED = new Color(210f / 255f, 27f / 255f, 27f / 255f, 1.0f);
    public final String COLOR_DESCRIPTOR_HEX = "[#5875b0]";
    public final String COLOR_DESCRIPTOR2_HEX = "[#bdc4d4]";
    private final float[] COLOR_GRADIENTS = new float[] {100f, 80f, 60f, 40f, 20f, 0f};

    public InteractableItemSlot[] hotbarSlots;
    public InteractableUIElement hoveredSlot = null;

    public List<PickupLine> pickupLines;
    public final Object PICKUP_LOCK = new Object();

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

    private final TextureRegion playerTabHead;

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

    public UIContainer currentContainer = null;

    /** Singleton instance */
    private static PlayerUI INSTANCE;

    public PlayerUI() {
        invSlot = tr("inv_slot");
        invSlotS = tr("inv_slotS");

        hotbarBase = tr("hotbar_base");

        TextureRegion tab = tr("tab");

        tabTopLeft = new TextureRegion(tab, 0, 0, 4, 4);
        tabTopRight = new TextureRegion(tab, 5, 0, 4, 4);
        tabBottomLeft = new TextureRegion(tab, 0, 5, 4, 4);
        tabBottomRight = new TextureRegion(tab, 5, 5, 4, 4);
        playerTabHead = new TextureRegion(tab, 0, 21, 9, 10);
        tabPingIcon = new TextureRegion(tab, 0, 16, 4, 4);
        tabBorder3x1 = new TextureRegion(tab, 0, 14, 3, 1);
        tabBorder1x3 = new TextureRegion(tab, 0, 10, 1, 3);

        whiteSquare = tr("square16x16");

        playerMinimap = new PlayerMinimap(this, tr("ui_minimap"), tr("ui_minimap_arrow"), tr("ui_minimap_player"));
        pickupLines = new LinkedList<>();

        glyphLayout = new GlyphLayout();

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

    public void addPickupLine(int itemId, int itemAmount) {
        synchronized(PICKUP_LOCK) {
            for(PickupLine pl : pickupLines) {
                if(pl.itemId == itemId) {
                    pickupLines.remove(pl);
                    pl.lifetime = 0;
                    pl.itemAmount += itemAmount;
                    pickupLines.add(pl);
                    return;
                }
            }

            pickupLines.add(new PickupLine(itemId, itemAmount));
        }
    }

    public TextureRegion tr(String name) {
        return ExpoAssets.get().textureRegion(name);
    }

    public void drawTooltip(int x, int y, String text) {
        drawTooltipColored(x, y, text, Color.WHITE);
    }

    public void drawTooltip(String text) {
        drawTooltipColored(text, Color.WHITE);
    }

    public void drawTooltipColored(String text, Color color, String... extraLines) {
        RenderContext rc = RenderContext.get();
        drawTooltipColored((int) rc.mouseX, (int) rc.mouseY, text, color, extraLines);
    }

    public void drawTooltipColored(int x, int y, String text, Color color, String... extraLines) {
        RenderContext rc = RenderContext.get();
        x += (int) (4 * uiScale); // offset
        y += (int) (4 * uiScale); // offset

        glyphLayout.setText(rc.m5x7_use, text);
        float tw = glyphLayout.width;

        for(String str : extraLines) {
            glyphLayout.setText(rc.m5x7_use, str);
            if(glyphLayout.width > tw) tw = glyphLayout.width;
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
                rc.m5x7_use.draw(rc.hudBatch, c, x + cornerSize + uiScale, y + cornerSize + th - uiScale - titleHeight - 9 * uiScale - i * (4 * uiScale + titleHeight));
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
                useFont.draw(rc.hudBatch, player.username, usx, usy);

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
                        rc.hudBatch.draw(whiteSquare, (int) (cmx - padding * uiScale), (int) (cmy - padding * uiScale - glyphLayout.height), glyphLayout.width + padding * 2 * uiScale, glyphLayout.height + padding * 2 * uiScale);
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
        Vector2 startHudPos = ClientUtils.entityPosToHudPos(ClientPlayer.getLocalPlayer().clientPosX, ClientPlayer.getLocalPlayer().clientPosY + 32 + glyphLayout.height);
        float MAX_LINE_LIFETIME = 1.5f;

        // Draw pickup lines
        BitmapFont useFont = rc.m5x7_border_use;

        synchronized(PICKUP_LOCK) {
            int yOffset = 0;

            for(int i = 0; i < pickupLines.size(); i++) {
                PickupLine line = pickupLines.get(pickupLines.size() - 1 - i);
                line.lifetime += rc.delta;
                if(line.lifetime >= MAX_LINE_LIFETIME) continue;

                float alpha = Interpolation.circleOut.apply(line.lifetime / MAX_LINE_LIFETIME);

                ItemMapping mapping = ItemMapper.get().getMapping(line.itemId);
                String displayText = line.itemAmount + "x " + mapping.displayName;
                glyphLayout.setText(useFont, displayText);

                float fullWidth = mapping.uiRender[0].useWidth * uiScale + 4 * uiScale + glyphLayout.width;
                rc.hudBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f - line.lifetime / MAX_LINE_LIFETIME);
                useFont.setColor(mapping.color.r, mapping.color.g, mapping.color.b, 1.0f - line.lifetime / MAX_LINE_LIFETIME);

                float startX = startHudPos.x - fullWidth * 0.5f;
                float startY = startHudPos.y + alpha * 48 + yOffset;

                for(ItemRender ir : mapping.uiRender) {
                    rc.hudBatch.draw(ir.useTextureRegion, startX + ir.offsetX * uiScale, startY + ir.offsetY * uiScale - (ir.useHeight - glyphLayout.height) * 0.5f,
                            ir.useTextureRegion.getRegionWidth() * uiScale, ir.useTextureRegion.getRegionHeight() * uiScale);

                    useFont.draw(rc.hudBatch, displayText, startX + mapping.uiRender[0].useWidth * uiScale + 4 * uiScale, startY + glyphLayout.height);
                }

                yOffset += (int) (Math.max(glyphLayout.height, mapping.uiRender[0].useHeight * uiScale) + 4 * uiScale);
            }

            rc.hudBatch.setColor(Color.WHITE);
            useFont.setColor(Color.WHITE);
            pickupLines.removeIf(line -> line.lifetime >= MAX_LINE_LIFETIME);
        }

        if(currentContainer != null && currentContainer.visible) {
            currentContainer.draw(rc, this);
        } else {
            drawHotbar(rc);
            playerMinimap.draw(rc);
        }

        if(hoveredSlot != null && PlayerInventory.LOCAL_INVENTORY.cursorItem == null) {
            hoveredSlot.onTooltip();
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
            text = "Hand";
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
        chat.addServerMessage(username + " joined the server.");
    }

    public void onPlayerQuit(String username) {
        chat.addServerMessage(username + " left the server.");
    }

}