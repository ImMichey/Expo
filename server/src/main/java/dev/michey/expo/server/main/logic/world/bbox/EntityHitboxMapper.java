package dev.michey.expo.server.main.logic.world.bbox;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.server.ServerLauncher;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class EntityHitboxMapper {

    private static EntityHitboxMapper INSTANCE;

    private final HashMap<ServerEntityType, EntityHitbox> map;

    public EntityHitboxMapper() {
        map = new HashMap<>();

        String data = null;

        if(Gdx.files == null) {
            try {
                data = new String(ServerLauncher.class.getResourceAsStream("/entity_hitbox.json").readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            data = Gdx.files.internal("entity_hitbox.json").readString();
        }

        JSONObject dimensions = new JSONObject(data);

        for(ServerEntityType type : ServerEntityType.values()) {
            if(dimensions.has(type.name())) {
                map.put(type, new EntityHitbox(dimensions.getJSONObject(type.name()).getJSONArray("bbox")));
            }
        }
    }

    public EntityHitbox getFor(ServerEntityType type) {
        return map.get(type);
    }

    public static EntityHitboxMapper get() {
        if(INSTANCE == null) INSTANCE = new EntityHitboxMapper();
        return INSTANCE;
    }

}