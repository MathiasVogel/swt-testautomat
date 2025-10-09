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
import java.util.Objects;

public class WikiPhilosophyFirefox {

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

                if ("philosophie".equalsIgnoreCase(lastTitle)) {
                    return new Result(Reason.TITLE_EXACT, path, step, lastTitle);
                }

                boolean headingContains = Boolean.TRUE.equals(
                        ((JavascriptExecutor) driver).executeScript(
                                "return Array.from(document.querySelectorAll('h1, h2'))" +
                                        ".some(h => (h.innerText||'').toLowerCase().includes('philosophie'));"
                        )
                );
                if (headingContains) {
                    return new Result(Reason.H1H2_CONTAINS, path, step, lastTitle);
                }

                if (step == maxSteps) {
                    return new Result(Reason.MAX_STEPS, path, step, lastTitle);
                }

                String nextHref = (String) ((JavascriptExecutor) driver).executeScript(FIRST_VALID_LINK_FINDER_JS);
                if (nextHref == null || nextHref.isEmpty()) {
                    return new Result(Reason.NO_LINK, path, step, lastTitle);
                }

                if (nextHref.startsWith("/")) {
                    String base;
                    base = Objects.requireNonNull(currentUrl).split("/wiki/")[0];
                    nextHref = base + nextHref;
                }

                driver.navigate().to(nextHref);
            }
        } catch (Throwable t) {
            return new Result(Reason.ERROR, path, Math.max(step, 0), lastTitle);
        } finally {
            driver.quit();
        }
        return new Result(Reason.MAX_STEPS, path, step, lastTitle);
    }

    private static String extractArticleTitle(String pageTitle) {
        if (pageTitle == null) return "";
        int idx = pageTitle.indexOf(" – ");
        if (idx < 0) idx = pageTitle.indexOf(" - ");
        return idx > 0 ? pageTitle.substring(0, idx) : pageTitle;
    }

    private static final String FIRST_VALID_LINK_FINDER_JS =
            """
                    const content = document.querySelector('#mw-content-text');
                    if (!content) return null;
                    const clone = content.cloneNode(true);
                    const blacklistSelectors = [
                      '.infobox', '.vertical-navbox', '.navbox', '.toc', '.thumb', '.references', '.reflist',
                      '.hatnote', '.metadata', '.mw-empty-elt', 'table', 'style', 'sup.reference', '.ambox'
                    ];
                    blacklistSelectors.forEach(sel => clone.querySelectorAll(sel).forEach(e => e.remove()));
                    function isItalic(el){
                      while (el) {
                        const tag = el.tagName ? el.tagName.toLowerCase() : '';
                        const style = el.ownerDocument.defaultView.getComputedStyle(el);
                        if (tag === 'i' || tag === 'em' || style.fontStyle === 'italic') return true;
                        el = el.parentElement;
                      }
                      return false;
                    }
                    const blocks = clone.querySelectorAll('p, ul, ol');
                    let depth = 0;
                    function scanNode(node){
                      if (node.nodeType === Node.TEXT_NODE){
                        const t = node.textContent;
                        for (let ch of t){ if (ch === '(') depth++; else if (ch === ')' && depth>0) depth--; }
                      } else if (node.nodeType === Node.ELEMENT_NODE){
                        const el = node;
                        if (el.matches('style, script, sup, table, figure, .IPA, .nowrap')) return null;
                        if (el.tagName && el.tagName.toLowerCase() === 'a'){
                          const href = el.getAttribute('href') || '';
                          const okHref = href.startsWith('/wiki/') && !href.includes(':') && !href.includes('#');
                          if (okHref && depth === 0 && !isItalic(el)) return href;
                        }
                        for (let child of el.childNodes){
                          const found = scanNode(child);
                          if (found) return found;
                        }
                      }
                      return null;
                    }
                    for (let b of blocks){
                      const found = scanNode(b);
                      if (found) return found;
                    }
                    return null;""";
}
