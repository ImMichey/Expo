package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.SquishAnimator2D;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.flora.ServerOakTree;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.util.ParticleBuilder;

public class ClientFallingTree extends ClientEntity implements ReflectableEntity {

    private TextureRegion treeTrunk;
    private TextureRegion treeLeaves;

    public boolean fallingRightDirection;
    public float leavesDisplacement;

    public float animationDelta;
    public int trunkVariant;
    private float rotation;
    public float colorDisplacement;
    public float windDisplacement;
    public float windDisplacementBase;
    public float transparency;
    public float leavesOffsetY;

    public boolean emptyCrown;

    public float windDisplacementAlpha;
    public float windDisplacementInterpolated;

    private static final float PHASE_TOTAL_DURATION = 4.3f;
    public int wakeupId;

    public SquishAnimator2D inheritedSquishAnimator;

    @Override
    public void onCreation() {
        if(trunkVariant == 0) {
            trunkVariant = 1;
        }

        treeTrunk = tr("oak_trunk_falling_" + trunkVariant);
        treeLeaves = tr("oak_leaves_reg_" + trunkVariant);

        updateTextureBounds(treeLeaves);

        playEntitySound("falling_tree");

        // wake up parent
        ClientOakTree tree = (ClientOakTree) entityManager().getEntityById(wakeupId);

        if(tree != null) {
            tree.wakeup();
        }
    }

