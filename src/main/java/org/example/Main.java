package org.example;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        List<String> startUrls = Arrays.asList(args[0].split(","));
        int maxSteps = Integer.parseInt(args[1]);

        boolean showWindows = args.length > 2;

        startUrls.stream()
                .map(url -> WikiPhilosophyFirefox.runGame(url, maxSteps, showWindows))
                .map(Main::formatResult)
                .forEach(System.out::println);
    }

    private static String formatResult(Result result) {
        Reason reason = result.reason;
        return result.startUrl + "-> " + switch (reason) {
            case TITLE_EXACT -> "SUCCESS: Found after %s steps".formatted(result.steps);
            case H1H2_CONTAINS -> "SUCCESS: Found after %s steps".formatted(result.steps);
            case MAX_STEPS -> "WARN: Maximum number of steps exceeded";
            case NO_LINK -> "ERROR: No further link found";
            case ERROR -> "ERROR: Unknown Error";
        };
    }
}