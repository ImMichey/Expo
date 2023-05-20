package dev.michey.expo.server.main.logic.world.gen;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.server.ServerLauncher;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class EntityPopulationBounds {

    private static EntityPopulationBounds INSTANCE;

    private final HashMap<ServerEntityType, EntityBoundsEntry> map;

    public EntityPopulationBounds() {
        map = new HashMap<>();

        String data = null;

        if(Gdx.files == null) {
            try {
                data = new String(ServerLauncher.class.getResourceAsStream("/entity_dimensions.json").readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            data = Gdx.files.internal("entity_dimensions.json").readString();
        }

        JSONObject dimensions = new JSONObject(data);

        for(ServerEntityType type : ServerEntityType.values()) {
            if(dimensions.has(type.name())) {
                map.put(type, new EntityBoundsEntry(dimensions.getJSONObject(type.name())));
            }
        }
    }

    public EntityBoundsEntry getFor(ServerEntityType type) {
        return map.get(type);
    }

    public static EntityPopulationBounds get() {
        if(INSTANCE == null) INSTANCE = new EntityPopulationBounds();
        return INSTANCE;
    }

}
