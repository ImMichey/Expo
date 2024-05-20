package dev.michey.expo.server.main.logic.world.chunk;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.GenerationUtils;

public class GenerationRandom {

    private final RandomXS128 random;

    public GenerationRandom(int chunkX, int chunkY, ServerEntityType type) {
        int _x = chunkX + type.ENTITY_ID;
        int _y = chunkY;
        long tmp = (_y + ((_x + 1) / 2));
        long n = _x + (tmp * tmp);

        random = new RandomXS128(n);
    }

    public GenerationRandom(int x, int y) {
        long tmp = (y + ((x + 1) / 2));
        long n = x + (tmp * tmp);
        random = new RandomXS128(n);
    }

    public double randomD() {
        return random.nextDouble();
    }

    public float random() {
        return random.nextFloat();
    }

    public float random(float range) {
        return random.nextFloat() * range;
    }

    public float random(float start, float end) {
        return start + random.nextFloat() * (end - start);
    }

    public int random(int range) {
        return random.nextInt(range + 1);
    }

    public int random(int start, int end) {
        return start + random.nextInt(end - start + 1);
    }

    public int of(int... values) {
        return values[random.nextInt(values.length)];
    }

    public int sign() {
        return 1 | random.nextInt() >> 31;
    }

    public boolean randomBoolean() {
        return random.nextBoolean();
    }

    public Vector2[] positions(int amount, float radiusMin, float radiusMax) {
        float anglePer = 360.0f / amount;
        Vector2[] positions = new Vector2[amount];
        float randomOffset = random(360f);

        for(int i = 0; i < amount; i++) {
            positions[i] = GenerationUtils.circular(anglePer * i + randomOffset, random(radiusMin, radiusMax));
        }

        return positions;
    }

    public Vector2 circularRandom(float radius) {
        return GenerationUtils.circular(random(360f), radius);
    }

    public void setSeed(int x, int y) {
        long tmp = (y + ((x + 1) / 2));
        long n = x + (tmp * tmp);
        random.setSeed(n);
    }

}
