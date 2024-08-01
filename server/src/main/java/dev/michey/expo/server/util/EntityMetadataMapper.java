package dev.michey.expo.server.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import dev.michey.expo.server.ServerLauncher;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class EntityMetadataMapper {

    private static EntityMetadataMapper INSTANCE;

    private final HashMap<ServerEntityType, EntityMetadata> map;

    public EntityMetadataMapper(boolean reload) {
        map = new HashMap<>();

        String data = null;

        if(Gdx.files == null) {
            try {
                data = new String(ServerLauncher.class.getResourceAsStream("/entity_metadata.json").readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            FileHandle fh = reload ? Gdx.files.absolute("C:\\IDEAProjects\\Expo\\assets_shared\\entity_metadata.json") : Gdx.files.internal("entity_metadata.json");

            if(!fh.exists() && reload) {
                fh = Gdx.files.internal("entity_metadata.json");
            }

            data = fh.readString();
        }

        JSONObject dimensions = new JSONObject(data);

        for(ServerEntityType type : ServerEntityType.values()) {
            if(dimensions.has(type.name())) {
                map.put(type, new EntityMetadata(dimensions.getJSONObject(type.name())));
            }
        }
    }

    public EntityMetadata getFor(ServerEntityType type) {
        return map.get(type);
    }

    public void refresh() {
        INSTANCE = new EntityMetadataMapper(true);
    }

    public static EntityMetadataMapper get() {
        if(INSTANCE == null) INSTANCE = new EntityMetadataMapper(false);
        return INSTANCE;
    }

}