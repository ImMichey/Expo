package dev.michey.expo.render.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipe;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipeMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.client.ItemRender;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ClientUtils;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.weather.Weather;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ClientStatic.DEV_MODE;

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
    public final Color COLOR_GREEN = new Color(127f / 255f, 237f / 255f, 51f / 255f, 1.0f);
    public final String COLOR_GREEN_HEX = COLOR_GREEN.toString();
    private final Color COLOR_YELLOW  = new Color(251f / 255f, 242f / 255f, 54f / 255f, 1.0f);
    private final Color COLOR_RED = new Color(210f / 255f, 27f / 255f, 27f / 255f, 1.0f);
    public final String COLOR_DESCRIPTOR_HEX = "[#5875b0]";
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

    private final TextureRegion darkenSquarePattern;
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
        darkenSquarePattern = tr("bg_squares128x128");

        playerMinimap = new PlayerMinimap(this, tr("ui_minimap"), tr("ui_minimap_arrow"), tr("ui_minimap_player"));

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
            public void onLeftClick() {

            }

            @Override
            public void onTooltip() {
                drawTooltipColored("Previous categories", ClientStatic.COLOR_CRAFT_TEXT);
            }

        };

        craftButtonRight = new InteractableUIElement(this, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_ARROW_RIGHT, craftArrowRightS, craftArrowRight) {

            @Override
            public void onLeftClick() {

            }

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

        changeUiScale(2.0f);
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
        RenderContext r = RenderContext.get();
        drawTooltipColored((int) r.mouseX, (int) r.mouseY, text, color, extraLines);
    }

    public void drawTooltipCraftingRecipe(CraftingRecipe recipe) {
        RenderContext r = RenderContext.get();
        int x = (int) r.mouseX;
        int y = (int) r.mouseY;

        x += 4 * uiScale;
        y += 4 * uiScale;

        ItemMapping mapping = ItemMapper.get().getMapping(recipe.outputId);
        String outputText = recipe.outputAmount + "x " + mapping.displayName;

        glyphLayout.setText(m6x11_use, outputText);
        float titleWidth = glyphLayout.width;

        float innerWidth = 8 * uiScale + titleWidth;

        glyphLayout.setText(m5x7_use, "Ingredients:");
        float ingredientsWidth = glyphLayout.width + 8 * uiScale;
        float generalM5X7Height = glyphLayout.height;

        if(ingredientsWidth > innerWidth) innerWidth = (ingredientsWidth);
        float maxIngredientRowWidth = 0;

        for(int i = 0; i < recipe.inputIds.length; i++) {
            String ingredientRowText = recipe.inputAmounts[i] + "x " + ItemMapper.get().getMapping(recipe.inputIds[i]).displayName;
            glyphLayout.setText(m5x7_use, ingredientRowText);
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

        r.hudBatch.draw(tooltipFiller, x + 4 * uiScale, y + 4 * uiScale, maxIngredientRowWidth + 7 * uiScale, 43 * uiScale + recipe.inputIds.length * 24 * uiScale + (recipe.inputIds.length - 1) * 1 * uiScale);

        // Draw ingredient rows
        float _cx = x + 7 * uiScale;
        float _cy = y + 7 * uiScale;
        float _coy = 0;

        for(int i = 0; i < recipe.inputIds.length; i++) {
            r.hudBatch.draw(tooltipFillerCrafting, _cx, _cy + _coy, maxIngredientRowWidth, 24 * uiScale);

            int id = recipe.inputIds[i];
            int am = recipe.inputAmounts[i];
            boolean hasIngredient = ClientPlayer.getLocalPlayer().playerInventory.hasItem(id, am);

            ItemMapping m = ItemMapper.get().getMapping(id);
            TextureRegion ingredientTexture = m.uiRender.textureRegion;

            float centeredTextureX = (24 - ingredientTexture.getRegionWidth()) * 0.5f * uiScale;
            float centeredTextureY = (24 - ingredientTexture.getRegionHeight()) * 0.5f * uiScale;

            r.hudBatch.draw(ingredientTexture, _cx + centeredTextureX, _cy + _coy + centeredTextureY, ingredientTexture.getRegionWidth() * uiScale, ingredientTexture.getRegionHeight() * uiScale);

            String ingredientText = am + "x " + m.displayName;
            glyphLayout.setText(m5x7_use, ingredientText);
            float th = glyphLayout.height;

            m5x7_use.setColor(hasIngredient ? ClientStatic.COLOR_CRAFT_GREEN : ClientStatic.COLOR_CRAFT_RED);
            m5x7_use.draw(r.hudBatch, ingredientText, _cx + 24 * uiScale, _cy + _coy + th + (24 * uiScale - th) * 0.5f);
            m5x7_use.setColor(Color.WHITE);

            _coy += 24 * uiScale + 1 * uiScale;
        }

        float _iy = _cy + _coy + 5 * uiScale;

        // Ingredients: text
        m5x7_use.setColor(ClientStatic.COLOR_CRAFT_INGREDIENTS);
        m5x7_use.draw(r.hudBatch, "Ingredients:", _cx, _iy + generalM5X7Height);
        m5x7_use.setColor(Color.WHITE);

        // Header line
        float headerY = _iy + generalM5X7Height + 11 * uiScale;
        glyphLayout.setText(m6x11_use, outputText);
        m6x11_use.draw(r.hudBatch, outputText, _cx, headerY + glyphLayout.height);

        float endY = headerY + glyphLayout.height + 4 * uiScale;
        drawBorderAt(r, x, y, maxIngredientRowWidth + 2 * uiScale, endY - y - 9 * uiScale);

        // Divider line
        r.hudBatch.draw(tooltipFillerLight, x + 3 * uiScale, _iy + generalM5X7Height + 4 * uiScale, maxIngredientRowWidth + 8 * uiScale, 2 * uiScale);
    }

    public void drawTooltipColored(int x, int y, String text, Color color, String... extraLines) {
        x += 4 * uiScale; // offset
        y += 4 * uiScale; // offset

        glyphLayout.setText(m5x7_use, text);
        float tw = glyphLayout.width;

        if(extraLines.length > 0) {
            for(String str : extraLines) {
                glyphLayout.setText(m5x7_use, str);
                if(glyphLayout.width > tw) tw = glyphLayout.width;
            }
        }

        float th = glyphLayout.height;
        float titleHeight = glyphLayout.height;

        if(extraLines.length > 0) {
            th += 9 * uiScale;

            for(int i = 0; i < extraLines.length; i++) {
                glyphLayout.setText(m5x7_use, extraLines[i]);
                th += glyphLayout.height;
                if(i > 0) th += 4 * uiScale;
            }
        }

        float cornerSize = tooltipBottomLeft.getRegionWidth() * uiScale;
        float borderSize = 1 * uiScale * 2;
        tw += borderSize;
        th += borderSize;

        RenderContext r = RenderContext.get();

        r.hudBatch.draw(tooltipFiller, x + 4 * uiScale, y + 4 * uiScale, tw + 4 * uiScale, th + 4 * uiScale);

        drawBorderAt(r, x, y, tw, th);

        m5x7_use.setColor(color);
        m5x7_use.draw(r.hudBatch, text, x + cornerSize + uiScale, y + cornerSize + th - uiScale);
        m5x7_use.setColor(Color.WHITE);

        if(extraLines.length > 0) {
            r.hudBatch.draw(tooltipFillerLight, x + cornerSize + uiScale, y + cornerSize + th - uiScale - titleHeight - 5 * uiScale, tw, uiScale);

            for(int i = 0; i < extraLines.length; i++) {
                String c = extraLines[i];
                m5x7_use.draw(r.hudBatch, c, x + cornerSize + uiScale, y + cornerSize + th - uiScale - titleHeight - 9 * uiScale - i * (4 * uiScale + titleHeight));
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
        RenderContext r = RenderContext.get();

        boolean previousInventoryOpenState = inventoryOpenState;
        boolean previousCraftingOpenState = craftingOpenState;
        inventoryOpenState = playerPresent() && ClientPlayer.getLocalPlayer().inventoryOpen;
        craftingOpenState = inventoryOpenState && craftingOpen;

        if(previousInventoryOpenState != inventoryOpenState) {
            // Update hotbar position.
            updateHotbarPosition();
            updateSlotVisibility();
        } else if(previousCraftingOpenState != craftingOpenState) {
            updateSlotVisibility();
        }

        if(r.mouseMoved || (previousInventoryOpenState != inventoryOpenState)) {
            updateInventoryElements();
        }

        playerMinimap.updateMinimap();

        if(fadeInDelta < fadeInDuration) {
            fadeInDelta += r.delta;
            if(fadeInDelta > fadeInDuration) fadeInDelta = fadeInDuration;
        }

        fadeRainTarget = ExpoClientContainer.get().getClientWorld().worldWeather == Weather.RAIN.WEATHER_ID ? 1.0f : 0.0f;

        if(fadeRainDelta != fadeRainTarget) {
            if(fadeRainDelta < fadeRainTarget) {
                fadeRainDelta += r.delta / fadeRainDuration;
                if(fadeRainDelta > fadeRainTarget) fadeRainDelta = fadeRainTarget;
            } else {
                fadeRainDelta -= r.delta / fadeRainDuration;
                if(fadeRainDelta < fadeRainTarget) fadeRainDelta = fadeRainTarget;
            }
        }
    }

    public void setFade(float duration) {
        fadeInDelta = 0;
        fadeInDuration = duration;
    }

    private boolean uiElementInBounds(InteractableUIElement slot) {
        RenderContext r = RenderContext.get();
        return r.mouseX >= slot.x && r.mouseX < slot.ex && r.mouseY >= slot.y && r.mouseY < slot.ey;
    }

    public void render() {
        RenderContext r = RenderContext.get();

        r.hudBatch.begin();

        {
            if(fadeRainDelta != 0) {
                Color COLOR_RAIN = ExpoClientContainer.get().getClientWorld().COLOR_RAIN;
                r.hudBatch.setColor(COLOR_RAIN.r, COLOR_RAIN.g, COLOR_RAIN.b, 0.05f * fadeRainDelta);
                r.hudBatch.draw(whiteSquare, -1, -1, Gdx.graphics.getWidth() + 2, Gdx.graphics.getHeight() + 2);
                r.hudBatch.setColor(Color.WHITE);
            }
        }

        if(PlayerInventory.LOCAL_INVENTORY == null) {
            r.hudBatch.end();
            return;
        }

        // Draw player names
        if(DEV_MODE || ExpoServerBase.get() != null) {
            var players = ClientEntityManager.get().getEntitiesByType(ClientEntityType.PLAYER);

            for(ClientEntity entity : players) {
                ClientPlayer player = (ClientPlayer) entity;
                BitmapFont useFont = m5x7_border_use;
                glyphLayout.setText(useFont, player.username);

                Vector2 hudPos = ClientUtils.entityPosToHudPos(player.clientPosX + 5, player.clientPosY + 32);
                useFont.draw(r.hudBatch, player.username, (int) hudPos.x - glyphLayout.width * 0.5f, (int) hudPos.y + glyphLayout.height);
            }
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
            r.hudBatch.draw(craftingOpen ? invBackgroundCrafting : invBackground, invX, invY, invW, invH);

            // Draw inventory slots
            drawSlots(hotbarSlots, inventorySlots, inventoryArmorSlots);
            if(craftOpenSlot.visible) craftOpenSlot.drawBase();
            if(craftButtonLeft.visible) craftButtonLeft.drawBase();
            if(craftButtonRight.visible) craftButtonRight.drawBase();
            for(var el : craftCategorySlots) if(el.visible) el.drawBase();
            for(var el : craftRecipeSlots) if(el.visible) el.drawBase();

            // Draw inventory text [244]
            glyphLayout.setText(m5x7_shadow_use, "Inventory");
            float invTextOffsetX = ((244 * uiScale) - glyphLayout.width) * 0.5f;
            float invTextOffsetY = glyphLayout.height + (craftingOpen ? 15 * uiScale : 0) + 156 * uiScale;
            m5x7_shadow_use.draw(r.hudBatch, "Inventory", invX + 35 * uiScale + invTextOffsetX, invY + invTextOffsetY);

            // Draw Crafting text
            if(craftingOpen) {
                glyphLayout.setText(m5x7_shadow_use, "Crafting");
                float cTextOffsetX = (150 * uiScale - glyphLayout.width) * 0.5f;
                m5x7_shadow_use.draw(r.hudBatch, "Crafting", invX + 278 * uiScale + cTextOffsetX, invY + 199 * uiScale + glyphLayout.height);
            }
        } else {
            drawHotbar(r);
            playerMinimap.draw(r);
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

        if(DEV_MODE || ExpoServerBase.get() == null) {
            chat.draw();
        }

        {
            // World enter animation black fade out
            if(fadeInDelta != fadeInDuration) {
                r.hudBatch.begin();
                r.hudBatch.setColor(0.0f, 0.0f, 0.0f, 1.0f - fadeInDelta / fadeInDuration);
                r.hudBatch.draw(whiteSquare, -1, -1, Gdx.graphics.getWidth() + 2, Gdx.graphics.getHeight() + 2);
                r.hudBatch.setColor(Color.WHITE);
                r.hudBatch.end();
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

            long ping = map.get(username);

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
        glyphLayout.setText(m5x7_border_use, text);

        m5x7_border_use.setColor(rgb[0], rgb[1], rgb[2], 1.0f);
        m5x7_border_use.draw(rc.hudBatch, text, x + (32 * uiScale - glyphLayout.width) * 0.5f, y + glyphLayout.height + (11 * uiScale - glyphLayout.height) * 0.5f);
    }

    public void updateInventoryBounds() {
        TextureRegion bgTex = craftingOpen ? invBackgroundCrafting : invBackground;
        invW = bgTex.getRegionWidth() * uiScale;
        invH = bgTex.getRegionHeight() * uiScale;

        invX = center(invW);
        invY = centerY(invH);
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

        m5x7_use = RenderContext.get().m5x7_all[(int) uiScale - 1];
        m6x11_use = RenderContext.get().m6x11_all[(int) uiScale - 1];

        m5x7_border_use = RenderContext.get().m5x7_border_all[(int) uiScale - 1];
        m6x11_border_use = RenderContext.get().m6x11_border_all[(int) uiScale - 1];

        m5x7_shadow_use = RenderContext.get().m5x7_shadow_all[(int) uiScale - 1];

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