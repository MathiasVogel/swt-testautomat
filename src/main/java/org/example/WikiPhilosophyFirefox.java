package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class WikiPhilosophyFirefox {

    private static final int DEFAULT_MAX_STEPS = 50;

    public static void main(String[] args) {
        int maxSteps = (args.length > 1) ? parseIntSafe(args[1], DEFAULT_MAX_STEPS) : DEFAULT_MAX_STEPS;
        boolean headless = args.length > 2 && "headless".equalsIgnoreCase(args[2]);
        String startUrl = (args.length > 0 && args[0] != null && !args[0].isBlank())
                ? args[0]
                : "https://de.wikipedia.org/wiki/Spezial:Zuf%C3%A4llige_Seite";

        Result r = runGame(startUrl, maxSteps, headless);
        System.out.printf("Beendet nach %d Schritten (%s) bei: %s%n", r.steps, r.reason, r.lastTitle);
        // Optional: Pfad ausgeben
        r.path.forEach(System.out::println);
    }

    /**
     * Testbare Kernlogik: führt das Spiel aus und liefert Reason + Pfad zurück.
     */
    public static Result runGame(String startUrl, int maxSteps, boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) options.addArguments("-headless");

        WebDriver driver = new FirefoxDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        List<String> path = new ArrayList<>();
        int step = 0;
        String lastTitle = "";

        try {
            driver.get(startUrl);

            for (step = 1; step <= maxSteps; step++) {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#mw-content-text")));
                String currentUrl = driver.getCurrentUrl();
                lastTitle = extractArticleTitle(driver.getTitle());
                path.add(currentUrl);

                // 1) Titel exakt "Philosophie"
                if ("philosophie".equalsIgnoreCase(lastTitle)) {
                    return new Result(Reason.TITLE_EXACT, path, step, lastTitle);
                }

                // 2) h1 oder h2 enthält "Philosophie" (case-insensitive)
                boolean headingContains = Boolean.TRUE.equals(
                        ((JavascriptExecutor) driver).executeScript(
                                "return Array.from(document.querySelectorAll('h1, h2'))" +
                                        ".some(h => (h.innerText||'').toLowerCase().includes('philosophie'));"
                        )
                );
                if (headingContains) {
                    return new Result(Reason.H1H2_CONTAINS, path, step, lastTitle);
                }

                // 3) Max Steps?
                if (step == maxSteps) {
                    return new Result(Reason.MAX_STEPS, path, step, lastTitle);
                }

                // Nächsten gültigen Link im Fließtext finden
                String nextHref = (String) ((JavascriptExecutor) driver).executeScript(FIRST_VALID_LINK_FINDER_JS);
                if (nextHref == null || nextHref.isEmpty()) {
                    return new Result(Reason.NO_LINK, path, step, lastTitle);
                }

                if (nextHref.startsWith("/")) {
                    String base = currentUrl.split("/wiki/")[0];
                    nextHref = base + nextHref;
                }

                driver.navigate().to(nextHref);
            }
        } catch (Throwable t) {
            return new Result(Reason.ERROR, path, Math.max(step, 0), lastTitle);
        } finally {
            driver.quit();
        }
        // Fallback (sollte nicht erreicht werden)
        return new Result(Reason.MAX_STEPS, path, Math.max(step, 0), lastTitle);
    }

    // -- Hilfsfunktionen & JS (unverändert) --

    private static String extractArticleTitle(String pageTitle) {
        if (pageTitle == null) return "";
        int idx = pageTitle.indexOf(" – ");
        if (idx < 0) idx = pageTitle.indexOf(" - ");
        return idx > 0 ? pageTitle.substring(0, idx) : pageTitle;
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static final String FIRST_VALID_LINK_FINDER_JS =
            "const content = document.querySelector('#mw-content-text');\n" +
                    "if (!content) return null;\n" +
                    "const clone = content.cloneNode(true);\n" +
                    "const blacklistSelectors = [\n" +
                    "  '.infobox', '.vertical-navbox', '.navbox', '.toc', '.thumb', '.references', '.reflist',\n" +
                    "  '.hatnote', '.metadata', '.mw-empty-elt', 'table', 'style', 'sup.reference', '.ambox'\n" +
                    "];\n" +
                    "blacklistSelectors.forEach(sel => clone.querySelectorAll(sel).forEach(e => e.remove()));\n" +
                    "function isItalic(el){\n" +
                    "  while (el) {\n" +
                    "    const tag = el.tagName ? el.tagName.toLowerCase() : '';\n" +
                    "    const style = el.ownerDocument.defaultView.getComputedStyle(el);\n" +
                    "    if (tag === 'i' || tag === 'em' || style.fontStyle === 'italic') return true;\n" +
                    "    el = el.parentElement;\n" +
                    "  }\n" +
                    "  return false;\n" +
                    "}\n" +
                    "const blocks = clone.querySelectorAll('p, ul, ol');\n" +
                    "let depth = 0;\n" +
                    "function scanNode(node){\n" +
                    "  if (node.nodeType === Node.TEXT_NODE){\n" +
                    "    const t = node.textContent;\n" +
                    "    for (let ch of t){ if (ch === '(') depth++; else if (ch === ')' && depth>0) depth--; }\n" +
                    "  } else if (node.nodeType === Node.ELEMENT_NODE){\n" +
                    "    const el = node;\n" +
                    "    if (el.matches('style, script, sup, table, figure, .IPA, .nowrap')) return null;\n" +
                    "    if (el.tagName && el.tagName.toLowerCase() === 'a'){\n" +
                    "      const href = el.getAttribute('href') || '';\n" +
                    "      const okHref = href.startsWith('/wiki/') && !href.includes(':') && !href.includes('#');\n" +
                    "      if (okHref && depth === 0 && !isItalic(el)) return href;\n" +
                    "    }\n" +
                    "    for (let child of el.childNodes){\n" +
                    "      const found = scanNode(child);\n" +
                    "      if (found) return found;\n" +
                    "    }\n" +
                    "  }\n" +
                    "  return null;\n" +
                    "}\n" +
                    "for (let b of blocks){\n" +
                    "  const found = scanNode(b);\n" +
                    "  if (found) return found;\n" +
                    "}\n" +
                    "return null;";
}
