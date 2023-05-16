package dev.michey.expo.logic.entity.particle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.ClientParticle;
import dev.michey.expo.render.RenderContext;

public class ClientParticleOakLeaf extends ClientParticle {

    private TextureRegion leaf;

    @Override
    public void onCreation() {
        leaf = tr("eotp_" + MathUtils.random(1, 2));

        float sx = leaf.getRegionWidth() * scaleX;
        float sy = leaf.getRegionHeight() * scaleY;

        updateTextureBounds(sx * 2, sy * 2, 0, 0, -sx, -sy);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        updateTexturePositionData();
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            if(rotationSpeed > 0) {
                rotation += delta * rotationSpeed;
            }

            rc.arraySpriteBatch.setColor(r, g, b, useAlpha);
            rc.arraySpriteBatch.draw(leaf, clientPosX, clientPosY, 0, 0, leaf.getRegionWidth(), leaf.getRegionHeight(), scaleX, scaleY, rotation);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.PARTICLE_OAK_LEAF;
    }

}
