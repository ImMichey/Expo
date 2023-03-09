package dev.michey.expo.weather;

import dev.michey.expo.util.ExpoShared;

public enum Weather {

    SUN(0, "Sun", new float[] {600, 1200, -1, -1}),
    RAIN(1, "Rain", new float[] {300, 600, 12.0f, 36.0f}),
    STORM(2, "Storm", new float[] {300, 600, 1.0f, 2.5f}),
    SNOW(3, "Snow", new float[] {300, 600, 1.0f, 2.5f});

    public final int WEATHER_ID;
    public final String WEATHER_NAME;
    public final float[] WEATHER_DATA;

    Weather(int WEATHER_ID, String WEATHER_NAME, float[] WEATHER_DATA) {
        this.WEATHER_ID = WEATHER_ID;
        this.WEATHER_NAME = WEATHER_NAME;
        this.WEATHER_DATA = WEATHER_DATA;
    }

    public static Weather idToWeather(int id) {
        return switch (id) {
            case 1 -> RAIN;
            case 2 -> STORM;
            case 3 -> SNOW;
            default -> SUN;
        };
    }

    public float generateWeatherDuration() {
        float rFloat = ExpoShared.RANDOM.nextFloat();
        float minDuration = WEATHER_DATA[0];
        float maxDuration = WEATHER_DATA[1];
        return minDuration + (maxDuration - minDuration) * rFloat;
    }

    public float generateWeatherStrength() {
        float rFloat = ExpoShared.RANDOM.nextFloat();
        float minStrength = WEATHER_DATA[2];
        float maxStrength = WEATHER_DATA[3];
        return minStrength + (maxStrength - minStrength) * rFloat;
    }

}
