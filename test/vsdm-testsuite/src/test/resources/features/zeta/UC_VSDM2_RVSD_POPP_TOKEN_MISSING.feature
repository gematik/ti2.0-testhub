#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_26477-01
@TYPE:ZETA
Funktionalität: Fehlerbehandlung ZETA Guard

  @TCID:UC_VSDM2_RVSD_POPP_TOKEN_MISSING
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:3
  @DESCRIPTION
  Szenariogrundriss: Fehlender PoPP Token

  Dieser Testfall beschreibt ein Fehlerszenario, das durch einen fehlenden PoPP-Token verursacht wird. Ursache hierfür
  könnte ein Fehler in der Implementierung des Client-Systems sein. Der ZETA Guard kann die Anfrage ohne PoPP-Token
  nicht verarbeiten und antwortet mit dem HTTP Return Code 400.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD ohne einen PoPP-Token vom VSDM Ressource Server abfragt
    Dann antwortet der ZETA Guard mit dem Fehlercode <Http-Code> und dem Text <Error-Text>

    Beispiele:
      | Smcb-Slot | Egk-Slot | Http-Code | Error-Text            |
      | 1         | 2        | 400       | "PoPP header missing" |
