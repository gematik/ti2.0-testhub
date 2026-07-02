#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_27012-06
@TYPE:ERROR
Funktionalität: Fehlerbehandlung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_CODE_79014
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Fehlercode 79014 - Fehlendes E-Tag

  Dieser Testfall beschreibt ein Fehlerszenario, das durch ein fehlendes Entity-Tag verursacht wird. Obwohl ein gültiger
  Access- und PoPP-Token für die Abfrage der VSD vom VSDM Ressource Server vorhanden sind, kann dieser die Anfrage nicht
  beantworten, weil das Primärsystem kein Entity-Tag in den HTTP Header-Daten gesendet hat.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einem fehlenden E-Tag vom VSDM Ressource Server abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <Error-Code>

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card                                  | Egk-Slot | Http-Code | Error-Code                                  |
      | "smcbCardImage.xml" | 1         | "${EGK_CARD_IMAGE_FILE:egkCardData.json}" | 2        | 428       | "VSDSERVICE_MISSING_PATIENT_RECORD_VERSION" |
