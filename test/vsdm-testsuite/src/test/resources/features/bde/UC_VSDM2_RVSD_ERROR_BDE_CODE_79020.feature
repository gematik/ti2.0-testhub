#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM2
@TYPE:BDE
Funktionalität: Betriebsdatenerfassung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_BDE_CODE_79020
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: BDE Fehlercode 79020 - VSD Fehler

  Dieser Testfall beschreibt ein Fehlerszenario, das durch einen Fehler in der DB verursacht wird. Obwohl die IK- und
  die KVNR-Nummern im PoPP-Token korrekt sind, kann der VSDM Ressource Server den passenden Datensatz nicht finden und
  antwortet dann mit dem HTTP Return Code 404 - NOT FOUND.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem sich mit seiner SMC-B beim ZETA Guard des VSDM 2.0 Fachdienstes authentifiziert
    Dann erhält das Primärsystem einen Access- und Refresh-Token vom ZETA Guard
    Wenn das Primärsystem die VSD vom VSDM Ressource Server abfragt und dieser die VSD nicht finden kann
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <Bde-Text>

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Http-Code | Bde-Text                              |
      | "smcbCardImage.xml" | 1         | "egkCardImage.xml" | 2        | 404       | "VSDSERVICE_PATIENT_RECORD_NOT_FOUND" |
