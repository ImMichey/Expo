package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;

public class ClientBeehive extends ClientEntity {

    private TextureRegion tex;

    @Override
    public void onCreation() {
        tex = ExpoAssets.get().textureRegion("entity_beehive");
        updateTextureBounds(tex);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(tex, finalDrawPosX, finalDrawPosY);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BEEHIVE;
    }

}