#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_26477
@TYPE:ERROR
Funktionalität: Fehlerbehandlung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_FORBIDDEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Ungültiger PoPP Token

  Dieser Testfall beschreibt ein Fehlerszenario, das durch einen ungültigen PoPP-Token verursacht wird. Ursache hierfür
  könnte ein Fehler bei der Generierung des PoPP-Tokens im PoPP-Service sein. Der ZETA Guard kann den fehlerhaften
  PoPP-Token nicht verarbeiten und antwortet mit dem HTTP Return Code 403.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einem ungültigen PoPP-Token vom VSDM Ressource Server abfragt
    Dann antwortet der ZETA Guard mit dem Fehlercode <Http-Code> und dem Text <Error-Text>

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Http-Code | Error-Text                 |
      | "smcbCardImage.xml" | 1         | "egkCardImage.xml" | 2        | 403       | "PoPP error: InvalidToken" |