    @Override
    public void onDeletion() {
        CameraShake.invoke(5.0f, 0.6f, new Vector2(clientPosX, clientPosY));

        float reach = ClientOakTree.MATRIX[trunkVariant - 1][1];
        int dir = fallingRightDirection ? 1 : -1;

        if(!emptyCrown && GameSettings.get().enableParticles) {
            new ParticleBuilder(ClientEntityType.PARTICLE_OAK_LEAF)
                    .amount(32, 64)
                    .scale(0.6f, 1.1f)
                    .lifetime(0.6f, 2.5f)
                    .position(clientPosX + ((leavesOffsetY + leavesDisplacement - 10) * dir), clientPosY - 10)
                    .offset((reach) * dir, 1f)
                    .velocity(-32, 32, 24, 80)
                    .fadeout(0.25f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .depth(depth - 0.01f)
                    .spawn();
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        animationDelta += delta;

        if(animationDelta >= ServerOakTree.FALLING_ANIMATION_DURATION) {
            animationDelta = ServerOakTree.FALLING_ANIMATION_DURATION;
            entityManager().removeEntity(this);
        }

        if(windDisplacement != 0) {
            float SPEED = 0.5f;
            windDisplacementAlpha += delta * SPEED;
            if(windDisplacementAlpha >= 1.0f) {
                windDisplacementAlpha = 1.0f;
            }
            windDisplacementInterpolated = Interpolation.circle.apply(windDisplacementAlpha);
            windDisplacement = windDisplacementBase - windDisplacementBase * windDisplacementInterpolated;
        }

        if(transparency < 1) {
            transparency += delta * 0.5f;
        }
    }

    @Override
    public void calculateReflection() {
        drawReflection = true;
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        float MAX_ROTATION = 100.0f;

        float negation = fallingRightDirection ? -1 : 1;
        float percentage = animationDelta / PHASE_TOTAL_DURATION;
        float interpolated = schizoInterpolation(percentage);
        rotation = (MAX_ROTATION) * negation * interpolated;

        if(percentage >= 0.75f) {
            interpolated = 0.75f + Interpolation.pow4In.apply((percentage - 0.75f) * 4);
        } else {
            interpolated = percentage;
        }

        rotation = (MAX_ROTATION) * negation * interpolated;

        float isaX = inheritedSquishAnimator.squishX;
        float isaY = inheritedSquishAnimator.squishY;

        rc.arraySpriteBatch.draw(treeTrunk,
                clientPosX - treeTrunk.getRegionWidth() * 0.5f - isaX * 0.5f,
                clientPosY - 18, treeTrunk.getRegionWidth() * 0.5f, 0,
                treeTrunk.getRegionWidth() + isaX, treeTrunk.getRegionHeight() + isaY, 1.0f, -1.0f, -rotation);

        if(!emptyCrown) {
            rc.arraySpriteBatch.setColor(1.0f - colorDisplacement, 1.0f, 1.0f - colorDisplacement, transparency);

            Vector2 offset = GenerationUtils.circular(rotation, 1);

            float leavesX = clientPosX - offset.y * treeTrunk.getRegionHeight();
            float leavesY = clientPosY - 18 - offset.x * treeTrunk.getRegionHeight();

            float offsetX = 10 - leavesDisplacement - leavesOffsetY + treeTrunk.getRegionHeight() - isaY * 0.5f;
            float offsetY = treeLeaves.getRegionWidth() * -0.5f - isaX * 0.5f;

            leavesX += (offset.y * offsetX) + (offset.x * offsetY);
            leavesY -= -(offset.x * offsetX) + (offset.y * offsetY);

            rc.arraySpriteBatch.drawCustomVertices(treeLeaves,
                    leavesX,
                    leavesY,
                    0, 0,
                    treeLeaves.getRegionWidth() + isaX, treeLeaves.getRegionHeight() + isaY, 1.0f, -1.0f, -rotation, windDisplacement, windDisplacement);

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    // \log\left(x^{2}\ +\ 1\right)\ +\ x^{25}
    // \log\left(\left(x^{2}\right)+1\right)+x^{25}
    private static Interpolation pow10In = new Interpolation.PowIn(12);

    private static float schizoInterpolation(float percentage) {
        float lg = (float) Math.log(percentage * percentage * percentage);
        if(lg <= 0) lg = 0;
        return (float) (lg + Math.pow(percentage, 9));
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = true;

        rc.useArrayBatch();
        rc.useRegularArrayShader();

        float MAX_ROTATION = 100.0f;

        float negation = fallingRightDirection ? -1 : 1;
        float percentage = animationDelta / PHASE_TOTAL_DURATION;
        float interpolated = schizoInterpolation(percentage);
        rotation = (MAX_ROTATION) * negation * interpolated;

        float isaX = inheritedSquishAnimator.squishX;
        float isaY = inheritedSquishAnimator.squishY;

        rc.arraySpriteBatch.draw(treeTrunk,
                clientPosX - treeTrunk.getRegionWidth() * 0.5f - isaX * 0.5f,
                clientPosY,
                treeTrunk.getRegionWidth() * 0.5f,
                0,
                treeTrunk.getRegionWidth() + isaX,
                treeTrunk.getRegionHeight() + isaY, 1.0f, 1.0f, rotation);

        if(!emptyCrown) {
            rc.arraySpriteBatch.setColor(1.0f - colorDisplacement, 1.0f, 1.0f - colorDisplacement, transparency);

            Vector2 offset = GenerationUtils.circular(rotation, 1);

            float leavesX = clientPosX - offset.y * treeTrunk.getRegionHeight();
            float leavesY = clientPosY + offset.x * treeTrunk.getRegionHeight();

            float offsetX = 10 - leavesDisplacement - leavesOffsetY + treeTrunk.getRegionHeight() - isaY * 0.5f;
            float offsetY = treeLeaves.getRegionWidth() * -0.5f - isaX * 0.5f;

            leavesX += (offset.y * offsetX) + (offset.x * offsetY);
            leavesY += -(offset.x * offsetX) + (offset.y * offsetY);

            rc.arraySpriteBatch.drawCustomVertices(treeLeaves,
                    leavesX,
                    leavesY,
                    0, 0,
                    treeLeaves.getRegionWidth() + isaX, treeLeaves.getRegionHeight() + isaY, 1.0f, 1.0f, rotation, windDisplacement, windDisplacement);

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        float isaX = inheritedSquishAnimator.squishX;
        float isaY = inheritedSquishAnimator.squishY;

        Affine2 trunkShadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(clientPosX, clientPosY - 10,
                -treeTrunk.getRegionWidth() * 0.5f - isaX * 0.5f, 10 + isaY * 0.5f,
                treeTrunk.getRegionWidth() * 0.5f, 0, rotation);

        Vector2 offset = GenerationUtils.circular(rotation, 1);


        float leavesX = -offset.y * treeTrunk.getRegionHeight();
        float leavesY = offset.x * treeTrunk.getRegionHeight();
        float offsetX = 10 - leavesDisplacement - leavesOffsetY + treeTrunk.getRegionHeight() - isaY * 0.5f;
        float offsetY = treeLeaves.getRegionWidth() * -0.5f - isaX * 0.5f;
        leavesX += (offset.y * offsetX) + (offset.x * offsetY);
        leavesY += -(offset.x * offsetX) + (offset.y * offsetY);

        Affine2 leavesShadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(clientPosX, clientPosY - 10, 0 + leavesX, 10 + leavesY,
                0, 0, rotation);

        rc.useArrayBatch();
        rc.useRegularArrayShader();

        float totalHeight = ClientOakTree.MATRIX[trunkVariant - 1][5];
        float fraction = 1f / totalHeight;
        float trunkDistanceFromGround = 10f;

        {
            // Trunk
            float b = 1f - trunkDistanceFromGround * fraction;
            float t;

            if(emptyCrown) {
                t = 0;
            } else {
                t = 1f - (treeTrunk.getRegionHeight() + trunkDistanceFromGround) * fraction;
            }

            float bc = new Color(0, 0, 0, b).toFloatBits();
            float tc = new Color(0, 0, 0, t).toFloatBits();

            rc.arraySpriteBatch.drawGradientCustomColor(treeTrunk, treeTrunk.getRegionWidth() + isaX, treeTrunk.getRegionHeight() + isaY, trunkShadow, tc, bc);
        }

        {
            if(!emptyCrown) {
                // Leaves
                float b = 0f + ClientOakTree.MATRIX[trunkVariant - 1][1] * fraction;
                float t = 0f;
                float bc = new Color(0, 0, 0, b).toFloatBits();
                float tc = new Color(0, 0, 0, t).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomColor(treeLeaves, treeLeaves.getRegionWidth() + isaX, treeLeaves.getRegionHeight() + isaY, leavesShadow, tc, bc);
            }
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.FALLING_TREE;
    }

}