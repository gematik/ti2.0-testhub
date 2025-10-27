#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM2
@TYPE:BDE
Funktionalität: Betriebsdatenerfassung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_BDE_CODE_79011
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: BDE Fehlercode 79011 - KVNR Fehler

  Dieser Testfall beschreibt ein Fehlerszenario, das durch eine unbekannte KVNR-Nummer verursacht wird, welche im
  PoPP-Token enthalten ist und vom VSDM Ressource Server nicht verarbeitet werden kann. Ursache für einen solchen
  Fehlerfall könnte z.B. eine abgelaufene Versicherung oder ein Versichertenwechsel sein.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem sich mit seiner SMC-B beim ZETA Guard des VSDM 2.0 Fachdienstes authentifiziert
    Dann erhält das Primärsystem einen Access- und Refresh-Token vom ZETA Guard
    Wenn das Primärsystem die VSD mit einer falschen KVNR-Nummer vom VSDM Ressource Server abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <Bde-Text>

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Http-Code | Bde-Text                  |
      | "smcbCardImage.xml" | 1         | "egkCardImage.xml" | 2        | 400       | "VSDSERVICE_INVALID_KVNR" |
