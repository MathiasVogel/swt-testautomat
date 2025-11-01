package org.example;

import org.example.config.ApplicationConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Main {
    private static final String DEFAULT_CONFIG_PATH = "./config.yaml";

    public static void main(String[] args) {
        String configPath = getConfigPath(args);

        try {
            Path configFile = Path.of(configPath);
            System.out.println("Loading config from: " + configFile.toAbsolutePath());

            var config = ApplicationConfig.fromFile(configFile);

            config.games.stream()
                    .map(gameConfig -> WikiPhilosophyFirefox.runGame(
                            gameConfig.startUrl,
                            gameConfig.steps,
                            config.headless))
                    .map(result -> formatResult(result, config))
                    .forEach(System.out::println);

        } catch (IOException ex) {
            System.err.println("Error: Config file not found at '" + configPath + "'");
            System.err.println("Details: " + ex.getMessage());
            System.exit(1);
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static String getConfigPath(String[] args) {
        if (args.length > 0) {
            return args[0];
        }
        return DEFAULT_CONFIG_PATH;
    }

    private static String formatResult(Result result, ApplicationConfig config) {
        Reason reason = result.reason;
        return result.startUrl + " -> " + switch (reason) {
            case TITLE_EXACT -> "SUCCESS: 'Philosophie' found after %s steps".formatted(formatSteps(result, config));
            case H1H2_CONTAINS -> "SUCCESS: 'Philosophie' found after %s steps".formatted(formatSteps(result, config));
            case MAX_STEPS -> "WARN: Maximum number of steps exceeded";
            case NO_LINK -> "ERROR: No further link found";
            case ERROR -> "ERROR: Unknown Error";
            case LOOP -> "WARN: Loop detected at %s".formatted(String.join(" -> ", result.path));
        } + " [" + formatVisitedPaths(result) + "]";
    }

    private static String formatSteps(Result result, ApplicationConfig config) {
        int totalSteps = config.firstStepCounts ? result.steps : result.steps - 1;
        return Integer.toString(totalSteps);
    }

    private static String formatVisitedPaths(Result result) {
        return String.join(" -> ", result.path);
    }
}