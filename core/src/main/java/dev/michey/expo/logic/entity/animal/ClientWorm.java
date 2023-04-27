package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;
import org.w3c.dom.Text;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientWorm extends ClientEntity {

    private Array<TextureRegion> idleAnimation;
    private Array<TextureRegion> walkAnimation;
    private TextureRegion drawCurrentFrame;
    private float animationDelta;

    private boolean cachedMoving;
    private boolean flipped;

    @Override
    public void onCreation() {
        idleAnimation = ta("entity_worm_idle", 3);
        walkAnimation = ta("entity_worm_walk", 5);
        drawCurrentFrame = idleAnimation.get(0);
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

        if(cachedMoving != isMoving()) {
            cachedMoving = !cachedMoving;
            animationDelta = 0;
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            animationDelta += delta;
            boolean flip = (!flipped && serverDirX == 0) || (flipped && serverDirX == 1);
            int index = (int) (animationDelta / 0.15f) % (cachedMoving ? 5 : 3);

            if(flip) {
                for(TextureRegion t : idleAnimation) t.flip(true, false);
                for(TextureRegion t : walkAnimation) t.flip(true, false);
                flipped = !flipped;
            }

            drawCurrentFrame = cachedMoving ? walkAnimation.get(index) : idleAnimation.get(index);
            updateTexture(0, 0, drawCurrentFrame.getRegionWidth(), drawCurrentFrame.getRegionHeight());

            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(drawCurrentFrame, clientPosX, clientPosY, drawWidth, drawHeight);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX, clientPosY);
        float[] mushroomVertices = rc.arraySpriteBatch.obtainShadowVertices(drawCurrentFrame, shadow);
        boolean drawMushroom = rc.verticesInBounds(mushroomVertices);

        if(drawMushroom) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradient(drawCurrentFrame, drawWidth, drawHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.WORM;
    }

}
