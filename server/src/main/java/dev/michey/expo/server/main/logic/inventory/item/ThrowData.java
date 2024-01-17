package dev.michey.expo.server.main.logic.inventory.item;

import org.json.JSONObject;

public class ThrowData {

    public float maxThrowDistance;
    public float minThrowDuration;
    public float maxThrowDuration;
    public float explosionRadius;
    public float explosionDamage;
    public float knockbackStrength;
    public float knockbackDuration;

    public ThrowData(JSONObject parse) {
        maxThrowDistance = parse.getFloat("maxThrowDistance");
        minThrowDuration = parse.getFloat("minThrowDuration");
        maxThrowDuration = parse.getFloat("maxThrowDuration");
        explosionRadius = parse.getFloat("explosionRadius");
        explosionDamage = parse.getFloat("explosionDamage");
        knockbackStrength = parse.getFloat("knockbackStrength");
        knockbackDuration = parse.getFloat("knockbackDuration");
    }

}