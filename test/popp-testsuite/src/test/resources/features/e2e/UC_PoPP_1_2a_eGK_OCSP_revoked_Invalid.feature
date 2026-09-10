#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP_Service
@TYPE:E2E
Funktionalität: PoPP-Token erzeugen mit eGK bei physischer Anwesenheit

  @TCID:UC_PoPP_1_2a_eGK_OCSP_revoked_Invalid
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenariogrundriss: PoPP-Token erzeugen bei physischer Anwesenheit mit widerrufener eGK

  Dieser Testfall testet die Business Anwendungsfälle
  UC_PoPP_1a PoPP-Token erzeugen bei physischer Anwesenheit in der LEI mit eGK und
  UC_PoPP_2a PoPP-Token erzeugen bei physischer Anwesenheit außerhalb der LEI mit widerrufener eGK.
  Ein Versicherter möchte eine Versorgung in einer LEI in Anspruch nehmen. Die LEI benötigt für den Zugriff auf die
  Daten des physisch anwesenden Versicherten einen Nachweis des Versorgungskontexts. Dazu wird die eGK des Versicherten
  an einem geeigneten Lesegerät präsentiert. Der Versicherte kann den Check-in-Prozess mit der widerrufenen eGK
  nicht durchgeführen und die LEI erhält im PS den notwendigen Nachweis des Versorgungskontexts nicht.

  Die widerrufene eGK des physisch anwesenden Versicherten wird in ein Kartenterminal eingesteckt.
  Das Primärsystem (PS) authentifiziert sich mit seiner SMC-B beim ZETA-Guard des PoPP-Service und
  erhält von diesem einen gültigen Access-Token. Das PS fragt daraufhin den PoPP-Token vom PoPP-Service ab. Der Testfall
  prüft die Fehlermeldung vom PoPP-Service und dass kein PoPP Token ausgestellt wird.

    Angenommen der Versicherte in der LEI präsentiert seine eGK <readerType> am Lesegerät <commType>
    Wenn Das Primärsystem den PoPP-Token mit Image <eGK> vom PoPP-Service abgefragt
    Dann erhält das Primärsystem den Status ERROR vom PoPP-Service mit Message <errorMessage>


    Beispiele:

      | readerType | commType          | errorMessage                                                                  | eGK                                |
      | "virtuell" | "kontaktbehaftet" | "Unexpected error: Server error ErrorEgkHandling: Invalid CA CVC certificate" | "EGK_80276883110000180787_REVOKED" |
      | "virtuell" | "kontaktlos"      | "Unexpected error: Server error ErrorEgkHandling: Invalid CA CVC certificate" | "EGK_80276883110000180787_REVOKED" |


  @TCID:UC_PoPP_1_2a_eGK_OCSP_revoked_Invalid_eHealth
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenariogrundriss: PoPP-Token erzeugen bei physischer Anwesenheit mit widerrufener eGK mit eHealth-Komponenten

  Dieser Testfall testet die Business Anwendungsfälle
  UC_PoPP_1a PoPP-Token erzeugen bei physischer Anwesenheit in der LEI mit eGK und
  UC_PoPP_2a PoPP-Token erzeugen bei physischer Anwesenheit außerhalb der LEI mit widerrufener eGK.
  Ein Versicherter möchte eine Versorgung in einer LEI in Anspruch nehmen. Die LEI benötigt für den Zugriff auf die
  Daten des physisch anwesenden Versicherten einen Nachweis des Versorgungskontexts. Dazu wird die eGK des Versicherten
  an einem geeigneten Lesegerät präsentiert. Der Versicherte kann den Check-in-Prozess mit der widerrufenen eGK
  nicht durchgeführen und die LEI erhält im PS den notwendigen Nachweis des Versorgungskontexts nicht.

  Die widerrufene eGK des physisch anwesenden Versicherten wird in ein Kartenterminal eingesteckt.
  Das Primärsystem (PS) authentifiziert sich mit seiner SMC-B beim ZETA-Guard des PoPP-Service und
  erhält von diesem einen gültigen Access-Token. Das PS fragt daraufhin den PoPP-Token vom PoPP-Service ab. Der Testfall
  prüft die Fehlermeldung vom PoPP-Service und dass kein PoPP Token ausgestellt wird.

    Angenommen der Versicherte in der LEI präsentiert seine eGK <readerType> am Lesegerät <commType>
    Wenn das Primärsystem den PoPP-Token vom PoPP-Service abfragt
    Dann erhält das Primärsystem den Status ERROR vom PoPP-Service mit Message <errorMessage>


    Beispiele:
      | readerType | commType          | errorMessage                                       |
      | "eH-KT"    | "kontaktbehaftet" | "Unexpected error: Server error ErrorEgkBlocked: " |
