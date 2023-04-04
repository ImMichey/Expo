package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;

public class ClientItem extends ClientEntity {

    public int itemId;
    public int itemAmount;

    public TextureRegion texture;

    @Override
    public void onCreation() {
        texture = ItemMapper.get().getMapping(itemId).uiRender.textureRegion;
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
        updateDepth();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        rc.useArrayBatch();
        if(rc.arraySpriteBatch.getShader() != rc.DEFAULT_GLES3_ARRAY_SHADER) rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);

        rc.arraySpriteBatch.draw(texture, clientPosX, clientPosY);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ITEM;
    }

}
