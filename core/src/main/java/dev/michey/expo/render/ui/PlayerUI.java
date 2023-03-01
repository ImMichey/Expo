package dev.michey.expo.render.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.ClientPlayer;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.imgui.ImGuiExpo;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.client.ItemRender;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.log.ExpoLogger.log;

public class PlayerUI {

    /** The scale for all UI elements (will be changeable in the options). */
    public float uiScale; // default = 2.0f
    private float uiWidth, uiHeight;
    public BitmapFont m5x7_use, m6x11_use, m5x7_border_use, m6x11_border_use, m5x7_shadow_use;

    /** Tab list */
    public boolean tablistOpen = false;
    private float pthW, pthH;
    private float pingW, pingH;
    private float cornerWH, borderWH;
    public GlyphLayout glyphLayout;

    /** Hotbar */
    private float hotbarW, hotbarH;
    public float slotW, slotH;
    private float healthW, healthH;
    private float hungerW, hungerH;
    private final Color COLOR_GREEN = new Color(127f / 255f, 237f / 255f, 51f / 255f, 1.0f);
    private final Color COLOR_YELLOW  = new Color(251f / 255f, 242f / 255f, 54f / 255f, 1.0f);
    private final Color COLOR_RED = new Color(210f / 255f, 27f / 255f, 27f / 255f, 1.0f);
    private final float[] COLOR_GRADIENTS = new float[] {100f, 80f, 60f, 40f, 20f, 0f};
    public InteractableItemSlot[] hotbarSlots;
    public InteractableItemSlot hoveredSlot = null;

    /** Inventory */
    private float invW, invH;
    private float invX, invY;
    public InteractableItemSlot[] inventorySlots;
    public InteractableItemSlot[] inventoryArmorSlots;

    /** Textures */
    public final TextureRegion invSlot;             // Regular Item Slot
    public final TextureRegion invSlotS;            // Regular Item Slot (hovered)

    public final TextureRegion invSlotHead;         // Head Item Slot
    public final TextureRegion invSlotHeadS;        // Head Item Slot (hovered)
    public final TextureRegion invSlotChest;        // Chest Item Slot
    public final TextureRegion invSlotChestS;       // Chest Item Slot (hovered)
    public final TextureRegion invSlotGloves;       // Gloves Item Slot
    public final TextureRegion invSlotGlovesS;      // Gloves Item Slot (hovered)
    public final TextureRegion invSlotLegs;         // Legs Item Slot
    public final TextureRegion invSlotLegsS;        // Legs Item Slot (hovered)
    public final TextureRegion invSlotFeet;         // Boots Item Slot
    public final TextureRegion invSlotFeetS;        // Boots Item Slot (hovered)

    private final TextureRegion hotbarBase;
    private final TextureRegion hotbarHealth;
    private final TextureRegion hotbarHunger;

    private final TextureRegion playerTabHead;

    private final TextureRegion tabTopLeft;
    private final TextureRegion tabTopRight;
    private final TextureRegion tabBottomLeft;
    private final TextureRegion tabBottomRight;
    private final TextureRegion tabBorder3x1;
    private final TextureRegion tabBorder1x3;
    private final TextureRegion tabPingIcon;

    public final TextureRegion whiteSquare;

    private final TextureRegion darkenSquarePattern;
    private final TextureRegion invBackground;      // Base inventory background

    private final TextureRegion tooltipTopLeft;
    private final TextureRegion tooltipTopRight;
    private final TextureRegion tooltipBottomLeft;
    private final TextureRegion tooltipBottomRight;
    private final TextureRegion tooltipBorder7x1;
    private final TextureRegion tooltipBorder1x7;
    private final TextureRegion tooltipFiller;

    private boolean inventoryOpenState;
    private boolean previousInventoryOpenState;

