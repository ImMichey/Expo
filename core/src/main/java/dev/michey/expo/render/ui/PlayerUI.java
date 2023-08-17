package dev.michey.expo.render.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.client.chat.ExpoClientChat;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipe;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipeMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.client.ItemRender;
import dev.michey.expo.util.*;
import dev.michey.expo.weather.Weather;

import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public class PlayerUI {

    /** The scale for all UI elements (will be changeable in the options). */
    public float uiScale; // default = 2.0f
    private float uiWidth, uiHeight;

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
    public final Color COLOR_GREEN = new Color(127f / 255f, 237f / 255f, 51f / 255f, 1.0f);
    public final String COLOR_GREEN_HEX = COLOR_GREEN.toString();
    private final Color COLOR_YELLOW  = new Color(251f / 255f, 242f / 255f, 54f / 255f, 1.0f);
    private final Color COLOR_RED = new Color(210f / 255f, 27f / 255f, 27f / 255f, 1.0f);
    public final String COLOR_DESCRIPTOR_HEX = "[#5875b0]";
    public final String COLOR_DESCRIPTOR2_HEX = "[#bdc4d4]";
    private final float[] COLOR_GRADIENTS = new float[] {100f, 80f, 60f, 40f, 20f, 0f};
    public InteractableItemSlot[] hotbarSlots;
    public InteractableUIElement hoveredSlot = null;

    /** Inventory */
    private float invW, invH;
    private float invX, invY;
    public InteractableItemSlot[] inventorySlots;
    public InteractableItemSlot[] inventoryArmorSlots;
    public InteractableUIElement craftOpenSlot;
    public InteractableUIElement craftButtonLeft, craftButtonRight;
    public InteractableUIElement[] craftCategorySlots;
    public InteractableRecipeSlot[] craftRecipeSlots;
    private InteractableUIElement selectedCategoryButton;
    public boolean craftingOpen = false;

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
    public final TextureRegion craftSlot;           // Crafting Item Slot
    public final TextureRegion craftSlotS;          // Crafting Item Slot (hovered)

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

    public final TextureRegion craftOpen;           // Open Craft Menu Item Slot
    public final TextureRegion craftOpenS;          // Open Craft Menu Item Slot (hovered)
    public final TextureRegion craftArrowLeft;
    public final TextureRegion craftArrowLeftS;
    public final TextureRegion craftArrowRight;
    public final TextureRegion craftArrowRightS;
    public final TextureRegion craftCategoryMisc;
    public final TextureRegion craftCategoryMiscS;
    public final TextureRegion craftCategoryTools;
    public final TextureRegion craftCategoryToolsS;
    public final TextureRegion craftCategoryFood;
    public final TextureRegion craftCategoryFoodS;

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

    private final TextureRegion invBackground;      // Base inventory background
    private final TextureRegion invBackgroundCrafting;

    private final TextureRegion tooltipTopLeft;
    private final TextureRegion tooltipTopRight;
    private final TextureRegion tooltipBottomLeft;
    private final TextureRegion tooltipBottomRight;
    private final TextureRegion tooltipBorder7x1;
    private final TextureRegion tooltipBorder1x7;
    private final TextureRegion tooltipFiller;
    private final TextureRegion tooltipFillerLight;
    private final TextureRegion tooltipFillerCrafting;

    private boolean inventoryOpenState;
    private boolean craftingOpenState;

    public PlayerUI() {
        invSlot = tr("inv_slot");
        invSlotS = tr("inv_slotS");
        craftSlot = tr("ui_crafting_emptyslot");
        craftSlotS = tr("ui_crafting_emptyslot_sel");

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

        craftOpen = tr("ui_crafting_open");
        craftOpenS = tr("ui_crafting_open_sel");
        craftArrowLeft = tr("ui_crafting_arrow_left");
        craftArrowLeftS = tr("ui_crafting_arrow_left_sel");
        craftArrowRight = tr("ui_crafting_arrow_right");
        craftArrowRightS = tr("ui_crafting_arrow_right_sel");
        craftCategoryMisc = tr("ui_crafting_category_misc");
        craftCategoryMiscS = tr("ui_crafting_category_misc_sel");
        craftCategoryFood = tr("ui_crafting_category_food");
        craftCategoryFoodS = tr("ui_crafting_category_food_sel");
        craftCategoryTools = tr("ui_crafting_category_tools");
        craftCategoryToolsS = tr("ui_crafting_category_tools_sel");

        hotbarBase = tr("hotbar_base");
        hotbarHealth = tr("hotbar_health");
        hotbarHunger = tr("hotbar_hunger");

        invBackground = tr("inv_bgc_");
        invBackgroundCrafting = tr("inv_bgco");

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

        craftOpenSlot = new InteractableUIElement(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_OPEN, craftOpenS, craftOpen) {

            @Override
            public void onLeftClick() {
                craftingOpen = !craftingOpen;
                updateInventoryBounds();
                updateHotbarPosition();
                updateInventoryElements();
            }

            @Override
            public void onTooltip() {
                drawTooltipColored((craftingOpen ? "Close" : "Open") + " Crafting", ClientStatic.COLOR_CRAFT_TEXT);
            }

        };

        craftButtonLeft = new InteractableUIElement(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_ARROW_LEFT, craftArrowLeftS, craftArrowLeft) {

            @Override
            public void onTooltip() {
                drawTooltipColored("Previous categories", ClientStatic.COLOR_CRAFT_TEXT);
            }

        };

        craftButtonRight = new InteractableUIElement(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_ARROW_RIGHT, craftArrowRightS, craftArrowRight) {

            @Override
            public void onTooltip() {
                drawTooltipColored("Next categories", ClientStatic.COLOR_CRAFT_TEXT);
            }

        };

        craftCategorySlots = new InteractableUIElement[4];
        craftCategorySlots[0] = new InteractableUIElement(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_MISC, craftCategoryMiscS, craftCategoryMisc) {
            @Override
            public void onTooltip() {
                drawTooltipColored("Category: Misc", ClientStatic.COLOR_CRAFT_TEXT);
            }

            @Override
            public void onLeftClick() {
                showCraftingRecipes(this, ExpoShared.CRAFTING_CATEGORY_MISC);
            }
        };
        craftCategorySlots[1] = new InteractableUIElement(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_TOOLS, craftCategoryToolsS, craftCategoryTools) {
            @Override
            public void onTooltip() {
                drawTooltipColored("Category: Tools", ClientStatic.COLOR_CRAFT_TEXT);
            }

            @Override
            public void onLeftClick() {
                showCraftingRecipes(this, ExpoShared.CRAFTING_CATEGORY_TOOLS);
            }
        };
        craftCategorySlots[2] = new InteractableUIElement(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_FOOD, craftCategoryFoodS, craftCategoryFood) {
            @Override
            public void onTooltip() {
                drawTooltipColored("Category: Food", ClientStatic.COLOR_CRAFT_TEXT);
            }

            @Override
            public void onLeftClick() {
                showCraftingRecipes(this, ExpoShared.CRAFTING_CATEGORY_FOOD);
            }
        };
        craftCategorySlots[3] = new InteractableUIElement(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_CATEGORY_3, craftSlotS, craftSlot);

        craftRecipeSlots = new InteractableRecipeSlot[25];
        for(int i = 0; i < craftRecipeSlots.length; i++) craftRecipeSlots[i] = new InteractableRecipeSlot(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_RECIPE_BASE);
        showCraftingRecipes(craftCategorySlots[0], ExpoShared.CRAFTING_CATEGORY_MISC);

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

        changeUiScale();
    }

    public void addPickupLine(int itemId, int itemAmount) {
        synchronized(PICKUP_LOCK) {
            pickupLines.add(new PickupLine(itemId, itemAmount));
        }
    }

    public void showCraftingRecipes(InteractableUIElement button, int category) {
        if(selectedCategoryButton != null) {
            selectedCategoryButton.selected = false;
        }
        selectedCategoryButton = button;
        selectedCategoryButton.selected = true;

        for(var slot : craftRecipeSlots) {
            slot.setHoldingRecipe(null);
        }

        CraftingRecipeMapping mapping = CraftingRecipeMapping.get();
        var recipes = mapping.getCategoryMap().get(category);

        int slotId = 24;

        for(String s : recipes) {
            CraftingRecipe recipe = mapping.getRecipeMap().get(s);
            craftRecipeSlots[slotId].setHoldingRecipe(recipe);
            slotId--;
            if(slotId == -1) break; // Implement in future when > 25 recipes per category
        }
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

    public void drawTooltipColored(String text, Color color, String... extraLines) {
        RenderContext rc = RenderContext.get();
        drawTooltipColored((int) rc.mouseX, (int) rc.mouseY, text, color, extraLines);
    }

    public void drawTooltipCraftingRecipe(CraftingRecipe recipe) {
        RenderContext rc = RenderContext.get();
        int x = (int) rc.mouseX;
        int y = (int) rc.mouseY;

        x += 4 * uiScale;
        y += 4 * uiScale;

        ItemMapping mapping = ItemMapper.get().getMapping(recipe.outputId);
        String outputText = recipe.outputAmount + "x " + mapping.displayName;

        glyphLayout.setText(rc.m6x11_use, outputText);
        float titleWidth = glyphLayout.width;

        float innerWidth = 8 * uiScale + titleWidth;

        glyphLayout.setText(rc.m5x7_use, "Ingredients:");
        float ingredientsWidth = glyphLayout.width + 8 * uiScale;
        float generalM5X7Height = glyphLayout.height;

        if(ingredientsWidth > innerWidth) innerWidth = (ingredientsWidth);
        float maxIngredientRowWidth = 0;

        for(int i = 0; i < recipe.inputIds.length; i++) {
            String ingredientRowText = recipe.inputAmounts[i] + "x " + ItemMapper.get().getMapping(recipe.inputIds[i]).displayName;
            glyphLayout.setText(rc.m5x7_use, ingredientRowText);
            float ingredientRowWidth = glyphLayout.width + 28 * uiScale;
            if(ingredientRowWidth > innerWidth) innerWidth = ingredientRowWidth;
            if(ingredientRowWidth > maxIngredientRowWidth) maxIngredientRowWidth = ingredientRowWidth;
        }

        if(titleWidth > maxIngredientRowWidth) maxIngredientRowWidth = titleWidth;

        // Check if shift needed
        float totalWidthTooltip = maxIngredientRowWidth + 9 * uiScale + cornerWH;
        float proposedEndX = x + totalWidthTooltip;
        float maxDisplayX = Gdx.graphics.getWidth();

        if(proposedEndX >= maxDisplayX) {
            x -= totalWidthTooltip;
        }

        rc.hudBatch.draw(tooltipFiller, x + 4 * uiScale, y + 4 * uiScale, maxIngredientRowWidth + 7 * uiScale, 43 * uiScale + recipe.inputIds.length * 24 * uiScale + (recipe.inputIds.length - 1) * 1 * uiScale);

        // Draw ingredient rows
        float _cx = x + 7 * uiScale;
        float _cy = y + 7 * uiScale;
        float _coy = 0;

        for(int i = 0; i < recipe.inputIds.length; i++) {
            rc.hudBatch.draw(tooltipFillerCrafting, _cx, _cy + _coy, maxIngredientRowWidth, 24 * uiScale);

            int id = recipe.inputIds[i];
            int am = recipe.inputAmounts[i];
            boolean hasIngredient = ClientPlayer.getLocalPlayer().playerInventory.hasItem(id, am);

            ItemMapping m = ItemMapper.get().getMapping(id);
            TextureRegion ingredientTexture = m.uiRender.textureRegion;

            float centeredTextureX = (24 - ingredientTexture.getRegionWidth()) * 0.5f * uiScale;
            float centeredTextureY = (24 - ingredientTexture.getRegionHeight()) * 0.5f * uiScale;

            rc.hudBatch.draw(ingredientTexture, _cx + centeredTextureX, _cy + _coy + centeredTextureY, ingredientTexture.getRegionWidth() * uiScale, ingredientTexture.getRegionHeight() * uiScale);

            String ingredientText = am + "x " + m.displayName;
            glyphLayout.setText(rc.m5x7_use, ingredientText);
            float th = glyphLayout.height;

            rc.m5x7_use.setColor(hasIngredient ? ClientStatic.COLOR_CRAFT_GREEN : ClientStatic.COLOR_CRAFT_RED);
            rc.m5x7_use.draw(rc.hudBatch, ingredientText, _cx + 24 * uiScale, _cy + _coy + th + (24 * uiScale - th) * 0.5f);
            rc.m5x7_use.setColor(Color.WHITE);

            _coy += 24 * uiScale + 1 * uiScale;
        }

        float _iy = _cy + _coy + 5 * uiScale;

        // Ingredients: text
        rc.m5x7_use.setColor(ClientStatic.COLOR_CRAFT_INGREDIENTS);
        rc.m5x7_use.draw(rc.hudBatch, "Ingredients:", _cx, _iy + generalM5X7Height);
        rc.m5x7_use.setColor(Color.WHITE);

        // Header line
        float headerY = _iy + generalM5X7Height + 11 * uiScale;
        glyphLayout.setText(rc.m6x11_use, outputText);
        rc.m6x11_use.draw(rc.hudBatch, outputText, _cx, headerY + glyphLayout.height);

        float endY = headerY + glyphLayout.height + 4 * uiScale;
        drawBorderAt(rc, x, y, maxIngredientRowWidth + 2 * uiScale, endY - y - 9 * uiScale);

        // Divider line
        rc.hudBatch.draw(tooltipFillerLight, x + 3 * uiScale, _iy + generalM5X7Height + 4 * uiScale, maxIngredientRowWidth + 8 * uiScale, 2 * uiScale);
    }

    public void drawTooltipColored(int x, int y, String text, Color color, String... extraLines) {
        RenderContext rc = RenderContext.get();
        x += 4 * uiScale; // offset
        y += 4 * uiScale; // offset

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

    private void updateSlotVisibility() {
        for(InteractableItemSlot slot : inventoryArmorSlots) {
            slot.visible = inventoryOpenState;
        }

        for(InteractableItemSlot slot : inventorySlots) {
            slot.visible = inventoryOpenState;
        }

        craftOpenSlot.visible = inventoryOpenState;
        craftButtonRight.visible = inventoryOpenState && craftingOpen;
        craftButtonLeft.visible = inventoryOpenState && craftingOpen;
        for(var el : craftCategorySlots) el.visible = inventoryOpenState && craftingOpen;
        for(var el : craftRecipeSlots) el.visible = inventoryOpenState && craftingOpen;
    }

    private void updateHotbarPosition() {
        if(inventoryOpenState) {
            // Inventory is now open.
            float startX = invX + 35 * uiScale;
            float startY = invY + 17 * uiScale + (craftingOpen ? 15 * uiScale : 0);

            for(int i = 0; i < hotbarSlots.length; i++) {
                hotbarSlots[i].update(startX + (i * slotW + i * uiScale), startY, slotW, slotH);
            }

            for(int i = 0; i < inventorySlots.length; i++) {
                int x = i % 9;
                int y = i / 9;
                inventorySlots[i].update(startX + (x * slotW + x * uiScale), startY + 33 * uiScale + y * 30 * uiScale, slotW, slotH);
            }

            for(int i = 0; i < inventoryArmorSlots.length; i++) {
                inventoryArmorSlots[inventoryArmorSlots.length - 1 - i].update(invX + 4 * uiScale, invY + 4 * uiScale + i * 30 * uiScale + (craftingOpen ? 15 * uiScale : 0), slotW, slotH);
            }

            if(craftingOpen) {
                craftOpenSlot.update(invX + 429 * uiScale, invY + 79 * uiScale, slotW, slotH);
                craftButtonLeft.update(invX + 285 * uiScale, invY + 163 * uiScale, craftArrowRight.getRegionWidth() * uiScale, craftArrowRight.getRegionHeight() * uiScale);
                craftButtonRight.update(invX + 409 * uiScale, invY + 163 * uiScale, craftArrowRight.getRegionWidth() * uiScale, craftArrowRight.getRegionHeight() * uiScale);
                for(int i = 0; i < craftCategorySlots.length; i++) {
                    var el = craftCategorySlots[i];
                    el.update(invX + 299 * uiScale + i * slotW + uiScale * i, invY + 153 * uiScale, slotW, slotH);
                }
                for(int i = 0; i < craftRecipeSlots.length; i++) {
                    var el = craftRecipeSlots[i];
                    int x = i % 5;
                    int y = i / 5;
                    el.update(invX + 390 * uiScale - x * slotW - uiScale * x, invY + 4 * uiScale + y * slotH + uiScale * y, slotW, slotH);
                }
            } else {
                craftOpenSlot.update(invX + 282 * uiScale, invY + 63 * uiScale, slotW, slotH);
            }
        } else {
            float startX = center(hotbarW);
            float startY = 2;

            for(int i = 0; i < hotbarSlots.length; i++) {
                hotbarSlots[i].update(startX + 4 * uiScale + (i * slotW + i * uiScale), startY + 4 * uiScale, slotW, slotH);
            }
        }
    }

    private void hoverCheck(InteractableUIElement slot) {
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

    public void updateInventoryElements() {
        for(InteractableItemSlot slot : hotbarSlots) {
            hoverCheck(slot);
        }

        for(InteractableItemSlot slot : inventorySlots) {
            hoverCheck(slot);
        }

        for(InteractableItemSlot slot : inventoryArmorSlots) {
            hoverCheck(slot);
        }

        hoverCheck(craftOpenSlot);
        hoverCheck(craftButtonLeft);
        hoverCheck(craftButtonRight);
        for(var el : craftCategorySlots) hoverCheck(el);
        for(var el : craftRecipeSlots) hoverCheck(el);
    }

    public void update() {
        RenderContext rc = RenderContext.get();

        boolean previousInventoryOpenState = inventoryOpenState;
        boolean previousCraftingOpenState = craftingOpenState;
        inventoryOpenState = playerPresent() && ClientPlayer.getLocalPlayer().inventoryOpen;
        craftingOpenState = inventoryOpenState && craftingOpen;

        rc.blurActive = inventoryOpenState;

        if(previousInventoryOpenState != inventoryOpenState) {
            // Update hotbar position.
            updateHotbarPosition();
            updateSlotVisibility();
        } else if(previousCraftingOpenState != craftingOpenState) {
            updateSlotVisibility();
        }

        if(rc.mouseMoved || (previousInventoryOpenState != inventoryOpenState)) {
            updateInventoryElements();
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

                Vector2 hudPos = ClientUtils.entityPosToHudPos(player.clientPosX + 5, player.clientPosY + 32);
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
        Vector2 startHudPos = ClientUtils.entityPosToHudPos(ClientPlayer.getLocalPlayer().clientPosX + 5, ClientPlayer.getLocalPlayer().clientPosY + 32 + glyphLayout.height);
        float MAX_LINE_LIFETIME = 1.25f;

        // Draw pickup lines
        BitmapFont useFont = rc.m5x7_border_use;

        synchronized(PICKUP_LOCK) {
            for(PickupLine line : pickupLines) {
                line.lifetime += rc.delta;
                if(line.lifetime >= MAX_LINE_LIFETIME) continue;

                float alpha = Interpolation.circleOut.apply(line.lifetime / MAX_LINE_LIFETIME);

                ItemMapping mapping = ItemMapper.get().getMapping(line.itemId);
                String displayText = line.itemAmount + "x " + mapping.displayName;
                glyphLayout.setText(useFont, displayText);

                float itemW = mapping.uiRender.textureRegion.getRegionWidth() * uiScale;
                float itemH = mapping.uiRender.textureRegion.getRegionHeight() * uiScale;
                float fullWidth = itemW + 4 * uiScale + glyphLayout.width;
                float startX = startHudPos.x - fullWidth * 0.5f;
                float startY = startHudPos.y + alpha * 48;

                rc.hudBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f - line.lifetime / MAX_LINE_LIFETIME);
                useFont.setColor(mapping.color.r, mapping.color.g, mapping.color.b, 1.0f - line.lifetime / MAX_LINE_LIFETIME);

                rc.hudBatch.draw(mapping.uiRender.textureRegion, startX, startY - (itemH - glyphLayout.height) * 0.5f, itemW, itemH);
                useFont.draw(rc.hudBatch, displayText, startX + itemW + 4 * uiScale, startY + glyphLayout.height);
            }

            rc.hudBatch.setColor(Color.WHITE);
            useFont.setColor(Color.WHITE);
            pickupLines.removeIf(line -> line.lifetime >= MAX_LINE_LIFETIME);
        }

        if(inventoryOpenState) {
            // Draw inventory background
            rc.hudBatch.draw(craftingOpen ? invBackgroundCrafting : invBackground, invX, invY, invW, invH);

            // Draw inventory slots
            drawSlots(hotbarSlots, inventorySlots, inventoryArmorSlots);
            if(craftOpenSlot.visible) craftOpenSlot.drawBase();
            if(craftButtonLeft.visible) craftButtonLeft.drawBase();
            if(craftButtonRight.visible) craftButtonRight.drawBase();
            for(var el : craftCategorySlots) if(el.visible) el.drawBase();
            for(var el : craftRecipeSlots) if(el.visible) el.drawBase();

            // Draw inventory text [244]
            glyphLayout.setText(rc.m5x7_shadow_use, "Inventory");
            float invTextOffsetX = ((244 * uiScale) - glyphLayout.width) * 0.5f;
            float invTextOffsetY = glyphLayout.height + (craftingOpen ? 15 * uiScale : 0) + 156 * uiScale;
            rc.m5x7_shadow_use.draw(rc.hudBatch, "Inventory", invX + 35 * uiScale + invTextOffsetX, invY + invTextOffsetY);

            // Draw Crafting text
            if(craftingOpen) {
                glyphLayout.setText(rc.m5x7_shadow_use, "Crafting");
                float cTextOffsetX = (150 * uiScale - glyphLayout.width) * 0.5f;
                rc.m5x7_shadow_use.draw(rc.hudBatch, "Crafting", invX + 278 * uiScale + cTextOffsetX, invY + 199 * uiScale + glyphLayout.height);
            }
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

    private void drawSlots(InteractableItemSlot[]... slots) {
        for(InteractableItemSlot[] array : slots) {
            for(InteractableItemSlot slot : array) {
                slot.drawBase();
            }

            for(InteractableItemSlot slot : array) {
                slot.drawContents();
            }

            if(DEV_MODE && Expo.get().getImGuiExpo().shouldDrawSlotIndices()) {
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

        RenderContext rc = RenderContext.get();

        float dw = draw.getRegionWidth() * render.scaleX * uiScale;
        float dh = draw.getRegionHeight() * render.scaleY * uiScale;

        float _x = rc.mouseX - dw * 0.5f;
        float _y = rc.mouseY - dh * 0.5f;

        rc.hudBatch.draw(draw, _x, _y, dw, dh);

        if(mapping.logic.maxStackSize > 1) {
            int amount = item.itemAmount;
            String amountAsText = String.valueOf(amount);

            glyphLayout.setText(rc.m5x7_shadow_use, amountAsText);
            float aw = glyphLayout.width;
            float ah = glyphLayout.height;

            float artificialEx = _x + (slotW - dw) * 0.5f + dw;
            float artificialBy = _y - (slotH - dh) * 0.5f;

            rc.m5x7_shadow_use.draw(rc.hudBatch, amountAsText, artificialEx - 1 * uiScale - aw, artificialBy + ah + 1 * uiScale);
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

        glyphLayout.setText(rc.m5x7_border_use, text);
        rc.m5x7_border_use.setColor(c);
        rc.m5x7_border_use.draw(rc.hudBatch, text, x + (138 * uiScale - glyphLayout.width) * 0.5f, y + glyphLayout.height + (11 * uiScale - glyphLayout.height) * 0.5f);
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

    public void updateInventoryBounds() {
        TextureRegion bgTex = craftingOpen ? invBackgroundCrafting : invBackground;
        invW = bgTex.getRegionWidth() * uiScale;
        invH = bgTex.getRegionHeight() * uiScale;

        invX = center(invW);
        invY = centerY(invH);
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

        updateInventoryBounds();

        hotbarW = hotbarBase.getRegionWidth() * uiScale;
        hotbarH = hotbarBase.getRegionHeight() * uiScale;

        slotW = invSlot.getRegionWidth() * uiScale;
        slotH = invSlot.getRegionHeight() * uiScale;

        healthW = hotbarHealth.getRegionWidth() * uiScale;
        healthH = hotbarHealth.getRegionHeight() * uiScale;

        hungerW = hotbarHunger.getRegionWidth() * uiScale;
        hungerH = hotbarHunger.getRegionHeight() * uiScale;

        playerMinimap.updateWH(uiScale);

        updateHotbarPosition();

        int lines = 12;
        int total = 19 + lines * 11;

        float _uiScale = uiScale - 1;
        if(_uiScale <= 0) _uiScale = 1;

        chat.resize((int) (180 * (_uiScale + 1)), (int) (total * (_uiScale)), _uiScale);
    }

    public void onResize() {
        uiWidth = Gdx.graphics.getWidth();
        uiHeight = Gdx.graphics.getHeight();

        invX = center(invW);
        invY = centerY(invH);
        updateHotbarPosition();

        chat.readjust();
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