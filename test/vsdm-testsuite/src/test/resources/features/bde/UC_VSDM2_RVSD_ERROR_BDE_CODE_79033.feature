#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM2
@TYPE:BDE
Funktionalität: Betriebsdatenerfassung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_BDE_CODE_79033
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: BDE Fehlercode 79033 - E-Tag Fehler

  Dieser Testfall beschreibt ein Fehlerszenario, das durch ein fehlendes Entity-Tag verursacht wird. Obwohl ein gültiger
  Access- und PoPP-Token für die Abfrage der VSD vom VSDM Ressource Server vorhanden sind, kann dieser die Anfrage nicht
  beantworten, weil das Primärsystem kein Entity-Tag in den HTTP Header-Daten gesendet hat.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem sich mit seiner SMC-B beim ZETA Guard des VSDM 2.0 Fachdienstes authentifiziert
    Dann erhält das Primärsystem einen Access- und Refresh-Token vom ZETA Guard
    Wenn das Primärsystem die VSD mit einem fehlenden E-Tag vom VSDM Ressource Server abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <Bde-Text>

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Http-Code | Bde-Text                                    |
      | "smcbCardImage.xml" | 1         | "egkCardImage.xml" | 2        | 428       | "VSDSERVICE_INVALID_PATIENT_RECORD_VERSION" |
