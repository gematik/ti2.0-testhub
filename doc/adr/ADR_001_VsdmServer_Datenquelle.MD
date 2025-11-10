# ADR: Datenquelle für den VSDM‑Server

Status: Accepted

## Kontext

Der VSDM‑Server versucht bei Anfragen mit einer KVNR zuerst, passende Testdaten aus dem Resources‑Verzeichnis zu laden (
Klasse `TestDataRepository`). Sind dort keine Daten vorhanden, werden synthetische Daten erzeugt. Das Verhalten kann
über
`application.yaml` mittels Prefixes der KVNR gesteuert werden.

## Entscheidung

- Primäre Datenquelle: YAML‑Dateien im Resources‑Ordner `src/main/resources/de/gematik/vsdm/testdata/person/patient/` (
  Package: `de.gematik.vsdm.testdata.person.patient`).
- Laden: `TestDataRepository` sucht nach einer Datei, die zur übergebenen KVNR passt (z.\,B. `{kvnr}.yaml` oder einer
  konfigurierten Namenskonvention).
- Fallback: Wenn keine Datei gefunden wird, erzeugt der Server synthetische Daten.
- Konfiguration: Verhalten gegenüber KVNRs wird über `vsdm.valid-kvnr-prefix` und `vsdm.invalid-kvnr-prefix` in
  `application.yaml` gesteuert.
- Fehlercodes: Bei fehlendem Patientendatensatz wird ein `OperationOutcome` mit dem Fehlercode
  `VSDSERVICE_PATIENT_RECORD_NOT_FOUND` zurückgegeben. Für alle anderen ungültigen KVNRs wird `OperationOutcome` mit
  `VSDSERVICE_INVALID_KVNR` zurückgegeben.

## Struktur der YAML‑Dateien

Im Ordner `src/main/resources/de/gematik/vsdm/testdata/person/patient/` liegen YAML‑Dateien pro Patient.

- `kvnr`: KVNR (Identifier)
- `name`:
    - `family`: Nachname
    - `given`: Liste der Vornamen
- `gender`: `m`|`w`|`other`|`unknown`
- `birthDate`: `YYYY-MM-DD`
- `address`, `telecom`, `insurer` (iknr)

Beispiel `src/main/resources/de/gematik/vsdm/testdata/person/patient/patient3.yaml`:

    personData:
      gender: w
      name:
        given: Kriemhild
        family: Amelie Abigail H. Freifrau Bruser
      birthDate: 2015-09-02
      kvnr: X110639491
      address:
        post:
          zip: 12345
          city: Berlin
          line1: Friedrichstr. 123
          line2: Wohnung 456
      insurance: Insurance Gematik

## Wie zusätzliche Patientendaten hinzugefügt werden können

1. Datei anlegen im Pfad `src/main/resources/de/gematik/vsdm/testdata/person/patient/`.
2. Dateiname: Beliebig, aber nach Konvention `patient*.yaml` (z.\,B. `patient123.yaml`).
3. YAML gemäß obigem Schema befüllen.
4. Projekt neu bauen / neu starten, damit die Resources geladen werden.

## Verhalten bei Anfragen (Reihenfolge)

1. `TestDataRepository` versucht, die YAML‑Datei für die KVNR zu laden.
    - Gefunden: Server liefert die geladenen Ressourcen (HTTP 200).
2. Nicht gefunden:
    - Wenn `kvnr.startsWith(vsdm.invalid-kvnr-prefix)` → Fehlerantwort:
        - HTTP 400 mit `OperationOutcome` und Code `VSDSERVICE_PATIENT_RECORD_NOT_FOUND`.
    - Sonst, wenn `kvnr.startsWith(vsdm.valid-kvnr-prefix)` → synthetische Daten (HTTP 200).
        - Synthetische Regeln: z.\,B. `family` wird `family-name-{kvnr}`, `given` = `given-name-{kvnr}`, `id` =
          `patient-{kvnr}`, Payor iknr kann Default sein.
    - Sonst → Fehlerantwort:
        - HTTP 400 mit `OperationOutcome` und Code `VSDSERVICE_INVALID_KVNR`.

Hinweis: In der Implementierung ist die Invalid‑Prefix‑Prüfung vor der Valid‑Prefix‑Prüfung angeordnet (Priorität:
`invalid` zuerst).

## Beispiel Rückgabedaten (synthetisch)

Kurzform eines `VsdmBundle` (vereinfachte JSON‑Darstellung):

    {
      "resourceType": "Bundle",
      "type": "collection",
      "entry": [
        {
          "resource": {
            "resourceType": "Patient",
            "id": "patient-12345",
            "identifier": [{ "system": "http://example.org/kvnr", "value": "12345" }],
            "name": [{ "family": "family-name-12345", "given": ["given-name-12345"] }]
          }
        },
        {
          "resource": {
            "resourceType": "Organization",
            "id": "org-107723372",
            "identifier": [{ "system": "http://example.org/iknr", "value": "107723372" }],
            "name": "Test GKV Krankenkasse"
          }
        },
        {
          "resource": {
            "resourceType": "Coverage",
            "id": "coverage-12345",
            "status": "active",
            "payor": [{ "reference": "Organization/org-107723372" }],
            "beneficiary": { "reference": "Patient/patient-12345" }
          }
        }
      ]
    }

## Konsequenzen

- Testdatenpflege ist einfach: YAML‑Dateien in Resources ablegen.
- Klare Priorisierung von echten Testdaten vor synthetischen Daten.
- Konfiguration via `application.yaml` macht Verhalten im Betrieb steuerbar.
