package org.example;

import java.util.List;

public class Result {
    public final Reason reason;
    public final List<String> path; // Liste der besuchten URLs (in Reihenfolge)
    public final int steps;
    public final String lastTitle;

    public Result(Reason reason, List<String> path, int steps, String lastTitle) {
        this.reason = reason;
        this.path = path;
        this.steps = steps;
        this.lastTitle = lastTitle;
    }
}