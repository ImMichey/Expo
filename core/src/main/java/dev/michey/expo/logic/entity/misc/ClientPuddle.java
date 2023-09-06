package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.reflections.ReflectableEntity;

public class ClientPuddle extends ClientEntity implements ReflectableEntity {

    private ExpoAnimation splashAnimation;
    private TextureRegion currentFrame;
    public boolean upperPart;
    public boolean small;

    @Override
    public void onCreation() {
        splashAnimation = new ExpoAnimation("puddle" + (small ? "_small" : "") + (upperPart ? "_upper" : ""), (small ? 7 : 6), 0.1f);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        splashAnimation.tick(delta);
        currentFrame = splashAnimation.getFrame();
        updateTextureBounds(currentFrame);

        if(splashAnimation.isAnimationFinished()) {
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
        return ClientEntityType.PUDDLE;
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(currentFrame, finalDrawPosX, finalDrawPosY);
    }

}