package dev.michey.expo.render.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipe;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.util.ClientPackets;

public class InteractableRecipeSlot extends InteractableUIElement {

    private CraftingRecipe holdingRecipe;
    private TextureRegion drawItemPreview;

    public InteractableRecipeSlot(PlayerUI parent, int inventorySlotId) {
        super(parent, inventorySlotId, parent.craftSlotS, parent.craftSlot);
    }

    @Override
    public void onTooltip() {
        // Show crafting ingredients in tooltip
        if(holdingRecipe != null) parent.drawTooltipCraftingRecipe(holdingRecipe);
    }

    public void setHoldingRecipe(CraftingRecipe recipe) {
        this.holdingRecipe = recipe;
        if(recipe != null) drawItemPreview = ItemMapper.get().getMapping(recipe.outputId).uiRender.textureRegion;
    }

    @Override
    public void onLeftClick() {
        // Try crafting
        if(holdingRecipe != null) ClientPackets.p35PlayerCraft(holdingRecipe.recipeIdentifier, Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT));
    }

    @Override
    public void drawBase() {
        super.drawBase();

        if(holdingRecipe != null) {
            RenderContext r = RenderContext.get();
            float iw = drawItemPreview.getRegionWidth() * parent.uiScale;
            float ih = drawItemPreview.getRegionHeight() * parent.uiScale;
            float _x = this.x + (parent.slotW - iw) * 0.5f;
            float _y = this.y + (parent.slotH - ih) * 0.5f;
            r.hudBatch.draw(drawItemPreview, _x, _y, iw, ih);

            int amount = holdingRecipe.outputAmount;
            String amountAsText = amount + "";

            parent.glyphLayout.setText(parent.m5x7_shadow_use, amountAsText);
            float aw = parent.glyphLayout.width;
            float ah = parent.glyphLayout.height;

            parent.m5x7_shadow_use.draw(r.hudBatch, amountAsText, this.ex - aw - 1 * parent.uiScale, this.y + ah + 1 * parent.uiScale);
        }
    }

}
