import org.example.Reason;
import org.example.Result;
import org.example.WikiPhilosophyFirefox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrationstest mit mehreren Startartikeln.
 * Aktivieren via Env-Var:
 * RUN_SELENIUM_TESTS=true
 */
@EnabledIfEnvironmentVariable(named = "RUN_SELENIUM_TESTS", matches = "true")
class WikiPhilosophyFirefoxParamTest {

    @ParameterizedTest(name = "[{index}] Start: {0}, maxSteps={1}")
    @DisplayName("Philosophie-Spiel: Abbruchbedingung erreicht (mehrere Startseiten)")
    @CsvSource({
            // Beliebte, stabile DE-Artikel
            "https://de.wikipedia.org/wiki/Bananen,20",
    })
    void philosophyGameStopsAsExpected(String startUrl, int maxSteps) {
        boolean headless = false; // für CI stabiler

        Result result = WikiPhilosophyFirefox.runGame(startUrl, maxSteps, headless);

        assertNotNull(result, "Result darf nicht null sein");
        assertTrue(
                (result).reason == Reason.TITLE_EXACT
                        || result.reason == Reason.H1H2_CONTAINS
                        || result.reason == Reason.MAX_STEPS
                        || result.reason == Reason.NO_LINK, // Sackgasse ist ok
                "Unerwarteter Abbruchgrund: " + result.reason
        );

        assertTrue(result.steps > 0, "Es sollten Schritte gelaufen sein");
        assertNotNull(result.path);
        assertFalse(result.path.isEmpty(), "Pfad sollte nicht leer sein");

        if (result.reason == Reason.MAX_STEPS) {
            assertEquals(maxSteps, result.steps, "Bei MAX_STEPS sollte step==maxSteps sein");
        }

        assertNotNull(result.lastTitle);
        assertFalse(result.lastTitle.isBlank(), "Letzter Titel sollte gesetzt sein");
    }
}
