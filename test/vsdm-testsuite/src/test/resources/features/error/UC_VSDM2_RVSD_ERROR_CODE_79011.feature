#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_27012-06
@TYPE:ERROR
Funktionalität: Fehlerbehandlung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_CODE_79011
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Fehlercode 79011 - Ungültige KVNR

  Dieser Testfall beschreibt ein Fehlerszenario, das durch eine ungültigen KV-Nummer verursacht wird, welche im
  PoPP-Token enthalten ist und vom VSDM Ressource Server nicht verarbeitet werden kann. Ursache für einen solchen
  Fehlerfall könnte z.B. eine abgelaufene Versicherung oder ein Versichertenwechsel sein.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einer ungültigen KVNR <KVNR> vom VSDM Ressource Server abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <KVNR> <Error-Code>

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card                                  | Egk-Slot | KVNR         | Http-Code | Error-Code                |
      | "smcbCardImage.xml" | 1         | "${EGK_CARD_IMAGE_FILE:egkCardData.json}" | 2        | "WRONG_KVNR" | 400       | "VSDSERVICE_INVALID_KVNR" |
