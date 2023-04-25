package dev.michey.expo.render.animator;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;

import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

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

    public ContactAnimator(ClientEntity parent, List<ClientEntityType> contactList) {
        this.parent = parent;
        this.contactList = contactList;
    }

    public ContactAnimator(ClientEntity parent) {
        this.parent = parent;
        this.contactList = new LinkedList<>();
        this.contactList.add(ClientEntityType.PLAYER);
    }

    public void tick(float delta) {
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
                        contactDir = entity.serverDirX == 0 ? (entity.drawRootX < parent.drawRootX ? 1 : -1) : (entity.serverDirX < 0 ? -1 : 1);
                    }
                }
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