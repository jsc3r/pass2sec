# pass2sec

Quarkus-PicoCLI-Anwendung (Java 25), die Proton-Pass-Login-Einträge nach Tresor auswählt,
SimpleLogin-Aliase anlegt, Passwörter über die offizielle Proton-CLI erzeugt und Website-Änderungen
über Playwright ausführt. Nach erfolgreichem Kontroll-Login wird der vorhandene Tresoreintrag
aktualisiert und erneut gelesen.

**Stand:** Die Proton-CLI-Anbindung, SimpleLogin-Anbindung, Browser-Engine und Wiederherstellung
sind implementiert. Website-Abläufe sind konfigurierbar. Mitgeliefert wird eine Vorlage für eine
fiktive Website, kein fertig getesteter GitHub-Adapter. Echte Konten wurden nicht verändert.
Beliebige Websites lassen sich nicht allein aus ihrer URL zuverlässig migrieren: Formulare,
2FA und E-Mail-Bestätigung brauchen ein passendes, getestetes Profil.

## Voraussetzungen und Build

### Einfach starten

Im Projektordner startet `./pass2sec` die Anwendung ohne Quarkus-Dev-Konsole.
Ohne Argumente öffnet sich eine interaktive Sitzung:

```text
$ pass2sec
pass2sec> help
pass2sec> auth help
pass2sec> auth proton login --dry-run
pass2sec> exit
```

Mit `help` werden alle verfügbaren Befehle erklärt. Nach jedem Befehl erscheint
der Prompt erneut; auch bei ungültigen oder fehlgeschlagenen Befehlen bleibt die
Sitzung offen. `exit`, `quit` oder Strg+D beendet sie, Strg+C bricht den Prozess ab.
Pfade mit Leerzeichen in Anführungszeichen setzen. Es gibt keine Shell-Ausführung,
Variablenersetzung oder gespeicherte Befehlshistorie. Passwörter und API-Keys
weiterhin ausschließlich in die dafür vorgesehenen verdeckten Abfragen eingeben.

Direkte Einzelaufrufe bleiben möglich:

```bash
./pass2sec
./pass2sec auth help
./pass2sec auth proton login --dry-run
```

Für den kurzen Aufruf `pass2sec` einmalig einen Link in einem Verzeichnis auf
`PATH` installieren (ohne vorhandene Dateien zu überschreiben):

```bash
mkdir -p "$HOME/.local/bin"
ln -s "$PWD/pass2sec" "$HOME/.local/bin/pass2sec"
pass2sec help
```

`$HOME/.local/bin` muss auf `PATH` stehen. Der Launcher verwendet `JAVA_HOME`,
alternativ `java` auf `PATH` oder das lokale IntelliJ-JDK unter
`~/.local/opt/idea/jbr`. Fehlt der Build, wird er mit Maven erstellt.
Nach Quellcodeänderungen erneut `mvn package` ausführen. Profile und der
Standard-Zustandspfad werden relativ zum Projektordner aufgelöst, auch beim Start
über den installierten Link. Optionen innerhalb der Sitzung gelten nur für den
aktuellen Befehl. Globale Startoptionen (z. B. `pass2sec --state-dir /privater/pfad`)
sind Sitzungsvorgaben; ein beim Start gesetztes `--dry-run` bleibt für die gesamte
Sitzung aktiv. Eigene Zustandspfade ebenfalls von Git und KI-Agenten ausschließen.

