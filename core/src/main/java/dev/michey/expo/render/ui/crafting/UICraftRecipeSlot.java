package dev.michey.expo.render.ui.crafting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.InteractableUIElement;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipe;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;

public class UICraftRecipeSlot extends InteractableUIElement {

    private final UIContainerInventory parentContainer;

    private CraftingRecipe holdingRecipe;
    private ItemRender[] drawItemPreview;

    public UICraftRecipeSlot(UIContainerInventory parentContainer) {
        super(ExpoShared.CONTAINER_ID_PLAYER, ExpoShared.PLAYER_INVENTORY_SLOT_CRAFT_RECIPE_BASE,
                ExpoAssets.get().textureRegion("ui_crafting_emptyslot_sel"), ExpoAssets.get().textureRegion("ui_crafting_emptyslot"));

        this.parentContainer = parentContainer;
    }

    public void setHoldingRecipe(CraftingRecipe recipe) {
        this.holdingRecipe = recipe;
        if(recipe != null) drawItemPreview = ItemMapper.get().getMapping(recipe.outputId).uiRender;
    }

    @Override
    public void onLeftClick() {
        if(holdingRecipe != null) ClientPackets.p35PlayerCraft(holdingRecipe.recipeIdentifier, Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT));
    }

    @Override
    public void onTooltip() {
        if(holdingRecipe != null) drawTooltipCraftingRecipe(holdingRecipe);
    }

    public void drawTooltipCraftingRecipe(CraftingRecipe recipe) {
        PlayerUI ui = PlayerUI.get();
        RenderContext rc = RenderContext.get();
        int x = (int) rc.mouseX;
        int y = (int) rc.mouseY;

        x += (int) (4 * ui.uiScale);
        y += (int) (4 * ui.uiScale);

        ItemMapping mapping = ItemMapper.get().getMapping(recipe.outputId);
        String outputText = recipe.outputAmount + "x " + mapping.displayName;

        ui.glyphLayout.setText(rc.m6x11_use, outputText);
        float titleWidth = ui.glyphLayout.width;

        float innerWidth = 8 * ui.uiScale + titleWidth;

        ui.glyphLayout.setText(rc.m5x7_use, "Ingredients:");
        float ingredientsWidth = ui.glyphLayout.width + 8 * ui.uiScale;
        float generalM5X7Height = ui.glyphLayout.height;

        if(ingredientsWidth > innerWidth) innerWidth = (ingredientsWidth);
        float maxIngredientRowWidth = 0;

        for(int i = 0; i < recipe.inputIds.length; i++) {
            String ingredientRowText = recipe.inputAmounts[i] + "x " + ItemMapper.get().getMapping(recipe.inputIds[i]).displayName;
            ui.glyphLayout.setText(rc.m5x7_use, ingredientRowText);
            float ingredientRowWidth = ui.glyphLayout.width + 28 * ui.uiScale;
            if(ingredientRowWidth > innerWidth) innerWidth = ingredientRowWidth;
            if(ingredientRowWidth > maxIngredientRowWidth) maxIngredientRowWidth = ingredientRowWidth;
        }

        if(titleWidth > maxIngredientRowWidth) maxIngredientRowWidth = titleWidth;

        // Check if shift needed
        float totalWidthTooltip = maxIngredientRowWidth + 9 * ui.uiScale + ui.cornerWH;
        float proposedEndX = x + totalWidthTooltip;
        float maxDisplayX = Gdx.graphics.getWidth();

        if(proposedEndX >= maxDisplayX) {
            x -= (int) totalWidthTooltip;
        }

        rc.hudBatch.draw(ui.tooltipFiller, x + 4 * ui.uiScale, y + 4 * ui.uiScale, maxIngredientRowWidth + 7 * ui.uiScale, 43 * ui.uiScale + recipe.inputIds.length * 24 * ui.uiScale + (recipe.inputIds.length - 1) * 1 * ui.uiScale);

        // Draw ingredient rows
        float _cx = x + 7 * ui.uiScale;
        float _cy = y + 7 * ui.uiScale;
        float _coy = 0;

        for(int i = 0; i < recipe.inputIds.length; i++) {
            rc.hudBatch.draw(ui.tooltipFillerCrafting, _cx, _cy + _coy, maxIngredientRowWidth, 24 * ui.uiScale);

            int id = recipe.inputIds[i];
            int am = recipe.inputAmounts[i];
            boolean hasIngredient = ClientPlayer.getLocalPlayer().playerInventory.hasItem(id, am);

            ItemMapping m = ItemMapper.get().getMapping(id);
            rc.drawItemTextures(m.uiRender, _cx, _cy + _coy, 24, 24);

            String ingredientText = am + "x " + m.displayName;
            ui.glyphLayout.setText(rc.m5x7_use, ingredientText);
            float th = ui.glyphLayout.height;

            rc.m5x7_use.setColor(hasIngredient ? ClientStatic.COLOR_CRAFT_GREEN : ClientStatic.COLOR_CRAFT_RED);
            rc.m5x7_use.draw(rc.hudBatch, ingredientText, _cx + 24 * ui.uiScale, _cy + _coy + th + (24 * ui.uiScale - th) * 0.5f);
            rc.m5x7_use.setColor(Color.WHITE);

            _coy += 24 * ui.uiScale + 1 * ui.uiScale;
        }

        float _iy = _cy + _coy + 5 * ui.uiScale;

        // Ingredients: text
        rc.m5x7_use.setColor(ClientStatic.COLOR_CRAFT_INGREDIENTS);
        rc.m5x7_use.draw(rc.hudBatch, "Ingredients:", _cx, _iy + generalM5X7Height);
        rc.m5x7_use.setColor(Color.WHITE);

        // Header line
        float headerY = _iy + generalM5X7Height + 11 * ui.uiScale;
        ui.glyphLayout.setText(rc.m6x11_use, outputText);
        rc.m6x11_use.draw(rc.hudBatch, outputText, _cx, headerY + ui.glyphLayout.height);

        float endY = headerY + ui.glyphLayout.height + 4 * ui.uiScale;
        ui.drawBorderAt(rc, x, y, maxIngredientRowWidth + 2 * ui.uiScale, endY - y - 9 * ui.uiScale);

        // Divider line
        rc.hudBatch.draw(ui.tooltipFillerLight, x + 3 * ui.uiScale, _iy + generalM5X7Height + 4 * ui.uiScale, maxIngredientRowWidth + 8 * ui.uiScale, 2 * ui.uiScale);
    }

    @Override
    public void drawBase() {
        super.drawBase();

        if(holdingRecipe != null) {
            RenderContext r = RenderContext.get();
            PlayerUI ui = PlayerUI.get();

            r.drawItemTextures(drawItemPreview, this.x, this.y, ui.slotW / ui.uiScale, ui.slotH / ui.uiScale);

            int amount = holdingRecipe.outputAmount;
            String amountAsText = String.valueOf(amount);

            ui.glyphLayout.setText(r.m5x7_shadow_use, amountAsText);
            float aw = ui.glyphLayout.width;
            float ah = ui.glyphLayout.height;

            r.m5x7_shadow_use.draw(r.hudBatch, amountAsText, this.ex - aw - 1 * ui.uiScale, this.y + ah + 1 * ui.uiScale);
        }
    }
}
