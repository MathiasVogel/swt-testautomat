package org.example;

import java.util.List;

public class Result {
    public final Reason reason;
    public final List<String> path; // Liste der besuchten URLs (in Reihenfolge)
    public final int steps;
    public final String lastTitle;
    public final String startUrl;

    public Result(Reason reason, List<String> path, int steps, String lastTitle, String startUrl) {
        this.reason = reason;
        this.path = path;
        this.steps = steps;
        this.lastTitle = lastTitle;
        this.startUrl = startUrl;
    }
}