#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP_Service
@TYPE:E2E
Funktionalität: Import und löschen von Daten in eGK-Hash-Datenbank

  @TCID:UC_PoPP_Import_Delete_hashDb_Valid
  @STATUS:Implementiert
  @MODUS:Manuell
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenario: Erfolgreicher Import löschen von Daten in eGK-Hash-Datenbank

  Dieser Testfall testet den Anwendungsfall Import und anschließendes löschen von Daten in eGK-Hash-Datenbank.
  - Ausgangszustand: eGK ist nicht in der hashDB bekannt
  - Ein TSP möchte Daten in die eGK-Hash-Datenbank importieren.
  - Verifizieren durch erfolgreiches anfordern eines PoPP-Token
  - Ein TSP möchte die Daten aus der hashDB löschen
  - Daten sind erfolgreich gelöscht wenn mit der Karte kontaktlos kein PoPP-Token angefordert werden kann

  Der TSP etabliert einen TLS Kanal mit dem PoPP-Service. Der TSP ruft die Schnittstelle I_PoPP_EHC_CertHash_Import
  auf um die Daten zu importieren.

    Angenommen TGR lösche aufgezeichnete Nachrichten
    Und der TSP verwendet die Client Identität "tspEgkTlsValid" für die mTLS-Verbindung zum PoPP-Service
    Wenn der TSP sendet den signierten eContent "80276883110000163142.eContent-signed" zum löschen an den PoPP Service
    Dann der TSP erhält eine positive Rückmeldung mit einer jobID
    Und TGR finde die erste Anfrage mit Pfad "/api/v1/hash-db/import"
    Und TGR setze lokale Variable "job_id_delete" auf "!{rbel:currentResponseAsString('$.body.jobId')}"
    Und warte für "30" Sekunden
    Wenn der TSP fragt das Ergebnis des Jobs mit der jobID "${job_id_delete}" ab
    Und der TSP löscht den abgeschlossenen Auftrag mit der JobId "${job_id_delete}"
    Und der Versicherte in der LEI präsentiert seine eGK "virtuell" am Lesegerät "kontaktlos"
    Und Das Primärsystem den PoPP-Token mit Image "EGK_80276883110000163142" vom PoPP-Service abgefragt
    Dann erhält das Primärsystem den Status ERROR vom PoPP-Service mit Message "Unexpected error: Server error WarningUnknownCertificates: "
    Wenn TGR lösche aufgezeichnete Nachrichten
    Und der TSP sendet den signierten eContent "80276883110000163142.eContent-signed" zum importieren an den PoPP Service
    Dann der TSP erhält eine positive Rückmeldung mit einer jobID
    Und TGR finde die erste Anfrage mit Pfad "/api/v1/hash-db/import"
    Und TGR setze lokale Variable "job_id_import" auf "!{rbel:currentResponseAsString('$.body.jobId')}"
    Und warte für "30" Sekunden
    Wenn der TSP fragt das Ergebnis des Jobs mit der jobID "${job_id_import}" ab
    Und der TSP löscht den abgeschlossenen Auftrag mit der JobId "${job_id_import}"

    Und der Versicherte in der LEI präsentiert seine eGK "virtuell" am Lesegerät "kontaktlos"
    Und Das Primärsystem den PoPP-Token mit Image "EGK_80276883110000163142" vom PoPP-Service abgefragt
    Dann das PoPP-Token ist vollständig und spezifikationskonform





