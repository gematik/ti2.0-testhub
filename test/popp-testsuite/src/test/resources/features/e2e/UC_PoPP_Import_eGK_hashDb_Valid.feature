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

  Szenariogrundriss: Erfolgreicher Import von Daten in eGK-Hash-Datenbank

  Dieser Testfall testet den Anwendungsfall Import von Daten in eGK-Hash-Datenbank.
  Ein TSP möchte Daten in die eGK-Hash-Datenbank importieren.

  Der TSP etabliert einen TLS Kanal mit dem PoPP-Service. Der TSP ruft die Schnittstelle I_PoPP_EHC_CertHash_Import
  auf um die Daten zu importieren.

    Angenommen die Schnittstelle I_PoPP_EHC_CertHash_Import ist beim PoPP-Service verfügbar
    Angenommen der TSP etabliert einen TLS Kanal mit dem PoPP-Service mit einem gültigen Client-Zertifikat
    Wenn der TSP die Schnittstelle I_PoPP_EHC_CertHash_Import <Anzahl> mit dem DER-codierten, signierten Payload <eContent> aufruft
    Dann erhält der TSP eine Rückmeldung mit den folgenden Informationen:
      | Typ        | Anzahl |
      | Importiert | <Anzahl> |
      | Fehlerhaft | 0       |
      | Ignoriert  | 0       |
      | Geblockt   | 0       |


    Beispiele:
        | Anzahl    | eContent   | @onlyThis |
        | 1         | eContent |   @run    |