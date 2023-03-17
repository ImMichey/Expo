package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;

public class ClientSelector extends ClientEntity {

    private TextureRegion selectorTexture;

    @Override
    public void onCreation() {
        selectorTexture = ExpoAssets.get().textureRegion("selector");
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        RenderContext rc = RenderContext.get();

        clientPosX = rc.mouseWorldGridX - 1;
        clientPosY = rc.mouseWorldGridY - 1;
    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void render(RenderContext rc, float delta) {
        updateDepth();

        //rc.useBatchAndShader(rc.arraySpriteBatch, rc.DEFAULT_GLES3_SHADER);
        //rc.currentBatch.draw(selectorTexture, clientPosX - 1, clientPosY - 1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SELECTOR;
    }

}
