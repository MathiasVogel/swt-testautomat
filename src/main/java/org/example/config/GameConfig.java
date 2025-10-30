package org.example.config;

public class GameConfig {
    public String startUrl;
    public int steps;

    public GameConfig() { }

    public GameConfig(String startUrl, int steps) {
        this.startUrl = startUrl;
        this.steps = steps;
    }
}
