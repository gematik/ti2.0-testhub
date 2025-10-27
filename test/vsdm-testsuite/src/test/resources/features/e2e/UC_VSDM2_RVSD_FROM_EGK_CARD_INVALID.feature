#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM2
@TYPE:E2E
Funktionalität: Abfrage der Versichertenstammdaten von der eGK

  @TCID:UC_VSDM2_RVSD_FROM_EGK_CARD_INVALID
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Abfrage der VSD von einer ungültigen eGK

  Dieser Testfall beschreibt einen Ausnahmefall zur Abfrage der VSD von der eGK des Versicherten, wenn der Fachdienst
  VSDM 2.0 mit einem Fehlercode antwortet. Der Fehlerfall wird hier mit einem ungültigen PoPP-Token ausgelöst,
  es könnte jedoch auch jeder andere Fehlerfall für das Szenario verwendet werden. Nachdem der VSDM Ressource Server
  mit dem HTTP Return Code 500 antwortet, versucht das Primärsystem die VSD von der eGK des Versicherten zu lesen. Da
  die eGK jedoch ungültig ist, kann der Versicherte nicht in der Leistungserbringerinstitution versorgt werden.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem sich mit seiner SMC-B beim ZETA Guard des VSDM 2.0 Fachdienstes authentifiziert
    Dann erhält das Primärsystem einen Access- und Refresh-Token vom ZETA Guard
    Wenn das Primärsystem die VSD mit einem ungültigen PoPP-Token vom VSDM Ressource Server abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <Bde-Text>
    Wenn das Primärsystem die VSD direkt von einer ungültigen eGK des Versicherten in der LEI abfragt
    Dann werden die VSD nicht von der eGK gelesen und der Versicherte kann nicht versorgt werden

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card                  | Egk-Slot | Http-Code | Bde-Text                           |
      | "smcbCardImage.xml" | 1         | "egkCardImageInvalid.xml" | 2        | 500       | "VSDSERVICE_INTERNAL_SERVER_ERROR" |
