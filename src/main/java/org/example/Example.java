package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import java.util.List;

public class Example {
    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();

        System.out.println("Bitte geben sie ein Wikipedia-Thema ein:");
        String topic = System.console().readLine();
        try {
            driver.get("https://de.wikipedia.org/wiki/" + "topic");
            boolean found = false;
            int maxSteps = 50;

            for (int i = 0; i < maxSteps && !found; i++) {

            }

        } finally {
            driver.quit();
        }
    }

    private static boolean ContainsSubject(WebDriver driver){
        WebElement content = driver.findElement(By.id("content"));

        // Finde Philosopie im ganzen Content


        return false;
    }

    private static void GetFirstLink(WebDriver driver) {
        WebElement content = driver.findElement(By.id("content"));

        WebElement body = content.findElement(By.id("bodyContent"));

        // Ersten Link im body finden, welcher nicht in einem span-Element enthalten ist und somit nicht kursiv ist oder in einer Klammer steht.
        WebElement firstLink = body.findElement(By.xpath(".//a[not(ancestor::span)]"));

        driver.get(firstLink.getAttribute("href"));
    }
}