- JDK 25 und Maven 3.9.x.
- Offizielle [Proton Pass CLI](https://protonpass.github.io/pass-cli/get-started/installation/),
  verfügbar als `pass-cli`; für den Zugriff `pass2sec auth proton login` ausführen. Anmeldung,
  2FA und Proton-Sitzung verwaltet diese CLI. pass2sec speichert kein Proton-Masterpasswort.
- SimpleLogin-Konto oder vorhandener API-Key und mindestens eine Mailbox-ID aus deinem Konto.
- Linux/macOS mit POSIX-Dateirechten für das Wiederherstellungsjournal.
- Für interaktive Browser-Abläufe eine grafische Sitzung.

```bash
mvn package
# Chromium und gegebenenfalls die im Playwright-Fehler genannten Betriebssystembibliotheken installieren:
java -cp 'target/quarkus-app/lib/main/*' com.microsoft.playwright.CLI install chromium
java -jar target/quarkus-app/quarkus-run.jar --help
```

Die gesamte Struktur `target/quarkus-app/` wird zum Start benötigt.
Tests ohne Proton-/SimpleLogin-Zugang: `mvn test`.

## Hilfe auf jeder Befehlsebene

`help`, `--help` und `-h` zeigen die Hilfe zum aktuellen Befehl, einschließlich Optionen,
Voraussetzungen, Beispielen und nächsten Schritten. Die gesamte Einrichtung und alle
Befehlsgruppen stehen in der Einstiegshilfe:

```bash
java -jar target/quarkus-app/quarkus-run.jar help
java -jar target/quarkus-app/quarkus-run.jar auth help
java -jar target/quarkus-app/quarkus-run.jar auth simplelogin login help
java -jar target/quarkus-app/quarkus-run.jar help auth simplelogin login
java -jar target/quarkus-app/quarkus-run.jar rotate help
java -jar target/quarkus-app/quarkus-run.jar status --help
```

Nach `help` können vollständige Befehlspfade angegeben werden. Die Suche erfolgt erst relativ
zur aktuellen Ebene, danach vom Programmeinstieg aus: `auth proton help rotate` zeigt ebenfalls
die Migrationshilfe. Pflichtargumente und Anmeldung sind für Hilfe nicht nötig.
Hilfe läuft vollständig lokal, ohne Secret-Dateien zu lesen oder Änderungen auszuführen;
auch `rotate --execute help` startet keine Migration und `keygen help` erzeugt keinen Schlüssel.

In den anwendungseigenen Abfragen für E-Mail, Passwort, TOTP, API-Key und Journal-Schlüssel
kann ebenfalls `help` eingegeben werden. Anschließend wird dieselbe Eingabe erneut abgefragt;
der Hilfewert wird nicht an einen Anbieter gesendet oder gespeichert. Für den wörtlichen
Eingabewert `help` stattdessen `\help` eingeben. `Strg+C` bricht den Vorgang ab.
In der interaktiven Sitzung werden Befehle ohne den Präfix `pass2sec` eingegeben;
kopierte Beispiele mit diesem Präfix funktionieren ebenfalls. Der delegierte Proton-Login
und der Playwright Inspector verwenden ihre eigene
Benutzerführung, nicht die interne `help`-Eingabe.

## Probeläufe mit `--dry-run`

`--dry-run` funktioniert vor und nach dem Befehl, auch bei verschachtelten Befehlen. Der Modus
deaktiviert fachliche Änderungen und kennzeichnet die Ausgabe mit `DRY-RUN`. **Er hat Vorrang
vor `--execute`.** Pflichtargumente bleiben erforderlich. Für Details ohne Pflichtargumente
weiterhin `help` verwenden.

```bash
java -jar target/quarkus-app/quarkus-run.jar auth proton login --dry-run
java -jar target/quarkus-app/quarkus-run.jar --dry-run auth simplelogin login
java -jar target/quarkus-app/quarkus-run.jar auth simplelogin set-key --dry-run
java -jar target/quarkus-app/quarkus-run.jar keygen --dry-run
java -jar target/quarkus-app/quarkus-run.jar accounts --vault SHARE_ID --dry-run
java -jar target/quarkus-app/quarkus-run.jar rotate --dry-run \
  --vault SHARE_ID --item ITEM_ID --profile profiles/mein-dienst.json \
  --suffix @meine-domain.de --mailbox 123
java -jar target/quarkus-app/quarkus-run.jar recover --dry-run \
  --vault SHARE_ID --item ITEM_ID --profile profiles/mein-dienst.json
```

| Funktion | Verhalten im Dry-Run |
| --- | --- |
| Proton-Login/-Logout | Beschreibt den Aufruf; startet keine CLI und verändert keine Sitzung. Binary und Anmeldung werden nicht geprüft. |
| SimpleLogin-Login/Key-Einrichtung | Prüft lokale Optionen wie HTTPS-Adresse und Gerätename und beschreibt die Schritte; keine Geheimnisabfrage, keine HTTP-Anfrage, kein neuer API-Key und keine Datei. |
| `keygen` | Beschreibt die Erzeugung; erzeugt und zeigt keinen Schlüssel. |
| `vaults`, `accounts` | Führt die bestehenden lesenden Proton-CLI-Aufrufe aus. Eine gültige Proton-Sitzung ist weiterhin nötig. |
| `rotate` | Validiert Profile, Aliasformat und Mailbox-IDs, liest die Proton-Einträge und plant deren Zuordnung; keine Alias-/Passworterzeugung, kein Browser und kein Zugriff auf lokale Zugangsdaten oder das Journal. |
| `recover` | Liest das bestehende Journal und prüft Profil und gegebenenfalls aktuelle Tresorwerte. Zeigt den geplanten Abgleich; kein Browser-Login, kein Tresor-Write und kein Journal-Update. |
| `status` | Liest den lokalen Zustand ohne Netzwerkzugriff; erzeugt weder Verzeichnis noch Sperrdatei. |

`rotate` bleibt auch ohne `--dry-run` standardmäßig ein Plan; erst `--execute` ohne `--dry-run`
führt die Migration aus. Die Ausgabe trennt geprüfte Voraussetzungen von nicht geprüften
Punkten: Ein Migrationsplan bestätigt weder einen freien Alias noch funktionierende Website-Formulare.
Es gibt keine simulierte Bestätigung erfolgreicher Passwort- oder E-Mail-Änderungen.

Für `recover --dry-run` und für `status` bei vorhandenen Daten wird der bisherige Journal-Schlüssel
benötigt (verdeckte Eingabe oder `PASS2SEC_JOURNAL_KEY`). Der Speicher wird dabei nur lesend und
ohne Anlegen einer Sperrdatei geöffnet. Ein Probelauf ist eine Momentaufnahme; vor echten Änderungen
wird erneut verifiziert. Fehlender Zustand, unpassende Profile oder widersprüchliche Tresorwerte
werden als Fehler gemeldet. Nach `PREPARED`/`ALIAS_READY` ist weiterhin manueller Abgleich nötig.

Lesende Proton-Aufrufe verwenden die offizielle CLI; deren interne Sitzungs-/Cacheverwaltung
wird durch diesen Modus nicht unterbunden. Die Anwendung selbst schreibt im Dry-Run keine
Zugangsdaten, Journalstände oder Sperrdateien. Authentifizierungs-Probeläufe sind ausdrücklich
keine Prüfung der echten Zugangsdaten oder der Server-Erreichbarkeit.

## Zugang konfigurieren

Alle Einrichtungsbefehle laufen über die Anwendung. Die offizielle Proton-CLI muss lediglich
installiert sein. Starte die Anwendung aus dem Projektverzeichnis, damit die lokalen Zugangsdaten
im Projekt unter `.pass2sec/` gespeichert werden.

```bash
java -jar target/quarkus-app/quarkus-run.jar auth proton login
```

Dieser Befehl ruft `pass-cli login` mit durchgereichter Terminal-Ein-/Ausgabe auf. Die offizielle
CLI zeigt den Browser-Anmeldelink an und übernimmt Proton-Anmeldung, 2FA, Hardware-Schlüssel und
gegebenenfalls das zusätzliche Pass-Passwort. Ein bereits gesetztes
`PROTON_PASS_PERSONAL_ACCESS_TOKEN` wird wie bei einem direkten CLI-Aufruf vererbt.
Abmelden: `java -jar target/quarkus-app/quarkus-run.jar auth proton logout`.

Erzeuge vor der ersten lokalen Speicherung einmalig einen Journal-Schlüssel und sichere ihn
in einem separaten Passwortmanager oder einer anderen sicheren Ablage:

```bash
java -jar target/quarkus-app/quarkus-run.jar keygen
java -jar target/quarkus-app/quarkus-run.jar auth simplelogin login --device pass2sec
```

Die Anwendung fragt im Terminal nach dem Journal-Schlüssel, deiner SimpleLogin-E-Mail und dem
Passwort. Bei aktivierter TOTP-Authentifizierung folgt die Code-Abfrage. Der SimpleLogin-Login
erzeugt einen API-Key für das angegebene Gerät, prüft ihn über `/api/user_info` und speichert ihn
**verschlüsselt in einer Datei unter `.pass2sec/` im Projekt**. Der Dateiname ist ein SHA-256-Hash
der Server-Zuordnung mit Endung `.enc`; verschiedene SimpleLogin-Server haben getrennte Dateien.
Passwort und TOTP werden nicht auf Platte gespeichert. Der API-Key wird nicht ausgegeben.

Alternativ einen bereits vorhandenen API-Key verdeckt einlesen:

```bash
java -jar target/quarkus-app/quarkus-run.jar auth simplelogin set-key
```

Dieser Weg ist auch für Konten vorgesehen, deren FIDO-/SSO-Anmeldung über die SimpleLogin-API
nicht verfügbar ist. Den vorhandenen Key zunächst im SimpleLogin-Konto erzeugen und dann an der
verdeckten Eingabe einfügen. Es gibt keine Passwort- oder API-Key-Kommandozeilenoptionen.
Ungültige Zugangsdaten überschreiben einen bereits gespeicherten Key nicht.
Falls ein Remote-Login erfolgreich war, aber die anschließende Speicherung scheitert,
gegebenenfalls den neu erstellten Geräte-Key im SimpleLogin-Konto prüfen und widerrufen.

**Für spätere Aufrufe denselben Journal-Schlüssel verwenden.** Ohne ihn sind der gespeicherte API-Key
und offene Vorgänge nicht lesbar. Der Schlüssel wird selbst nicht im Projekt gespeichert. Fehlt
`PASS2SEC_JOURNAL_KEY`, wird er bei Bedarf verdeckt im Terminal abgefragt. In IDE-Ausgabefenstern
ohne echtes Terminal stehen diese verdeckten Eingaben nicht zur Verfügung.

Für unbeaufsichtigte Aufrufe weiterhin `PASS2SEC_JOURNAL_KEY` aus einer geeigneten Secret-Verwaltung
setzen. Optional überschreibt `SIMPLELOGIN_API_KEY` den lokal gespeicherten API-Key. Ohne diese
Variable verwendet `rotate --execute` automatisch die verschlüsselt gespeicherten Zugangsdaten.
Eine eigene Instanz lässt sich bei Einrichtung und Migration mit derselben
`--simplelogin-url https://eigene-instanz.example` auswählen.

## Git- und Agent-Ausschlüsse

`.pass2sec/`, lokale `.env`-Dateien und `*.key` sind in `.gitignore` ausgeschlossen.
Dieselben Geheimnispfade stehen in `.aiignore`, `.aiexclude`, `.cursorignore` und `.geminiignore`.
[AGENTS.md](AGENTS.md) untersagt Agenten zusätzlich ausdrücklich das Lesen, Durchsuchen,
Entschlüsseln oder Hochladen dieser Pfade, auch über Terminalwerkzeuge.

Die Ignore-Regeln müssen vom jeweiligen Agenten unterstützt und aktiviert werden. Sie sind
keine Betriebssystem-Zugriffssperre und verhindern insbesondere keine beliebigen Shell-Zugriffe.
Bei JetBrains die Einstellung „Enable .aiignore“ prüfen; siehe
[JetBrains-Dokumentation](https://www.jetbrains.com/help/ai-assistant/disable-ai-assistant.html).
Bei einem eigenen `--state-dir` dessen Pfad zusätzlich in Git- und Agent-Ausschlüsse aufnehmen.
Bereits zuvor eingecheckte Geheimnisse werden durch Ignore-Dateien nicht aus der Git-Historie entfernt.

## Fachliche Struktur mit BCE

Die erste Paketebene richtet sich nach fachlichen Themen. Innerhalb eines Themas werden Boundary,
Control und Entity verwendet, soweit dort benötigt:

```text
de.jsc3r.pass2sec
├── authentication
│   ├── boundary       # Login-CLI, Terminal, SimpleLogin-Authentifizierung, Key-Datei
│   ├── control        # Anmeldung, Validierung und Speicherung koordinieren
│   └── entity         # API-Zugangsdaten
├── vault
│   ├── boundary       # Tresor-CLI, Proton-Prozessadapter und JSON-Decoder
│   ├── control/port   # Benötigte Tresoroperationen
│   └── entity         # Account, Vault und Summary
├── alias
│   ├── boundary       # SimpleLogin-Alias-API
│   ├── control/port   # Aliasoperationen
│   └── entity         # AliasPlan
├── migration
│   ├── boundary       # Migrations-CLI, Browser, Journal
│   ├── control        # Planung, Rotation, Verifikation, Commit und Wiederherstellung
│   └── entity         # Rotation, Zustände, Plan und Ergebnis
├── profile
│   ├── boundary       # JSON-Profile laden
│   └── entity         # Profile, Step, Action und fachliche Validierung
├── shared             # Gemeinsam genutzte technische Komponenten, ebenfalls mit BCE
└── application
    └── boundary/cli   # CLI-Einstieg und Zusammenbau der Komponenten
```

Eine Klasse, ein Record, Enum oder Interface je Datei. Controls erhalten Abhängigkeiten über
Konstruktoren und verwenden Port-Interfaces statt konkreter Boundary-Adapter. Dadurch lassen sich
HTTP, Prozesse, Benutzerabfragen und Speicherung unabhängig testen. Die gemeinsame Commit-Logik
wird von Rotation und Wiederherstellung verwendet; die Wiederherstellung selbst besitzt keine
Abhängigkeit zur Alias-Erstellung. `SafeCommand` vereinheitlicht Fehlerbehandlung und Exit-Codes
über ein Template Method. Die Entities führen kein I/O aus.

Die gewünschte Alias-Endung muss exakt in den [SimpleLogin-Aliasoptionen](https://github.com/simple-login/app/blob/master/docs/api.md)
deines Kontos vorkommen. `--suffix @simplelogin.de` ist nur verwendbar, wenn dir diese Domain
tatsächlich zur Verfügung steht. Andere Endungen können beispielsweise `.wort@aleeas.com`
sein; aus dem Präfix `github` entsteht dann `github.wort@aleeas.com`.
Es wird kein Ersatzalias geraten, wenn die angeforderte Adresse nicht verfügbar ist.

## Einträge auswählen und planen

```bash
java -jar target/quarkus-app/quarkus-run.jar vaults
java -jar target/quarkus-app/quarkus-run.jar accounts --vault SHARE_ID
```

`--vault` erwartet die **Share-ID**, nicht die davon abweichende Vault-ID. `accounts` listet nur
aktive Login-Einträge und deren Titel/IDs auf, keine Passwörter.

Kopiere [profiles/example.json](profiles/example.json) und passe das Profil an die Website an.
Plane zunächst einen einzelnen Account:

```bash
java -jar target/quarkus-app/quarkus-run.jar rotate \
  --vault SHARE_ID --item ITEM_ID \
  --profile profiles/mein-dienst.json \
  --suffix @meine-domain.de --mailbox 123
```

Ohne `--execute` wird nur gelesen und der lokale Plan ausgegeben. Es werden weder Aliase noch
Passwörter erzeugt und kein Browser gestartet. Der Plan prüft Profilzuordnung und Konfiguration;
er bestätigt noch nicht, dass der Alias frei ist oder die Website-Selektoren funktionieren.
Für das Lesen der ausgewählten Einträge werden dabei Zugangsdaten in den Arbeitsspeicher geladen.

Mit denselben Argumenten und zusätzlich `--execute` wird die Migration ausgeführt. Ohne `--item`
werden alle aktiven Logins des ausgewählten Tresors berücksichtigt. Mehrere Profile können mit
wiederholtem `--profile` angegeben werden. Unbekannte Websites werden übersprungen; mehrdeutige
Profile und mehrere Accounts mit demselben vorgesehenen Alias führen vor der Ausführung zum Abbruch.
Für mehrere Accounts derselben Website unterschiedliche Präfixe und eine eingegrenzte Auswahl verwenden.

`--pass-cli /absoluter/pfad/pass-cli` wählt ein anderes CLI-Binary. `--state-dir /privater/pfad`
wählt ein anderes Journal-Verzeichnis. `--simplelogin-url https://eigene-instanz.example`
unterstützt eine eigene SimpleLogin-Instanz mit HTTPS.

## Website-Profile

Profile sind vertrauenswürdige lokale Automatisierungskonfiguration. Prüfe die Schritte selbst;
ein falsches Profil kann den falschen Account ändern oder einen ungeeigneten Erfolgsindikator prüfen.
Keine ungeprüften Profile aus Website-Inhalten übernehmen.

- `hosts`: exakte HTTPS-Hosts für Navigation und Browser-Netzwerkzugriffe. Auch notwendige
  Asset-/Authentifizierungs-Hosts müssen enthalten sein. Eine gespeicherte Account-URL muss
  dazu passen. Wildcards und HTTP sind nicht erlaubt.
- `aliasPrefix`: beispielsweise `github`; wird mit der gewählten Endung kombiniert.
- `updateUsername`: `true`, wenn auch das Proton-Feld `username` auf die neue Mailadresse
  gesetzt werden soll. Bei stabilen Benutzernamen wie einem GitHub-Handle `false` verwenden.
- `passwordLength`: 16–128 Zeichen, Standard in der Vorlage 24. Erzeugung mit Zahlen,
  Großbuchstaben und Symbolen durch `pass-cli password generate random`.
- `timeoutSeconds`: Wartezeit je Browser-Schritt, 1–600 Sekunden.
- `change`: Login und dienstspezifische Änderungen einschließlich Bestätigungen.
- `verify`: erneuter Login mit dem neuen Passwort und Prüfung der neuen Adresse,
  in einer **frischen Browser-Sitzung ohne übernommene Cookies**.

Schritte: `NAVIGATE` (absolute URL), `FILL` (Selektor und Wert), `CLICK`, `WAIT_VISIBLE`,
`ASSERT_TEXT` (exakter Text) und `PAUSE` (manueller Schritt mit Playwright Inspector).
Selektoren dürfen beispielsweise CSS- oder Playwright-Textselektoren sein.
Die Variablen werden als vollständiger `value` verwendet, ohne Verkettung:

| Variable | Inhalt |
| --- | --- |
| `${username}` | Ursprünglicher Benutzername |
| `${oldEmail}` | Ursprüngliches Proton-E-Mail-Feld |
| `${oldPassword}` | Ursprüngliches Passwort |
| `${newEmail}` | Neu angelegter SimpleLogin-Alias |
| `${newPassword}` | Von Proton erzeugtes Passwort |

Bei einem leeren E-Mail-Feld und einer Mailadresse im Benutzernamen im alten Login `${username}`
verwenden. Bei einer Website, deren Login-ID sich mit der Adresse ändert, muss die Verifikation
`${newEmail}` verwenden. Für einen stabilen Handle kann die Verifikation `${username}` verwenden.
Die Verifikation muss das neue Passwort absenden und anschließend die neue Adresse über
`ASSERT_TEXT` auf einer authentifizierten Account-Seite prüfen. Nicht nur eine Erfolgsmeldung prüfen.

`PAUSE` erlaubt 2FA, CAPTCHA oder die Bestätigung eines Mail-Links durch den Benutzer.
Danach im Inspector mit „Resume“ fortsetzen. Es gibt keine automatisierte Proton-Mail-Inbox-Anbindung
und keine CAPTCHA-Umgehung. Bei GitHub muss der neue Alias zusätzlich bestätigt und als primär
gesetzt werden; ein bloßes Hinzufügen einer weiteren Adresse reicht nicht für die gewünschte Migration.
Siehe [GitHub: primäre E-Mail-Adresse](https://docs.github.com/en/account-and-profile/setting-up-and-managing-your-personal-account-on-github/managing-email-preferences/changing-your-primary-email-address)
und [Passwort ändern](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/updating-your-github-access-credentials).

Ohne manuelle Schritte kann ein entsprechend getestetes Profil mit `--headless` laufen.
Profile mit `PAUSE` werden dafür bereits vor der Alias-Erstellung abgelehnt.
Browser-Profile, Cookies, Screenshots und Traces werden nicht dauerhaft gespeichert.

## Abbrüche und Wiederherstellung

Der Ablauf ist `PREPARED → ALIAS_READY → WEBSITE_STARTED → WEBSITE_VERIFIED → COMMITTED`.
Vor dem ersten Schreibzugriff werden alte und neue Zugangsdaten mit AES-256-GCM verschlüsselt
im Journal gespeichert. Dateien erhalten 0600, das Verzeichnis 0700; atomarer Austausch und
Prozesssperre schützen den lokalen Zustand. Dieselbe Journal-Ablage für weitere Aufrufe verwenden.

```bash
java -jar target/quarkus-app/quarkus-run.jar status --vault SHARE_ID --item ITEM_ID
java -jar target/quarkus-app/quarkus-run.jar recover \
  --vault SHARE_ID --item ITEM_ID --profile profiles/mein-dienst.json
```

`recover` ab `WEBSITE_STARTED` führt ausschließlich das konfigurierte Verifikationsprofil aus
und gleicht anschließend den Tresor ab. Es erzeugt keinen zweiten Alias und wiederholt den
Passwortwechsel nicht. Selektoren im Profil können bei Bedarf korrigiert werden; Profilname und
`updateUsername` müssen zum gespeicherten Vorgang passen. Bereits erfolgreich geschriebene
Tresorwerte werden erkannt, etwa wenn die Erfolgsantwort verloren ging.

Nach `COMMITTED` wird derselbe Eintrag bei weiteren `rotate`-Aufrufen mit diesem Journal übersprungen.
Das Journal wird bewusst behalten und enthält weiterhin verschlüsselte alte/neue Zugangsdaten.

Bei einem Abbruch vor `WEBSITE_STARTED` oder einer Teiländerung, etwa E-Mail geändert,
Passwort aber noch alt, gibt es **keine automatische Wiederholung oder Rücksetzung**.
SimpleLogin-/Website-Zustand und Mail-Bestätigungen müssen dann manuell abgeglichen werden.
Das Journal erhalten; den offenen Zustand nicht durch Löschen und erneutes Starten umgehen.
Eine automatisierte Reparatur solcher Teiländerungen sowie eine Klartext-Exportfunktion sind
derzeit nicht implementiert. Bestehende Aliase werden weder gelöscht noch deaktiviert.

Ein zwischenzeitlich abweichender Tresoreintrag wird vor dem Schreiben erkannt und nicht überschrieben.
Die offizielle CLI bietet hier keinen atomaren Compare-and-Swap: Während der Migration den Account
nicht parallel in anderen Proton-Clients bearbeiten. Externe Website- und Tresoränderungen sind keine
gemeinsame Transaktion; dafür dient die explizite Wiederherstellung.

Passwörter und API-Antworten werden nicht protokolliert. **Die offizielle `item update`-Schnittstelle
übernimmt Feldwerte als Prozessargumente**, weshalb das neue Passwort während dieses kurzen Aufrufs
für entsprechend berechtigte lokale Prozesse sichtbar sein kann. Die Anwendung startet keine Shell
und schreibt keine Passwortargumente in die Shell-History. Nur auf einem vertrauenswürdigen Rechner ausführen.

Verwendete Schnittstellen: [Proton Items](https://protonpass.github.io/pass-cli/commands/item/),
[Proton Passwortgenerator](https://protonpass.github.io/pass-cli/commands/password/),
[SimpleLogin API](https://github.com/simple-login/app/blob/master/docs/api.md),
[Playwright Java](https://playwright.dev/java/docs/intro).
