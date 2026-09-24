#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AF-ID:AF_10413
@TYPE:E2E
Funktionalität: Abfrage der Versichertenstammdaten von der eGK

  @TCID:UC_VSDM2_RVSD_FROM_EGK_CARD_VALID
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Abfrage der VSD von einer gültigen eGK

  Dieser Testfall beschreibt einen Ausnahmefall zur Abfrage der VSD von der eGK des Versicherten, wenn der Fachdienst
  VSDM 2.0 mit einem Fehlercode antwortet. Der Fehlerfall wird hier mit einer ungültigen Profile-Version ausgelöst,
  es könnte jedoch auch jeder andere Fehlerfall für das Szenario verwendet werden. Nachdem der VSDM Ressource Server
  mit einem HTTP Fehler Code antwortet, liest das Primärsystem die VSD von der eGK des Versicherten und dieser kann
  nun in der Leistungserbringerinstitution versorgt werden.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einer unbekannten FHIR Profile Version <Version> abfragt
    Dann antwortet der VSDM Fachdienst mit einem Fehlercode größer oder gleich 400
    Wenn das Primärsystem die VSD direkt von einer gültigen eGK des Versicherten in der LEI abfragt
    Dann werden die VSD von der eGK gelesen und der Versicherte kann versorgt werden

    Beispiele:
      | Smcb-Slot | Egk-Slot | Version |
      | 1         | 2        | "1.2.3" |
