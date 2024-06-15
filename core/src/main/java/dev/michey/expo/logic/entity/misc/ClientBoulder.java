package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.world.clientphysics.ClientPhysicsBody;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.SquishAnimator2D;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;

import static dev.michey.expo.server.main.logic.entity.misc.ServerBoulder.*;

public class ClientBoulder extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private int variant;
    private TextureRegion texture;
    private TextureRegion shadowMask;
    private float[] interactionPointArray;
    private TextureRegion selectionTexture;

    private ClientPhysicsBody physicsBody;

    private final SquishAnimator2D squishAnimator2D = new SquishAnimator2D(0.2f, 1.5f, 1.5f);

    @Override
    public void onCreation() {
        String texName = "entity_boulder";

        if(variant == VARIANT_COAL || variant == (VARIANT_COAL + 1)) {
            texName += "_coal_" + (variant - VARIANT_COAL + 1);
        } else if(variant == VARIANT_IRON || variant == (VARIANT_IRON + 1)) {
            texName += "_iron_" + (variant - VARIANT_IRON + 1);
        } else {
            texName += "_" + (variant - VARIANT_REG + 1);
        }

        texture = tr(texName);
        shadowMask = texture;
        selectionTexture = generateSelectionTexture(texture);
        updateTextureBounds(texture);
        interactionPointArray = generateInteractionArray(2);

        float[] b = ROCK_BODIES[variant - 1];
        physicsBody = new ClientPhysicsBody(this, b[0], b[1], b[2], b[3]);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("stone_hit");
        squishAnimator2D.reset();

        ParticleSheet.Common.spawnBoulderHitParticles(this, variant == 2);

        if(selected && newHealth <= 0) {
            ParticleSheet.Common.spawnDustHitParticles(this);
            CameraShake.invoke(1.0f, 0.33f);
        }
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("stone_break");
        }

        physicsBody.dispose();
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues();
        squishAnimator2D.calculate(delta);

        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX - squishAnimator2D.squishX * 0.5f, finalSelectionDrawPosY,
                selectionTexture.getRegionWidth() + squishAnimator2D.squishX, selectionTexture.getRegionHeight() + squishAnimator2D.squishY);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            squishAnimator2D.calculate(delta);

            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.draw(texture, finalDrawPosX - squishAnimator2D.squishX * 0.5f, finalDrawPosY,
                    texture.getRegionWidth() + squishAnimator2D.squishX, texture.getRegionHeight() + squishAnimator2D.squishY);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(texture, finalDrawPosX - squishAnimator2D.squishX * 0.5f, finalDrawPosY + 2,
                texture.getRegionWidth() + squishAnimator2D.squishX, (texture.getRegionHeight() + squishAnimator2D.squishY) * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX - squishAnimator2D.squishX * 0.5f, finalTextureStartY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(shadowMask, shadow);
        boolean draw = rc.verticesInBounds(vertices);

        if(draw) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradient(shadowMask, textureWidth + squishAnimator2D.squishX, textureHeight + squishAnimator2D.squishY, shadow);
        }
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.35f, 0.4f, 0, 1.0f);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BOULDER;
    }

}