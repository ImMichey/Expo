package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.SpawnItem;
import dev.michey.expo.util.EntityRemovalReason;

public class ServerFallingTree extends ServerEntity {

    public int variant;
    public boolean fallDirectionRight;
    public float animationDelta;

    public static final float FALLING_ANIMATION_DURATION = 4.25f;

    @Override
    public void onCreation() {
        if(variant == 0) variant = 1;
    }

    @Override
    public void tick(float delta) {
        animationDelta += delta;

        if(animationDelta >= FALLING_ANIMATION_DURATION) {
            killEntityWithPacket(EntityRemovalReason.DEATH);
        }
    }

    @Override
    public void onDie() {
        spawnItemsAround(0.0f, 0.0f, 14.0f, 18.0f,
                new SpawnItem("item_oak_log", 3, 6),
                new SpawnItem("item_acorn", 1, 2)
        );
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.FALLING_TREE;
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {variant, fallDirectionRight, animationDelta};
    }

}
