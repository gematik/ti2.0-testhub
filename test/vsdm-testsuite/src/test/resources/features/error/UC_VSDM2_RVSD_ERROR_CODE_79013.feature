#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_27012-06
@TYPE:ERROR
Funktionalität: Fehlerbehandlung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_CODE_79013
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Fehlercode 79013 - Unbekannte KVNR

  Dieser Testfall beschreibt ein Fehlerszenario, das durch eine unbekannte KV-Nummer verursacht wird, welche im
  PoPP-Token enthalten ist und vom VSDM Ressource Server nicht verarbeitet werden kann. Ursache für einen solchen
  Fehlerfall könnte z.B. ein PoPP-Token mit unbekannter KVNR zum Fachdienst VSDM 2.0 sein.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einer unbekannten KVNR <KVNR> vom VSDM Ressource Server abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <KVNR> <Error-Code>

    Beispiele:
      | Smcb-Slot | Egk-Slot | KVNR         | Http-Code | Error-Code                |
      | 1         | 2        | "X912345675" | 404       | "VSDSERVICE_UNKNOWN_KVNR" |
