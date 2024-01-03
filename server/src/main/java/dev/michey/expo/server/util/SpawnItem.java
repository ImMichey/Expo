package dev.michey.expo.server.util;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;

public class SpawnItem {

    public int id;
    public int amount;

    public SpawnItem(String identifier, int min, int max) {
        id = ItemMapper.get().getMapping(identifier).id;
        amount = MathUtils.random(min, max);
    }

    public SpawnItem(String identifier, int min, int max, float chance) {
        if(MathUtils.random() <= chance) {
            id = ItemMapper.get().getMapping(identifier).id;
            amount = min + MathUtils.random(max - min);
        }
    }

}
