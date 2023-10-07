package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.util.GenerationUtils;

public class ClientCloud extends ClientEntity {

    private TextureRegion cloudTexture;
    private Vector2 dir;

    @Override
    public void onCreation() {
        cloudTexture = tr("entity_cloud_1");
        updateTextureBounds(cloudTexture);

        dir = GenerationUtils.circularRandom(32f);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        clientPosX += dir.x * delta;
        clientPosY += dir.y * delta;
    }

    @Override
    public void render(RenderContext rc, float delta) {

    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.drawGradientCloud(cloudTexture, clientPosX, clientPosY, 0.4f);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CLOUD;
    }

}
