package dev.michey.expo.util.visualizer;

import org.json.JSONObject;

public class VisualizerData2 {

    public String name;
    public String alias;
    public int ecoRoundsPlayed;
    public int ecoRoundsWon;
    public int totalRoundsPlayed;
    public float winrate;
    public float perc;

    public VisualizerData2(JSONObject json) {
        name = json.getString("name");
        alias = json.getString("alias");
        ecoRoundsPlayed = json.getInt("ecoRoundsPlayed");
        ecoRoundsWon = json.getInt("ecoRoundsWon");
        totalRoundsPlayed = json.getInt("totalRoundsPlayed");
        winrate = json.getFloat("winRate");
        perc = totalRoundsPlayed / (float) ecoRoundsPlayed;
    }

}
