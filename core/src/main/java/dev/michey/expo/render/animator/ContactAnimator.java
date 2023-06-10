package dev.michey.expo.render.animator;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;

import java.util.LinkedList;
import java.util.List;

public class ContactAnimator {

    private final ClientEntity parent;
    private final List<ClientEntityType> contactList;
    private float contactDelta = 0f;
    private float contactDir = 0f;
    private final float SPEED = 3.5f;
    private final float STRENGTH = 3.5f;
    private final float STRENGTH_DECREASE = 0.7f;
    private final int STEPS = 5; // step = 0.5f (half radiant)
    private float useStrength = STRENGTH;

    // Export value
    public float value;
    public float squish = 1.0f;
    public float squishDelta;
    public boolean doSquish = false;
    public float squishAdjustment = 0.0f;
    public boolean small = true;

    public static final LinkedList<ClientEntityType> WALKING_ENTITY_LIST;

    static {
        WALKING_ENTITY_LIST = new LinkedList<>();
        WALKING_ENTITY_LIST.add(ClientEntityType.PLAYER);
    }

    public ContactAnimator(ClientEntity parent, List<ClientEntityType> contactList) {
        this.parent = parent;
        this.contactList = contactList;
    }

    public ContactAnimator(ClientEntity parent) {
        this.parent = parent;
        this.contactList = WALKING_ENTITY_LIST;
    }

    public void tick(float delta) {
        if(small) {
            if(contactDelta == 0 && parent.visibleToRenderEngine) {
                for(List<ClientEntity> list : parent.entityManager().getEntitiesByType(contactList)) {
                    for(ClientEntity entity : list) {
                        if(!entity.isMoving()) continue;
                        float xDst = parent.dstRootX(entity);
                        float yDst = parent.dstRootY(entity);

                        if(xDst < 6.0f && yDst < 5.0f) {
                            // Contact.
                            parent.playEntitySound("leaves_rustle");
                            contactDelta = STEPS * 0.5f;
                            contactDir = entity.serverDirX == 0 ? (entity.finalTextureCenterX < parent.finalTextureCenterX ? 1 : -1) : (entity.serverDirX < 0 ? -1 : 1);

                            doSquish = true;
                            squishDelta = 0.0f;
                        }
                    }
                }
            }

            if(doSquish) {
                float MIN_SQUISH = 0.3334f;
                float SQUISH_SPEED = 1.2f;
                squishDelta += delta * SQUISH_SPEED;

                if(squishDelta >= 1.0f) {
                    squishDelta = 1.0f;
                    doSquish = false;
                }

                squish = MIN_SQUISH + Interpolation.bounceOut.apply(squishDelta) * (1.0f - MIN_SQUISH);
                squishAdjustment = squish != 1.0f ? (1f - squish) : 0f;
            }
        }

        if(contactDelta != 0) {
            contactDelta -= delta * SPEED;
            if(contactDelta < 0f) contactDelta = 0f;

            float full = STEPS * 0.5f;
            float diff = full - contactDelta;
            int decreases = (int) (diff / 0.5f);
            useStrength = STRENGTH - STRENGTH_DECREASE * decreases;

            value = useStrength * contactDir * MathUtils.sin(((STEPS * 0.5f) - contactDelta) * MathUtils.PI2);
        } else {
            value = 0;
        }
    }

    public void onContact() {
        contactDelta = STEPS * 0.5f;
        contactDir = 1;
    }

}
