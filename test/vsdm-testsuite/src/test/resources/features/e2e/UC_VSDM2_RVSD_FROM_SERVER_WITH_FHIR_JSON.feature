#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AF-ID:AF_10412
@TYPE:E2E
Funktionalität: Abfrage der Versichertenstammdaten vom Fachdienst VSDM 2.0

  @TCID:UC_VSDM2_RVSD_FROM_SERVER_WITH_FHIR_JSON
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION
  Szenariogrundriss: Abfrage der VSD mit FHIR-JSON

  Dieser Testfall beschreibt den ersten Standard-Anwendungsfall zur Abfrage der VSD vom Fachdienst VSDM 2.0.
  Die eGK des Versicherten wird in ein Kartenterminal der Leistungserbringerinstitution (LEI) eingesteckt.
  Das Primärsystem (PS) authentifiziert sich mit seiner SMC-B beim ZETA-Guard des Fachdienstes VSDM 2.0 und
  erhält von diesem einen gültigen Access-Token. Zusammen mit einem gültigen PoPP-Token, der den Versorgungskontext
  zwischen dem Versicherten und der LEI bescheinigt, können nun die VSD vom VSDM Ressource Server abgefragt werden.
  Zuvor vergleicht der VSDM Ressource Server das Entity-Tag des PS mit seinem eigenen und stellt einen Unterschied
  fest. Das unterschiedliche Entity-Tag veranlasst den VSDM Ressource Server, die VSD als FHIR-Datensatz mit dem
  Content-Type "application/fhir+json" an das PS zu senden.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit dem Accept-Header <Accept-Header> vom VSDM Ressource Server abfragt
    Dann sendet der VSDM Ressource Server die aktualisierten VSD mit dem Content-Type <Content-Type> zum Primärsystem

    Beispiele:
      | Smcb-Slot | Egk-Slot | Accept-Header           | Content-Type            |
      | 1         | 2        | "application/fhir+json" | "application/fhir+json" |
