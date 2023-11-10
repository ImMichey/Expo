package dev.michey.expo.logic.entity.misc;

import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.visbility.TopVisibilityEntity;

public class ClientHealthBar extends ClientEntity implements TopVisibilityEntity {

    @Override
    public void onCreation() {

    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(RenderContext rc, float delta) {

    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.HEALTH_BAR;
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {

    }

}
