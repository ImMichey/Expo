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
        texture = ExpoAssets.get().textureRegion("tile_not_set");
        updateTexture(0, 0, texture.getRegionWidth(), texture.getRegionHeight());
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        updateCenterAndRoot();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        drawnLastFrame = rc.inDrawBounds(this);

        if(drawnLastFrame) {
            updateDepth();

            rc.useRegularBatch();
            rc.batch.draw(texture, clientPosX, clientPosY);
        }
    }

    /*
    @Override
    public float[] interactionPoints() {
        return new float[] {
                clientPosX, clientPosY,
                clientPosX + drawWidth, clientPosY,
                clientPosX, clientPosY + drawHeight,
                clientPosX + drawWidth, clientPosY + drawHeight,
        };
    }
    */

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.DUMMY;
    }

}
