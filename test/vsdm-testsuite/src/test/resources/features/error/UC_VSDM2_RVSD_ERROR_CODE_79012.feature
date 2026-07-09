#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_27012-06
@TYPE:ERROR
Funktionalität: Fehlerbehandlung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_CODE_79012
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Fehlercode 79012 - Unbekannte IK

  Dieser Testfall beschreibt ein Fehlerszenario, das durch eine unbekannte IK-Nummer verursacht wird, welche im
  PoPP-Token enthalten ist und vom VSDM Ressource Server nicht verarbeitet werden kann. Ursache für einen solchen
  Fehlerfall könnte z.B. ein PoPP-Token mit unbekannter IK zum Fachdienst VSDM 2.0 sein.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einer unbekannten IK <IK> vom VSDM Ressource Server abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <IK> <Error-Code>

    Beispiele:
      | Smcb-Slot | Egk-Slot | IK          | Http-Code | Error-Code              |
      | 1         | 2        | "123456780" | 400       | "VSDSERVICE_UNKNOWN_IK" |
