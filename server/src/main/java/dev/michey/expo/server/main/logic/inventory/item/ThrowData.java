package dev.michey.expo.server.main.logic.inventory.item;

import dev.michey.expo.server.util.JsonConverter;
import org.json.JSONObject;

public class ThrowData {

    public float[] projectileBox;
    public float maxThrowDistance;
    public float minThrowDuration;
    public float maxThrowDuration;
    public float explosionRadius;
    public float explosionDamage;
    public float knockbackStrength;
    public float knockbackDuration;
    public float impactDamage;

    public ThrowData(JSONObject parse) {
        projectileBox = JsonConverter.pullFloats(parse.getJSONArray("projectileBox"));
        maxThrowDistance = parse.getFloat("maxThrowDistance");
        minThrowDuration = parse.getFloat("minThrowDuration");
        maxThrowDuration = parse.getFloat("maxThrowDuration");

        if(parse.has("impactDamage")) {
            impactDamage = parse.getFloat("impactDamage");
        }

        if(parse.has("explosionRadius")) {
            explosionRadius = parse.getFloat("explosionRadius");
        }

        if(parse.has("explosionDamage")) {
            explosionDamage = parse.getFloat("explosionDamage");
        }

        if(parse.has("knockbackStrength")) {
            knockbackStrength = parse.getFloat("knockbackStrength");
        }

        if(parse.has("knockbackDuration")) {
            knockbackDuration = parse.getFloat("knockbackDuration");
        }
    }

    public boolean hasExplosion() {
        return explosionRadius > 0 && explosionDamage > 0;
    }

    public boolean hasImpact() {
        return impactDamage > 0;
    }

    public boolean isImpactExplosion() {
        return hasExplosion() && hasImpact();
    }

}