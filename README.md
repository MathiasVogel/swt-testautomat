# SWT-Testautomat
Repo für das Gruppen-Projekt in SWT

Ausführen der Applikation:

JAR erzeugen:
```bash
mvn package
```

Config File mit gewünschter Konfiguration erstellen und als config.yaml speichern. Bsp.:
```yaml
headless: true
firstStepCounts: false
games:
  - startUrl: "https://de.wikipedia.org/wiki/Bananen"
    steps: 10
  - startUrl: "https://de.wikipedia.org/wiki/Apfel"
    steps: 3
```

JAR ausführen (mit Pfad zum config-file falls dieses nicht im selben Ordner mit dem Namen config.yaml ist):
```bash
java -jar swt-testautomat-1.0.0-SNAPSHOT.jar

# Oder mit Pfad zum config file
java -jar swt-testautomat-1.0.0-SNAPSHOT.jar ../config.yaml
```
