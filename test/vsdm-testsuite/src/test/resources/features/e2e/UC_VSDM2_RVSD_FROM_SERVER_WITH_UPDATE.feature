#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM2
@TYPE:E2E
Funktionalität: Abfrage der Versichertenstammdaten vom Fachdienst VSDM 2.0

  @TCID:UC_VSDM2_RVSD_FROM_SERVER_WITH_UPDATE
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Abfrage der VSD mit VSD Update

    Dieser Testfall beschreibt den ersten Standard-Anwendungsfall zur Abfrage der VSD vom Fachdienst VSDM 2.0.
    Die eGK des Versicherten wird in ein Kartenterminal der Leistungserbringerinstitution (LEI) eingesteckt.
    Das Primärsystem (PS) authentifiziert sich mit seiner SMC-B beim ZETA-Guard des Fachdienstes VSDM 2.0 und
    erhält von diesem einen gültigen Access-Token. Zusammen mit einem gültigen PoPP-Token, der den Versorgungskontext
    zwischen dem Versicherten und der LEI bescheinigt, können nun die VSD vom VSDM Ressource Server abgefragt werden.
    Zuvor vergleicht der VSDM Ressource Server das Entity-Tag des PS mit seinem eigenen und stellt einen Unterschied
    fest. Das unterschiedliche Entity-Tag veranlasst den VSDM Ressource Server, die VSD als FHIR-Datensatz mit einem
    HTTP Return Code 200 an das PS zu senden. Das PS speichert schließlich die VSD, die Prüfziffer, das Entity-Tag
    sowie den PoPP-Token in seiner lokalen Datenbank und der Versicherte kann nun durch die LEI versorgt werden.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem sich mit seiner SMC-B beim ZETA Guard des VSDM 2.0 Fachdienstes authentifiziert
    Dann erhält das Primärsystem einen Access- und Refresh-Token vom ZETA Guard
    Wenn das Primärsystem die VSD mittels PoPP- und Access-Token vom VSDM Ressource Server abfragt
    Und der VSDM Ressource Server beim E-Tag-Vergleich einen Unterschied feststellt
    Dann sendet der VSDM Ressource Server die aktualisierten VSD mit dem Statuscode <Http-Code> zum Primärsystem
    Und das Primärsystem speichert die aktualisierten VSD in seiner lokalen Datenbank
    Und das Primärsystem speichert den PoPP-Token in seiner lokalen Datenbank
    Und das Primärsystem speichert die Prüfziffer in seiner lokalen Datenbank
    Und das Primärsystem speichert das E-Tag in seiner lokalen Datenbank

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Http-Code |
      | "smcbCardImage.xml" | 1         | "egkCardImage.xml" | 2        | 200       |
