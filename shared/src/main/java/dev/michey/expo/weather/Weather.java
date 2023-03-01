package dev.michey.expo.weather;

public enum Weather {

    SUN(0, "Sun"),
    RAIN(1, "Rain"),
    STORM(2, "Storm"),
    SNOW(3, "Snow");

    public final int WEATHER_ID;
    public final String WEATHER_NAME;

    Weather(int WEATHER_ID, String WEATHER_NAME) {
        this.WEATHER_ID = WEATHER_ID;
        this.WEATHER_NAME = WEATHER_NAME;
    }

    public static Weather idToWeather(int id) {
        return switch (id) {
            case 1 -> RAIN;
            case 2 -> STORM;
            case 3 -> SNOW;
            default -> SUN;
        };
    }

}
