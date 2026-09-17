#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_27012-06
@TYPE:ERROR
Funktionalität: Fehlerbehandlung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_CODE_WITH_FHIR_XML
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: VsdmOperationOutcome als FHIR-XML

  Dieser Testfall beschreibt ein Fehlerszenario, das durch den fehlenden FHIR-Profilversion-Parameter bei der
  Abfrage der VSD verursacht wird. Der Fehlercode wird mit dem Content-Type "application/fhir+xml" gesendet.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einer fehlenden FHIR Profile Version und dem Accept-Header <Accept-Header> abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Content-Type <Content-Type>

    Beispiele:
      | Smcb-Slot | Egk-Slot | Http-Code | Accept-Header          | Content-Type           |
      | 1         | 2        | 400       | "application/fhir+xml" | "application/fhir+xml" |
