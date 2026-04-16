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
    Wenn der TSP sendet den signierten eContent "80276883110000144098.eContent-signed" an den PoPP Service
    Dann der TSP erhält eine positive Rückmeldung mit einer jobID
    Wenn der TSP fragt den Status seines Imports ab
    Dann der TSP erhält Informationen über den Status seines Imports


  @TCID:UC_PoPP_Import_eGK_hashDb_Result
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenario: Result eines vorherigen erfolgreich Imports in eGK-Hash-Datenbank

  Dieser Testfall testet den Anwendungsfall Import von Daten in eGK-Hash-Datenbank.
  Ein TSP hat bereits zuvor Daten in die eGK-Hash-Datenbank importieren. Jetzt möchte er das Ergebnis des Imports wissen.

  Der TSP etabliert einen TLS Kanal mit dem PoPP-Service. Der TSP ruft die Schnittstelle I_PoPP_EHC_CertHash_Import
  auf, um das Resultat des Imports abzufragen.

    Angenommen TGR lösche aufgezeichnete Nachrichten
    Und der TSP verwendet die Client Identität "tspEgkTlsValid" für die mTLS-Verbindung zum PoPP-Service
    Wenn der TSP fragt das Ergebnis seines erfolgreichen Imports ab
    Dann der TSP erhält Informationen über das Ergebnis seines Imports


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

    Angenommen der TSP verwendet die Client Identität "tspEgkTlsValid" für die mTLS-Verbindung zum PoPP-Service
    Wenn der TSP sendet einen Löschauftrag
    Dann der TSP erhält eine positive Rückmeldung zu seiner Löschung