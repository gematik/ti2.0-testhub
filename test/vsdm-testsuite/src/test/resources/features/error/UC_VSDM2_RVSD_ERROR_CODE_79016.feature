#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_27012-06
@TYPE:ERROR
Funktionalität: Fehlerbehandlung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_CODE_79016
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Fehlercode 79016 - Fehlende FHIR Profile Version

  Dieser Testfall beschreibt ein Fehlerszenario, das durch den fehlenden FHIR-Profilversion-Parameter bei der
  Abfrage der VSD verursacht wird. Aktuell unterstützt der Fachdienst VSDM 2.0 nur die Version 1.0.0.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einer fehlenden FHIR Profile Version abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <Error-Code>

    Beispiele:
      | Smcb-Slot | Egk-Slot | Http-Code | Error-Code                           |
      | 1         | 2        | 400       | "VSDSERVICE_MISSING_PROFILE_VERSION" |
