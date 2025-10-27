#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM2
@TYPE:BDE
Funktionalität: Betriebsdatenerfassung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_BDE_CODE_79100
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: BDE Fehlercode 79100 - Server Fehler

  Dieser Testfall beschreibt ein Fehlerszenario, das durch einen ungültigen PoPP-Token verursacht wird. Ursache hierfür
  könnte ein Fehler bei der Generierung des PoPP-Tokens im PoPP-Service sein. Der VSDM Ressource Server kann den
  fehlerhaften PoPP-Token nicht verarbeiten und antwortet mit dem HTTP Return Code 500.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem sich mit seiner SMC-B beim ZETA Guard des VSDM 2.0 Fachdienstes authentifiziert
    Dann erhält das Primärsystem einen Access- und Refresh-Token vom ZETA Guard
    Wenn das Primärsystem die VSD mit einem ungültigen PoPP-Token vom VSDM Ressource Server abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <Bde-Text>

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Http-Code | Bde-Text                           |
      | "smcbCardImage.xml" | 1         | "egkCardImage.xml" | 2        | 500       | "VSDSERVICE_INTERNAL_SERVER_ERROR" |
