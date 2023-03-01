package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;

public class ClientDummy extends ClientEntity {

    private TextureRegion texture;

    @Override
    public void onCreation() {
        texture = ExpoAssets.get().soil;
        updateTexture(0, 0, texture.getRegionWidth(), texture.getRegionHeight());
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        updateCenterAndRoot();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        updateDepth();
        rc.useBatchAndShader(rc.batch, rc.DEFAULT_GLES3_SHADER);
        rc.currentBatch.draw(texture, clientPosX, clientPosY);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.DUMMY;
    }

}