    public PlayerUI() {
        invSlot = tr("inv_slot");
        invSlotS = tr("inv_slotS");

        invSlotHead = tr("ui_inventory_headslot");
        invSlotHeadS = tr("ui_inventory_headslot_sel");

        invSlotChest = tr("ui_inventory_chestslot");
        invSlotChestS = tr("ui_inventory_chestslot_sel");

        invSlotGloves = tr("ui_inventory_gloveslot");
        invSlotGlovesS = tr("ui_inventory_gloveslot_sel");

        invSlotLegs = tr("ui_inventory_legslot");
        invSlotLegsS = tr("ui_inventory_legslot_sel");

        invSlotFeet = tr("ui_inventory_bootslot");
        invSlotFeetS = tr("ui_inventory_bootslot_sel");

        hotbarBase = tr("hotbar_base");
        hotbarHealth = tr("hotbar_health");
        hotbarHunger = tr("hotbar_hunger");

        invBackground = tr("inv_bg");

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
        darkenSquarePattern = tr("bg_squares128x128");

        glyphLayout = new GlyphLayout();

        hotbarSlots = new InteractableItemSlot[9];

        for(int i = 0; i < 9; i++) {
            hotbarSlots[i] = new InteractableItemSlot(this, i);
        }

        inventorySlots = new InteractableItemSlot[27];

        for(int i = 0 ; i < inventorySlots.length; i++) {
            inventorySlots[i] = new InteractableItemSlot(this, i + 9);
        }

        inventoryArmorSlots = new InteractableItemSlot[] {
                new InteractableItemSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_HEAD, invSlotHeadS, invSlotHead),
                new InteractableItemSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_CHEST, invSlotChestS, invSlotChest),
                new InteractableItemSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_GLOVES, invSlotGlovesS, invSlotGloves),
                new InteractableItemSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_LEGS, invSlotLegsS, invSlotLegs),
                new InteractableItemSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_FEET, invSlotFeetS, invSlotFeet)
        };

        // tooltip begin
        TextureRegion tooltip = tr("ui_tooltip");

        tooltipTopLeft = new TextureRegion(tooltip, 0, 0+16, 6, 6);
        tooltipTopRight = new TextureRegion(tooltip, 8, 0+16, 6, 6);
        tooltipBottomLeft = new TextureRegion(tooltip, 0, 8+16, 6, 6);
        tooltipBottomRight = new TextureRegion(tooltip, 8, 8+16, 6, 6);
        tooltipBorder1x7 = new TextureRegion(tooltip, 16, 0+16, 1, 4);
        tooltipBorder7x1 = new TextureRegion(tooltip, 18, 0+16, 4, 1);
        tooltipFiller = new TextureRegion(tooltip, 18, 2+16, 1, 1);
        // tooltip end

        changeUiScale(2.0f);
    }

    private TextureRegion tr(String name) {
        return ExpoAssets.get().textureRegion(name);
    }

    private boolean playerPresent() {
        return ClientPlayer.getLocalPlayer() != null;
    }

    public void drawTooltip(int x, int y, String text) {
        drawTooltipColored(x, y, text, Color.WHITE);
    }

    public void drawTooltip(String text) {
        drawTooltipColored(text, Color.WHITE);
    }

    public void drawTooltipColored(String text, Color color) {
        RenderContext r = RenderContext.get();
        drawTooltipColored((int) r.mouseX, (int) r.mouseY, text, color);
    }

    public void drawTooltipColored(int x, int y, String text, Color color) {
        x += 4 * uiScale; // offset
        y += 4 * uiScale; // offset

        glyphLayout.setText(m5x7_use, text);
        float tw = glyphLayout.width;
        float th = glyphLayout.height;

        float cornerSize = tooltipBottomLeft.getRegionWidth() * uiScale;
        float borderSize = 1 * uiScale * 2;
        tw += borderSize;
        th += borderSize;

        RenderContext r = RenderContext.get();

        r.hudBatch.draw(tooltipFiller, x + 4 * uiScale, y + 4 * uiScale, tw + 4 * uiScale, th + 4 * uiScale);

        r.hudBatch.draw(tooltipBottomLeft, x, y, cornerSize, cornerSize);
        r.hudBatch.draw(tooltipBottomRight, x + tw + cornerSize, y, cornerSize, cornerSize);
        r.hudBatch.draw(tooltipBorder1x7, x + cornerSize, y, tw, 4 * uiScale);

        r.hudBatch.draw(tooltipBorder7x1, x, y + cornerSize, 4 * uiScale, th);
        r.hudBatch.draw(tooltipBorder7x1, x + cornerSize + tw + (cornerSize - 4 * uiScale), y + cornerSize, 4 * uiScale, th);

        r.hudBatch.draw(tooltipTopLeft, x, y + cornerSize + th, cornerSize, cornerSize);
        r.hudBatch.draw(tooltipTopRight, x + tw + cornerSize, y + cornerSize + th, cornerSize, cornerSize);
        r.hudBatch.draw(tooltipBorder1x7, x + cornerSize, y + cornerSize + th + (cornerSize - 4 * uiScale), tw, 4 * uiScale);

        m5x7_use.setColor(color);
        m5x7_use.draw(r.hudBatch, text, x + cornerSize + (tw - glyphLayout.width) * 0.5f, y + cornerSize + th - (th - glyphLayout.height) * 0.5f);
        m5x7_use.setColor(Color.WHITE);
    }

    private void updateSlotVisibility() {
        for(InteractableItemSlot slot : inventoryArmorSlots) {
            slot.visible = inventoryOpenState;
        }

        for(InteractableItemSlot slot : inventorySlots) {
            slot.visible = inventoryOpenState;
        }
    }

    private void updateHotbarPosition() {
        if(inventoryOpenState) {
            // Inventory is now open.
            float startX = invX + 35 * uiScale;
            float startY = invY + 17 * uiScale;

            for(int i = 0; i < hotbarSlots.length; i++) {
                hotbarSlots[i].update(startX + (i * slotW + i * uiScale), startY, slotW, slotH);
            }

            for(int i = 0; i < inventorySlots.length; i++) {
                int x = i % 9;
                int y = i / 9;
                inventorySlots[i].update(startX + (x * slotW + x * uiScale), startY + 33 * uiScale + y * 30 * uiScale, slotW, slotH);
            }

            for(int i = 0; i < inventoryArmorSlots.length; i++) {
                inventoryArmorSlots[inventoryArmorSlots.length - 1 - i].update(invX + 4 * uiScale, invY + 4 * uiScale + i * 30 * uiScale, slotW, slotH);
            }
        } else {
            float startX = center(hotbarW);
            float startY = 2;

            for(int i = 0; i < hotbarSlots.length; i++) {
                hotbarSlots[i].update(startX + 4 * uiScale + (i * slotW + i * uiScale), startY + 4 * uiScale, slotW, slotH);
            }
        }
    }

    private void hoverCheck(InteractableItemSlot slot) {
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
                if(hoveredSlot.equals(slot)) {
                    hoveredSlot = null;
                }
            }
        }
    }

    public void update() {
            RenderContext r = RenderContext.get();

        previousInventoryOpenState = inventoryOpenState;
        inventoryOpenState = playerPresent() && ClientPlayer.getLocalPlayer().inventoryOpen;

        if(previousInventoryOpenState != inventoryOpenState) {
            // Update hotbar position.
            updateHotbarPosition();
            updateSlotVisibility();
        }

        if(r.mouseMoved || (previousInventoryOpenState != inventoryOpenState)) {
            for(InteractableItemSlot slot : hotbarSlots) {
                hoverCheck(slot);
            }

            for(InteractableItemSlot slot : inventorySlots) {
                hoverCheck(slot);
            }

            for(InteractableItemSlot slot : inventoryArmorSlots) {
                hoverCheck(slot);
            }
        }
    }

    private boolean uiElementInBounds(InteractableItemSlot slot) {
        RenderContext r = RenderContext.get();
        return r.mouseX >= slot.x && r.mouseX < slot.ex && r.mouseY >= slot.y && r.mouseY < slot.ey;
    }

    public void render() {
        RenderContext r = RenderContext.get();

        r.hudBatch.begin();

        if(PlayerInventory.LOCAL_INVENTORY == null) {
            r.hudBatch.end();
            return;
        }

        if(inventoryOpenState) {
            // Draw square pattern background
            float baseWH = darkenSquarePattern.getRegionWidth();
            int timesX = (int) (uiWidth / baseWH) + 1;
            int timesY = (int) (uiHeight / baseWH) + 1;

            for(int x = 0; x < timesX; x++) {
                for(int y = 0; y < timesY; y++) {
                    r.hudBatch.draw(darkenSquarePattern, x * baseWH, y * baseWH, baseWH, baseWH);
                }
            }

            // Draw inventory background
            r.hudBatch.draw(invBackground, invX, invY, invW, invH);

            // Draw inventory slots
            drawSlots(hotbarSlots, inventorySlots, inventoryArmorSlots);

            // Draw inventory text
            glyphLayout.setText(m5x7_shadow_use, "Inventory");
            m5x7_shadow_use.draw(r.hudBatch, "Inventory", invX + 31 * uiScale + ((invW - 31 * uiScale) - glyphLayout.width) * 0.5f, invY + invH + glyphLayout.height);
        } else {
            drawHotbar(r);
        }

        if(hoveredSlot != null && PlayerInventory.LOCAL_INVENTORY.cursorItem == null) {
            hoveredSlot.onTooltip();
        }

        if(tablistOpen) {
            drawTablist(r);
        }

        if(PlayerInventory.LOCAL_INVENTORY.cursorItem != null) {
            drawCursor(PlayerInventory.LOCAL_INVENTORY.cursorItem);
        }

        r.hudBatch.end();
    }

    private void drawSlots(InteractableItemSlot[]... slots) {
        for(InteractableItemSlot[] array : slots) {
            for(InteractableItemSlot slot : array) {
                slot.drawBase();
            }

            for(InteractableItemSlot slot : array) {
                slot.drawContents();
            }

            if(Expo.get().getImGuiExpo().shouldDrawSlotIndices()) {
                for(InteractableItemSlot slot : array) {
                    slot.drawSlotIndices();
                }
            }
        }
    }

    public void drawCursor(ClientInventoryItem item) {
        ItemMapping mapping = ItemMapper.get().getMapping(item.itemId);
        ItemRender render = mapping.uiRender;
        TextureRegion draw = render.textureRegion;

        RenderContext r = RenderContext.get();

        float dw = draw.getRegionWidth() * render.scaleX * uiScale;
        float dh = draw.getRegionHeight() * render.scaleY * uiScale;

        float _x = r.mouseX - dw * 0.5f;
        float _y = r.mouseY - dh * 0.5f;

        r.hudBatch.draw(draw, _x, _y, dw, dh);

        if(mapping.logic.maxStackSize > 1) {
            int amount = item.itemAmount;
            String amountAsText = amount + "";

            glyphLayout.setText(m5x7_shadow_use, amountAsText);
            float aw = glyphLayout.width;
            float ah = glyphLayout.height;

            float artificialEx = _x + (slotW - dw) * 0.5f + dw;
            float artificialBy = _y - (slotH - dh) * 0.5f;

            m5x7_shadow_use.draw(r.hudBatch, amountAsText, artificialEx - 1 * uiScale - aw, artificialBy + ah + 1 * uiScale);
        }
    }

    private void drawTablist(RenderContext r) {
        var map = ExpoClientContainer.get().getPlayerOnlineList();
        float muW = 0, muH = 0; // Max Username width + height
        float mpW = 0; // Max ping width + height
        int lines = map.size();

        // Determine max width + height per line
        for(String username : map.keySet()) {
            glyphLayout.setText(m5x7_shadow_use, username);
            if(glyphLayout.width > muW) muW = glyphLayout.width;
            if(glyphLayout.height > muH) muH = glyphLayout.height;

            glyphLayout.setText(m5x7_shadow_use, "999+"); // max ping to display = 999
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
        r.hudBatch.setColor(64f / 255f, 64f / 255f, 64f / 255f, 1.0f);
        r.hudBatch.draw(whiteSquare, startDrawX + 1 * uiScale, startDrawY + 1 * uiScale, totalWidth - 2 * uiScale, totalHeight - 2 * uiScale);
        r.hudBatch.setColor(Color.WHITE);

        // Bottom row
        r.hudBatch.draw(tabBottomLeft, startDrawX, startDrawY, cornerWH, cornerWH);
        r.hudBatch.draw(tabBottomRight, startDrawX + totalWidth - cornerWH, startDrawY, cornerWH, cornerWH);
        r.hudBatch.draw(tabBorder1x3, startDrawX + cornerWH, startDrawY, totalWidth - cornerWH * 2, borderWH);

        // Top row
        r.hudBatch.draw(tabTopLeft, startDrawX, startDrawY + totalHeight - cornerWH, cornerWH, cornerWH);
        r.hudBatch.draw(tabTopRight, startDrawX + totalWidth - cornerWH, startDrawY + totalHeight - cornerWH, cornerWH, cornerWH);
        r.hudBatch.draw(tabBorder1x3, startDrawX + cornerWH, startDrawY + totalHeight - borderWH, totalWidth - cornerWH * 2, borderWH);

        // Sides
        r.hudBatch.draw(tabBorder3x1, startDrawX, startDrawY + cornerWH, borderWH, totalHeight - cornerWH * 2);
        r.hudBatch.draw(tabBorder3x1, startDrawX + totalWidth - borderWH, startDrawY + cornerWH, borderWH, totalHeight - cornerWH * 2);

        // For each element
        float rowOffsetX = borderWH + 2 * uiScale;
        float rowOffsetY = borderWH + 2 * uiScale;

        for(String username : map.keySet()) {
            float _x = startDrawX + rowOffsetX;
            float _w = pthW + 2 * uiScale;

            r.hudBatch.setColor(43f / 255f, 43f / 255f, 43f / 255f, 1.0f);
            r.hudBatch.draw(whiteSquare, _x, startDrawY + rowOffsetY, _w, rowHeight);

            _x += _w + 2 * uiScale;
            _w = muW + 4 * uiScale;
            r.hudBatch.draw(whiteSquare, _x, startDrawY + rowOffsetY, _w, rowHeight);

            _x += _w + 2 * uiScale;
            _w = mpW + 6 * uiScale + pingW;
            r.hudBatch.draw(whiteSquare, _x, startDrawY + rowOffsetY, _w, rowHeight);

            r.hudBatch.setColor(Color.WHITE);
            r.hudBatch.draw(playerTabHead, startDrawX + rowOffsetX + 1 * uiScale, startDrawY + rowOffsetY + 1 * uiScale, pthW, pthH);
            m5x7_shadow_use.draw(r.hudBatch, username, startDrawX + rowOffsetX + pthW + 2 * uiScale + 2 * uiScale + 2 * uiScale, startDrawY + rowOffsetY + 2 * uiScale + muH + 1 * uiScale);

            long ping = map.get(username); // debug

            if(ping < 80) {
                m5x7_shadow_use.setColor(COLOR_GREEN);
                r.hudBatch.setColor(COLOR_GREEN);
            } else if(ping < 200) {
                m5x7_shadow_use.setColor(COLOR_YELLOW);
                r.hudBatch.setColor(COLOR_YELLOW);
            } else {
                m5x7_shadow_use.setColor(COLOR_RED);
                r.hudBatch.setColor(COLOR_RED);
            }

            String pingAsString = (ping > 999 ? "999+" : ping + "");
            glyphLayout.setText(m5x7_shadow_use, pingAsString);

            float pingBoxWidth = pingW + 6 * uiScale + mpW;
            float pingIconX = _x + (pingBoxWidth - (pingW + 2 * uiScale + glyphLayout.width)) * 0.5f;
            r.hudBatch.draw(tabPingIcon, pingIconX, startDrawY + rowOffsetY + (rowHeight - pingH) * 0.5f, pingW, pingH);
            m5x7_shadow_use.draw(r.hudBatch, pingAsString, pingIconX + 2 * uiScale + pingW, startDrawY + rowOffsetY + 2 * uiScale + muH + 1 * uiScale);

            m5x7_shadow_use.setColor(Color.WHITE);
            r.hudBatch.setColor(Color.WHITE);

            rowOffsetY += rowHeight + 2 * uiScale;
        }
    }

    private void drawHotbar(RenderContext r) {
        float startX = center(hotbarW);
        float startY = 2;

        // Hotbar base
        r.hudBatch.draw(hotbarBase, startX, startY, hotbarW, hotbarH);

        // Health + hunger
        r.hudBatch.draw(hotbarHealth, startX + 10 * uiScale, startY + 39 * uiScale, healthW, healthH);
        r.hudBatch.draw(hotbarHunger, startX + 230 * uiScale, startY + 39 * uiScale, hungerW, hungerH);

        // Hotbar slots
        drawSlots(hotbarSlots);

        // Health status
        status(r, ClientPlayer.getLocalPlayer().playerHealth, startX + 22 * uiScale, startY + 38 * uiScale);

        // Hunger status
        status(r, ClientPlayer.getLocalPlayer().playerHunger, startX + 196 * uiScale, startY + 38 * uiScale);

        // Current item
        var local = PlayerInventory.LOCAL_INVENTORY.currentItem();
        itemText(r, local == null ? null : local.toMapping(), startX + 56 * uiScale, startY + 38 * uiScale);
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

        glyphLayout.setText(m5x7_border_use, text);
        m5x7_border_use.setColor(c);
        m5x7_border_use.draw(rc.hudBatch, text, x + (138 * uiScale - glyphLayout.width) * 0.5f, y + glyphLayout.height + (11 * uiScale - glyphLayout.height) * 0.5f);
        m5x7_border_use.setColor(Color.WHITE);
    }

    private void status(RenderContext rc, float status, float x, float y) {
        float r, g, b;

        if(status > COLOR_GRADIENTS[1]) {
            r = COLOR_GREEN.r;
            g = COLOR_GREEN.g;
            b = COLOR_GREEN.b;
        } else if(status > COLOR_GRADIENTS[2]) {
            float d = COLOR_GRADIENTS[1] - COLOR_GRADIENTS[2]; // =20
            float s = COLOR_GRADIENTS[1] - status; // 0->20
            float n = s / d;

            float rD = COLOR_YELLOW.r - COLOR_GREEN.r;
            r = COLOR_GREEN.r + rD * n;
            float gD = COLOR_YELLOW.g - COLOR_GREEN.g;
            g = COLOR_GREEN.g + gD * n;
            float bD = COLOR_YELLOW.b - COLOR_GREEN.b;
            b = COLOR_GREEN.b + bD * n;
        } else if(status > COLOR_GRADIENTS[3]) {
            r = COLOR_YELLOW.r;
            g = COLOR_YELLOW.g;
            b = COLOR_YELLOW.b;
        } else if(status > COLOR_GRADIENTS[4]) {
            float d = COLOR_GRADIENTS[3] - COLOR_GRADIENTS[4]; // =20
            float s = COLOR_GRADIENTS[3] - status; // 0->20
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

        String text = (int) status + "%";
        glyphLayout.setText(m5x7_border_use, text);

        m5x7_border_use.setColor(r, g, b, 1.0f);
        m5x7_border_use.draw(rc.hudBatch, text, x + (32 * uiScale - glyphLayout.width) * 0.5f, y + glyphLayout.height + (11 * uiScale - glyphLayout.height) * 0.5f);
    }

    public void changeUiScale(float scale) {
        uiWidth = Gdx.graphics.getWidth();
        uiHeight = Gdx.graphics.getHeight();
        uiScale = scale;

        pthW = playerTabHead.getRegionWidth() * uiScale;
        pthH = playerTabHead.getRegionHeight() * uiScale;
        cornerWH = tabTopLeft.getRegionWidth() * uiScale;
        pingW = tabPingIcon.getRegionWidth() * uiScale;
        pingH = tabPingIcon.getRegionHeight() * uiScale;
        borderWH = tabBorder3x1.getRegionWidth() * uiScale;

        invW = invBackground.getRegionWidth() * uiScale;
        invH = invBackground.getRegionHeight() * uiScale;

        invX = center(invW);
        invY = centerY(invH);

        hotbarW = hotbarBase.getRegionWidth() * uiScale;
        hotbarH = hotbarBase.getRegionHeight() * uiScale;

        slotW = invSlot.getRegionWidth() * uiScale;
        slotH = invSlot.getRegionHeight() * uiScale;

        healthW = hotbarHealth.getRegionWidth() * uiScale;
        healthH = hotbarHealth.getRegionHeight() * uiScale;

        hungerW = hotbarHunger.getRegionWidth() * uiScale;
        hungerH = hotbarHunger.getRegionHeight() * uiScale;

        m5x7_use = RenderContext.get().m5x7_all[(int) uiScale - 1];
        m6x11_use = RenderContext.get().m6x11_all[(int) uiScale - 1];

        m5x7_border_use = RenderContext.get().m5x7_border_all[(int) uiScale - 1];
        m6x11_border_use = RenderContext.get().m6x11_border_all[(int) uiScale - 1];

        m5x7_shadow_use = RenderContext.get().m5x7_shadow_all[(int) uiScale - 1];

        updateHotbarPosition();
    }

    public void onResize() {
        uiWidth = Gdx.graphics.getWidth();
        uiHeight = Gdx.graphics.getHeight();

        invX = center(invW);
        invY = centerY(invH);
        updateHotbarPosition();
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

    }

    public void onPlayerQuit(String username) {

    }

}
