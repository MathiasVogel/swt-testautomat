package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class WikiPhilosophyFirefox {

    private static final String TARGET_DE = "Philosophie";
    private static final String TARGET_EN = "Philosophy";

    // Optional: CLI-Flags
    // args[0] = Start-URL (optional)
    // args[1] = maxSteps (optional, Standard 50)
    // args[2] = "headless" (optional)
    public static void main(String[] args) {
        // Falls Selenium Manager (ab 4.6) den Treiber nicht automatisch lädt:
        // System.setProperty("webdriver.gecko.driver", "/pfad/zu/geckodriver");

        int maxSteps = args.length > 1 ? parseIntSafe(args[1], 50) : 50;
        boolean headless = args.length > 2 && "headless".equalsIgnoreCase(args[2]);

        String startUrl = args.length > 0 && args[0] != null && !args[0].isBlank()
                ? args[0]
                : "https://de.wikipedia.org/wiki/Spezial:Zuf%C3%A4llige_Seite";

        FirefoxOptions options = new FirefoxOptions();
        if (headless) options.addArguments("-headless");

        WebDriver driver = new FirefoxDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        Set<String> visited = new HashSet<>();

        try {
            driver.get(startUrl);

            for (int step = 1; step <= maxSteps; step++) {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#mw-content-text")));

                String currentUrl = driver.getCurrentUrl();
                String title = extractArticleTitle(driver.getTitle());
                String lang = currentUrl.contains("//de.") ? "de" : "en";
                String target = lang.equals("de") ? TARGET_DE : TARGET_EN;

                String canonical = canonicalArticleName(currentUrl);
                if (!visited.add(canonical)) {
                    System.out.printf("[%02d] ♻️ Zyklus entdeckt bei: %s%n", step, title);
                    break;
                }

                System.out.printf("[%02d] %s (%s)%n", step, title, currentUrl);

                if (title.equalsIgnoreCase(target)) {
                    System.out.println("🎯 Ziel erreicht: " + target);
                    break;
                }

                // JS ermittelt den ersten „gültigen“ Link im Fließtext
                String nextHref = (String) ((JavascriptExecutor) driver).executeScript(FIRST_VALID_LINK_FINDER_JS);
                if (nextHref == null || nextHref.isEmpty()) {
                    System.out.println("❌ Kein gültiger Link gefunden – Sackgasse.");
                    break;
                }

                // Relativen Link absolut machen
                if (nextHref.startsWith("/")) {
                    String base = currentUrl.split("/wiki/")[0]; // z. B. https://de.wikipedia.org
                    nextHref = base + nextHref;
                }

                driver.navigate().to(nextHref);
            }
        } finally {
            driver.quit();
        }
    }

    // Robustere Titel-Extraktion (entfernt „– Wikipedia“/„- Wikipedia“)
    private static String extractArticleTitle(String pageTitle) {
        if (pageTitle == null) return "";
        int idx = pageTitle.indexOf(" – ");
        if (idx < 0) idx = pageTitle.indexOf(" - ");
        return idx > 0 ? pageTitle.substring(0, idx) : pageTitle;
    }

    // Kanonischer Slug (ohne #Anker, %20→Leerzeichen), um Zyklen zu erkennen
    private static String canonicalArticleName(String url) {
        try {
            String[] parts = url.split("/wiki/");
            if (parts.length < 2) return url;
            String slug = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            int hash = slug.indexOf('#');
            if (hash >= 0) slug = slug.substring(0, hash);
            return slug.replace('_', ' ');
        } catch (Exception e) {
            return url;
        }
    }

    private static int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    // -- JavaScript-Snippet: findet den ersten gültigen Link im Artikel-Fließtext --
    // Regeln:
    // - Nur interne /wiki/... Links (keine Namensräume wie Datei:, Hilfe:, Kategorie:)
    // - Keine Anker (#)
    // - Keine Links in Klammern (wir zählen Klammer-Tiefe)
    // - Keine kursiven Links
    // - Ignoriere Infoboxen, TOC, Navboxen, Bilder, Referenzen, Tabellen etc.
    private static final String FIRST_VALID_LINK_FINDER_JS =
            "const content = document.querySelector('#mw-content-text');\n" +
            "if (!content) return null;\n" +
            "const blacklistSelectors = [\n" +
            "  '.infobox', '.vertical-navbox', '.navbox', '.toc', '.thumb', '.references', '.reflist',\n" +
            "  '.hatnote', '.metadata', '.mw-empty-elt', 'table', 'style', 'sup.reference', '.ambox'\n" +
            "];\n" +
            "blacklistSelectors.forEach(sel => content.querySelectorAll(sel).forEach(e => e.remove()));\n" +
            "function isItalic(el){\n" +
            "  while (el) {\n" +
            "    const tag = el.tagName ? el.tagName.toLowerCase() : '';\n" +
            "    const style = el.ownerDocument.defaultView.getComputedStyle(el);\n" +
            "    if (tag === 'i' || tag === 'em' || style.fontStyle === 'italic') return true;\n" +
            "    el = el.parentElement;\n" +
            "  }\n" +
            "  return false;\n" +
            "}\n" +
            "const blocks = content.querySelectorAll('p, ul, ol');\n" +
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
