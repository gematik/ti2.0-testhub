#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AF-ID:AF_10412
@TYPE:GITTI
Funktionalität: Abfrage der Versichertenstammdaten vom Fachdienst VSDM 2.0

  @TCID:UC_GITTI_RVSD_FROM_SERVER_WITH_UPDATE
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
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
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B im Slot <smcbSlot>
    Angenommen der Versicherte in der LEI verwendet eine eGK im Slot <egkSlot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <readerType> am Lesegerät <commType>
    Wenn das Primärsystem den PoPP-Token mit der eGK vom PoPP-Service abgefragt
    Dann empfängt das Primärsystem den vollständigen und spezifikationskonformen PoPP-Token
    Und die Daten des Versicherten im PoPP-Token entsprechen denen auf der eGK
    Und die Daten des Leistungserbringers im PoPP-Token entsprechen denen auf der SMC-B
    Wenn das Primärsystem die VSD mit dem validen PoPP-Token vom VSDM Ressource Server abfragt
    Dann sendet der VSDM Ressource Server die aktualisierten VSD mit dem Statuscode <httpCode> zum Primärsystem
    Und dann sendet der VSDM Ressource Server ein neues E-Tag zum Primärsystem
    Und die aktualisierten VSD enthalten das VsdmBundle mit den korrekten Patientendaten
    Und die aktualisierten VSD enthalten das VsdmBundle mit den korrekten Versicherungsdaten

    Beispiele:
      | smcbSlot | egkSlot | readerType | commType          | httpCode |
      | 1        | 2       | "virtuell" | "kontaktbehaftet" | 200      |
