package dev.michey.expo.util.visualizer;

import org.json.JSONObject;

public class VisualizerData {

    public String name;
    public String alias;
    public int roundsPlayed;
    public int roundsWon;
    public float winrate;

    public VisualizerData(JSONObject json) {
        name = json.getString("name");
        alias = json.getString("alias");
        roundsPlayed = json.getInt("roundsPlayed");
        roundsWon = json.getInt("roundsWon");
        winrate = json.getFloat("winRate");
    }

}
