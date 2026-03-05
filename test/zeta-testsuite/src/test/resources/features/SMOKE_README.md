# Smoke Tests - Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `smoke.feature`.

## Übersicht

Das Feature testet die **Erreichbarkeit** der ZeTA-Komponenten:
- Einfache HTTP GET-Anfragen an Health-/Status-Endpoints
- Prüfung auf HTTP 200 Response
- Ausgabe der Antwortzeit

---

## Szenario: Availability Check (@smoke)

**Tag:** `@smoke`

Dieses Szenario prüft, ob alle ZeTA-Komponenten erreichbar sind und korrekt antworten.

### Getestete Komponenten

| Komponente | Container | Port | Beschreibung |
|------------|-----------|------|--------------|
| ZeTA PDP PoPP | `popp-zeta-pdp` | 9112 | Policy Decision Point – trifft Autorisierungsentscheidungen für PoPP |
| ZeTA PEP PoPP | `popp-zeta-pep` | 9110 | Policy Enforcement Point – setzt Zugriffskontrolle für PoPP durch |
| ZeTA PDP Ingress | `vsdm-zeta-ingress` | 9119 | Ingress für VSDM – stellt `.well-known`-Endpunkte bereit |

### Flow-Diagramm

```
┌─────────┐         ┌─────────────────────┐
│  Test   │         │  Ziel-Komponente    │
│ Client  │         │  (PDP/PEP/Ingress)  │
└────┬────┘         └──────────┬──────────┘
     │                         │
     │  1. HTTP GET            │
     │  /service/status        │
     │  oder /.well-known/...  │
     │────────────────────────>│
     │                         │
     │                         │  2. Health Check
     │                         │     ausführen
     │                         │
     │  3. HTTP 200 OK         │
     │<────────────────────────│
     │                         │
     │  4. Antwortzeit         │
     │     ausgeben            │
     │                         │
```

## Prüfkriterien

| Kriterium | Erwarteter Wert |
|-----------|-----------------|
| HTTP Version | HTTP/1.1 |
| Response Code | 200 |
| Antwortzeit | Wird ausgegeben (keine feste Grenze) |

---

## Test ausführen

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@smoke'
```

---

## Zusammenfassung

| Komponente | Endpoint | Erwartung |
|------------|----------|-----------|
| ZeTA PDP PoPP | `/service/status` | HTTP 200 |
| ZeTA PEP PoPP | `/service/status` | HTTP 200 |
| ZeTA PDP Ingress | `/.well-known/oauth-protected-resource` | HTTP 200 |

**Zweck:** Schnelle Überprüfung, ob alle ZeTA-Dienste gestartet und erreichbar sind, bevor komplexere Tests ausgeführt werden.
