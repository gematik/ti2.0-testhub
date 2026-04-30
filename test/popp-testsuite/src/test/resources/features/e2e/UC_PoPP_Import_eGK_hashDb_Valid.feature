#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP
@TYPE:E2E
Funktionalität: Import von Daten in eGK-Hash-Datenbank

  @TCID:UC_PoPP_Import_eGK_hashDb_Valid
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenario: Erfolgreicher Import von Daten in eGK-Hash-Datenbank

  Dieser Testfall testet den Anwendungsfall Import von Daten in eGK-Hash-Datenbank.
  Ein TSP möchte Daten in die eGK-Hash-Datenbank importieren.

  Der TSP etabliert einen TLS Kanal mit dem PoPP-Service. Der TSP ruft die Schnittstelle I_PoPP_EHC_CertHash_Import
  auf um die Daten zu importieren.

    Angenommen TGR lösche aufgezeichnete Nachrichten
    Und der TSP verwendet die Client Identität "tspEgkTlsValid" für die mTLS-Verbindung zum PoPP-Service
    Und der TSP sendet den signierten eContent "80276883110000144098.eContent-signed" zum importieren an den PoPP Service
    Dann der TSP erhält eine positive Rückmeldung mit einer jobID
    Und TGR set local variable "job_id_import" to "!{rbel:currentResponseAsString('$.body.jobId')}"
    Wenn der TSP fragt den Status seines Imports oder seiner Löschung ab
    Dann der TSP erhält Informationen über den Status seines Imports
    Und der TSP löscht den abgeschlossenen Auftrag mit der JobId "${job_id_import}"


  @TCID:UC_PoPP_Delete_eGK_hashDb_Valid
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenario: Erfolgreiches Löschen von Daten in eGK-Hash-Datenbank

  Dieser Testfall testet den Anwendungsfall Löschen von Daten in eGK-Hash-Datenbank.
  Ein TSP möchte Daten aus der eGK-Hash-Datenbank löschen.

  Der TSP etabliert einen TLS Kanal mit dem PoPP-Service. Der TSP ruft die Schnittstelle I_PoPP_EHC_CertHash_Import
  auf um die Daten zu löschen.

    Angenommen TGR lösche aufgezeichnete Nachrichten
    Und der TSP verwendet die Client Identität "tspEgkTlsValid" für die mTLS-Verbindung zum PoPP-Service
    Und der TSP sendet den signierten eContent "80276883110000144099.eContent-signed" zum importieren an den PoPP Service
    Dann der TSP erhält eine positive Rückmeldung mit einer jobID
    Und TGR find first request to path "/api/v1/hash-db/import"
    Und TGR set local variable "job_id_import" to "!{rbel:currentResponseAsString('$.body.jobId')}"
    Und warte für "30" Sekunden
    Wenn der TSP fragt das Ergebnis des Jobs mit der jobID "${job_id_import}" ab
    Dann der TSP erhält Informationen über das Ergebnis seines Imports
    Und der TSP löscht den abgeschlossenen Auftrag mit der JobId "${job_id_import}"
    Und TGR lösche aufgezeichnete Nachrichten
    Wenn der TSP sendet den signierten eContent "80276883110000144099.eContent-signed" zum löschen an den PoPP Service
    Dann der TSP erhält eine positive Rückmeldung mit einer jobID
    Und TGR find first request to path "/api/v1/hash-db/import"
    Und TGR set local variable "job_id_delete" to "!{rbel:currentResponseAsString('$.body.jobId')}"
    Und warte für "30" Sekunden
    Wenn der TSP fragt das Ergebnis des Jobs mit der jobID "${job_id_delete}" ab
    Und der TSP löscht den abgeschlossenen Auftrag mit der JobId "${job_id_delete}"