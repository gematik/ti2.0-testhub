#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_26477-01
@TYPE:POLICY
Funktionalität: VSDM 2.0 Policy

  @TCID:UC_VSDM2_RVSD_POLICY_DENIED
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Ungültiger Profession OID

  Dieser Testfall beschreibt ein Fehlerszenario, das durch eine ungültige Profession OID verursacht wird. Der ZETA Guard
  im Fachdienst VSDM 2.0 prüft anhand des SMC-B-Zertifikats u.a. die Profession OID, befindet sich diese nicht in der
  Allow-Liste, wird eine Anfrage an den Fachdienst mit entsprechender Fehlermeldung abgelehnt.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einer ungültigen Profession OID vom VSDM Ressource Server abfragt
    Dann antwortet der ZETA Guard mit dem Fehlercode <Http-Code> und dem Text <Error-Text>

    Beispiele:
      | Smcb-Slot | Egk-Slot | Http-Code | Error-Text      |
      | 1         | 2        | 403       | "policy_denied" |
