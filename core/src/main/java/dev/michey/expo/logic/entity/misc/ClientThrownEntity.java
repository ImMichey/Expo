package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;

public class ClientThrownEntity extends ClientEntity implements ReflectableEntity {

    private int thrownItemId;
    private Vector2 originPos;
    private Vector2 dstPos;
    private Vector2 normPos;
    private float thrownProgress;
    private float thrownSpeed;

    private ItemRender[] ir;
    private Vector2[] pathCache;
    private float heightOffset;

    @Override
    public void onCreation() {
        ir = ItemMapper.get().getMapping(thrownItemId).thrownRender;

        TextureRegion use = ir[0].useTextureRegion;
        updateTextureBounds(use.getRegionWidth(), use.getRegionHeight() + 32, 0, 0);
        drawReflection = true;
    }

    @Override
    public void onDeletion() {
        ParticleSheet.Common.spawnThrownDustParticles(this, heightOffset);

        if(thrownItemId == ItemMapper.get().getMapping("item_bomb").id || thrownItemId == ItemMapper.get().getMapping("item_nuke").id) { // Bomb
            CameraShake.invoke(5.0f, 0.6f, new Vector2(clientPosX, clientPosY));
            playEntitySound("explosion");

            ParticleSheet.Common.spawnExplosionParticles(this);
            ParticleSheet.Common.spawnGoreParticles(ir[0].useTextureRegion, clientPosX, clientPosY);
        }
    }

    @Override
    public void tick(float delta) {
        thrownProgress += delta * thrownSpeed;
        if(thrownProgress > 1) thrownProgress = 1f;

        clientPosX = originPos.x + normPos.x * thrownProgress;
        clientPosY = originPos.y + normPos.y * thrownProgress;

        updateTexturePositionData();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth();

            rc.useArrayBatch();
            rc.useRegularArrayShader();

            float place = pathCache.length * thrownProgress;
            int firstPlace = (int) place;
            if(firstPlace == pathCache.length) firstPlace = pathCache.length - 1;

            Vector2 first = pathCache[firstPlace];
            Vector2 second;

            if(firstPlace == (pathCache.length - 1)) {
                second = first;
            } else {
                second = pathCache[firstPlace + 1];
            }

            float t = place - firstPlace;
            heightOffset = (first.y + (second.y - first.y) * t) * 64;

            for(ItemRender r : ir) {
                // 90 -> 0 -> -90
                float sign = normPos.x < 0 ? -1 : 1;
                float rotation = sign * 80f - 160f * Interpolation.slowFast.apply(thrownProgress) * sign;

                rc.arraySpriteBatch.draw(r.useTextureRegion,
                        finalDrawPosX + r.offsetX,
                        finalDrawPosY + r.offsetY + heightOffset,
                        r.useTextureRegion.getRegionWidth() * 0.5f,
                        r.useTextureRegion.getRegionHeight() * 0.5f,
                        r.useTextureRegion.getRegionWidth(),
                        r.useTextureRegion.getRegionHeight(),
                        1.0f,
                        1.0f,
                        rotation);
            }
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        for(ItemRender r : ir) {
            rc.arraySpriteBatch.draw(r.useTextureRegion,
                    finalDrawPosX - r.offsetX,
                    finalDrawPosY - r.offsetY - heightOffset,
                    r.useTextureRegion.getRegionWidth(),
                    r.useTextureRegion.getRegionHeight() * -1);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffset(finalTextureStartX, finalTextureStartY, 0, heightOffset);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(ir[0].useTextureRegion, shadow);

        if(rc.verticesInBounds(vertices)) {
            rc.arraySpriteBatch.drawGradient(ir[0].useTextureRegion, ir[0].useWidth, ir[0].useHeight, shadow);
        }
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        thrownItemId = (int) payload[0];
        originPos = new Vector2((float) payload[1], (float) payload[2]);
        dstPos = new Vector2((float) payload[3], (float) payload[4]);
        thrownProgress = (float) payload[5];
        thrownSpeed = (float) payload[6];
        normPos = dstPos.cpy().sub(originPos);

        Vector2[] controlPoints = new Vector2[] {
                new Vector2(0, 0),
                new Vector2(0, 1f),
                new Vector2(1f, 0)
        };
        var bezier = new Bezier<>(controlPoints);

        int PRECISION = 60;
        pathCache = new Vector2[PRECISION];

        for(int i = 0; i < PRECISION; i++) {
            pathCache[i] = new Vector2();
            float x = i / (float) PRECISION;
            bezier.valueAt(pathCache[i], x);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.THROWN_ENTITY;
    }

}