package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.graphics.Color;
import dev.michey.expo.server.util.JsonConverter;
import org.json.JSONObject;

public class ItemRenderLight {

    public Color color;
    public float distanceMin;
    public float distanceMax;
    public boolean pulsating;
    public boolean flicker;
    public float flickerStrength;
    public float flickerCooldown;
    public float pulsatingSpeed;
    public float emissionQuadratic;
    public float emissionConstant;
    public int rayCount;

    public ItemRenderLight(JSONObject parse) {
        if(parse.has("color")) {
            color = JsonConverter.pullColor(parse.getJSONArray("color"));
        } else {
            color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        if(parse.has("distance")) {
            distanceMin = parse.getFloat("distance");
            distanceMax = distanceMin;
        }

        if(parse.has("distanceMin") && parse.has("distanceMax")) {
            distanceMin = parse.getFloat("distanceMin");
            distanceMax = parse.getFloat("distanceMax");
        }

        if(parse.has("pulsatingSpeed")) {
            pulsatingSpeed = parse.getFloat("pulsatingSpeed");
        } else {
            pulsatingSpeed = 1.0f;
        }

        if(parse.has("emissionQuadratic")) {
            emissionQuadratic = parse.getFloat("emissionQuadratic");
        } else {
            emissionQuadratic = 0.75f;
        }

        if(parse.has("emissionConstant")) {
            emissionConstant = parse.getFloat("emissionConstant");
        } else {
            emissionConstant = 0.75f;
        }

        if(parse.has("flickerCooldown")) {
            flickerCooldown = parse.getFloat("flickerCooldown");
        } else {
            flickerCooldown = 0.15f;
        }

        if(parse.has("flickerStrength")) {
            flickerStrength = parse.getFloat("flickerStrength");
        }

        if(parse.has("rayCount")) {
            rayCount = parse.getInt("rayCount");
        } else {
            rayCount = 32;
        }

        if(parse.has("pulsating")) {
            pulsating = parse.getBoolean("pulsating");
        } else {
            pulsating = false;
        }

        if(parse.has("flicker")) {
            flicker = parse.getBoolean("flicker");
        } else {
            flicker = false;
        }
    }

}