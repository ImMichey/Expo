package dev.michey.expo.server.main.logic.inventory.item.mapping;

import org.json.JSONObject;

public class ItemRenderParticleEmitter {

    public String emitterName;
    public float emitterDelay;

    private float delta;
    public boolean spawnParticlesThisTick;

    public ItemRenderParticleEmitter(JSONObject parse) {
        emitterName = parse.getString("emitterName");
        emitterDelay = parse.getFloat("emitterDelay");
    }

    public void update(float delta) {
        this.delta += delta;

        if(this.delta >= emitterDelay) {
            this.delta %= emitterDelay;
            spawnParticlesThisTick = true;
        } else {
            spawnParticlesThisTick = false;
        }
    }

}