# Card Client Lib Java

This library contains an example implementation of the client module for the communication with smartcard hardware like eGK, SM(C)-B.
This can also connect to card simulator resources

---

## Nutzung der Bibliothek (Deutsch)

Diese Bibliothek ermöglicht die Kommunikation mit Smartcard-Hardware (z.B. eGK, SMC-B) sowie mit Kartensimulatoren. Nachfolgend finden Sie eine Schritt-für-Schritt-Anleitung zur Verwendung.

### 1. Installation

Fügen Sie die Bibliothek zu Ihrem Projekt als Maven-Dependency hinzu oder binden Sie das JAR direkt ein.

```xml
<dependency>
    <groupId>de.gematik.ti20</groupId>
    <artifactId>card-client-lib-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Initialisierung

Erstellen Sie eine Konfiguration für das Terminal und initialisieren Sie ein `CardTerminal`-Objekt:

```java
CardTerminalConfig config = new CardTerminalConfig();
config.setType(CardTerminal.Type.SIMULATOR); // oder z.B. REAL
config.setSerialNumber("123456");
// ggf. weitere Konfigurationen setzen

CardTerminal terminal = new CardTerminal(config);
```

### 3. Verbindung zu einem Slot herstellen

```java
String slotId = "slot1";
terminal.connect(slotId, new TerminalSlot.TerminalSlotEventHandler() {
    @Override
    public void onCardInserted(TerminalSlot slot) {
        System.out.println("Karte eingesteckt.");
    }
    @Override
    public void onCardRemoved(TerminalSlot slot) {
        System.out.println("Karte entfernt.");
    }
});
```

### 4. Kommando an die Karte senden

```java
CardCommand command = new CardCommand(CardCommand.Type.GET_CARD_DATA);
command.addArg("key", "value");
TerminalSlot slot = terminal.getSlot(slotId);
slot.send(command);
```

### 5. Ergebnis empfangen

Das Ergebnis kann über Rückgabewerte oder Events verarbeitet werden. Details dazu finden Sie in der jeweiligen Slot-Implementierung.

### 6. Trennen der Verbindung

```java
terminal.disconnect(slotId);
```

### Hinweise
- Für mehrere Terminals kann `CardTerminalService` verwendet werden.
- Die Bibliothek unterstützt verschiedene Kartentypen (eGK, SMC-B, SMC-K).
- Für den produktiven Einsatz sollten Exception-Handler und Logging integriert werden.

---

Weitere Details finden Sie im Quellcode und in der Javadoc-Dokumentation.