package dev.michey.expo.server.steam;

import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ExpoServerSteam {

    private final String webApiKey;

    public ExpoServerSteam(String webApiKey) {
        this.webApiKey = webApiKey;
    }

    public JSONObject authenticateUserTicket(String ticket) {
        var client = HttpClient.newHttpClient();

        var request = HttpRequest.newBuilder(URI.create("https://api.steampowered.com/ISteamUserAuth/AuthenticateUserTicket/v1/?format=json&key="
                        + webApiKey + "&appid=" + ExpoShared.STEAM_APP_ID + "&ticket=" + ticket))
                .GET()
                .build();

        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new JSONObject(response.body());
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

}
