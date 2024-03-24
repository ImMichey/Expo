package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.visbility.TopVisibilityEntity;

public class ClientCloud extends ClientEntity implements TopVisibilityEntity {

    private float cloudScale;
    private float cloudSpeed;

    private TextureRegion cloudTexture;
    private Vector2 dir;

    private static final float MIN_SPEED = 2f;
    private static final float MAX_SPEED = 20f;

    private float spawnAlpha = 0.0f;

    @Override
    public void onCreation() {
        cloudTexture = new TextureRegion(tr("entity_cloud_" + MathUtils.random(1, 2)));
        cloudTexture.flip(MathUtils.randomBoolean(), MathUtils.randomBoolean());

        cloudScale = MathUtils.random(1.0f, 2.0f);
        updateTextureBoundScaled(cloudTexture, cloudScale, cloudScale);

        cloudSpeed = MathUtils.random(MIN_SPEED, MAX_SPEED);
        dir = new Vector2(0.9f + MathUtils.random(0.2f), 0.9f + MathUtils.random(0.2f)).nor().scl(cloudSpeed);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        spawnAlpha += delta;
        if(spawnAlpha > 1.0f) spawnAlpha = 1.0f;

        clientPosX += dir.x * delta;
        clientPosY += dir.y * delta;
        updateTexturePositionData();

        ClientChunk c = getCurrentChunk();

        if(c == null || !c.visibleLogic) {
            entityManager().removeEntity(this);
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {

    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CLOUD;
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            float alpha = 0.5f + (cloudSpeed - MIN_SPEED) * 0.5f / (MAX_SPEED - MIN_SPEED);
            rc.arraySpriteBatch.drawGradientCloud(cloudTexture, finalDrawPosX, finalDrawPosY, alpha * spawnAlpha, cloudScale, cloudScale);
        }
    }

}