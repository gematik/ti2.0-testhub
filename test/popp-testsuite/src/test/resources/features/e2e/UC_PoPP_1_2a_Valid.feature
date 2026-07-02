#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP_Service
@TYPE:E2E
Funktionalität: PoPP-Token erzeugen mit eGK bei physischer Anwesenheit

  Hintergrund:
    Und TGR lösche aufgezeichnete Nachrichten


  @TCID:UC_PoPP_1_2a_Valid
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION
  Szenariogrundriss: PoPP-Token erzeugen bei physischer Anwesenheit mit eGK

  Dieser Testfall testet die Business Anwendungsfälle
  UC_PoPP_1a PoPP-Token erzeugen bei physischer Anwesenheit in der LEI mit eGK und
  UC_PoPP_2a PoPP-Token erzeugen bei physischer Anwesenheit außerhalb der LEI mit eGK. Beide Business Anwendungsfälle
  werden durch die gleichen technischen Anwendungsfälle realisiert.
  Ein Versicherter möchte eine Versorgung in einer LEI in Anspruch nehmen. Die LEI benötigt für den Zugriff auf die
  Daten des physisch anwesenden Versicherten einen Nachweis des Versorgungskontexts. Dazu wird die eGK des Versicherten
  an einem geeigneten Lesegerät präsentiert. Nachdem der Versicherte den Check-in-Prozess mit der eGK
  durchgeführt hat, ist dieser abgeschlossen und die LEI erhält im PS den notwendigen Nachweis des Versorgungskontexts.

  Die eGK des physisch anwesenden Versicherten wird in ein Kartenterminal eingesteckt
  (AF_10393 in ein eH-KT oder AF_10387 in ein Standard-Kartenleser).
  Das Primärsystem (PS) authentifiziert sich mit seiner SMC-B beim ZETA-Guard des PoPP-Service und
  erhält von diesem einen gültigen Access-Token (AF_10402). Das PS fragt daraufhin den PoPP-Token vom PoPP-Service ab. Der Testfall
  prüft die APDU Szenarien für die Kommunikation mit der eGK sowie die Vollständigkeit und Spezifikationskonformität
  des erhaltenen PoPP-Tokens.

    Angenommen der Versicherte in der LEI präsentiert seine eGK <readerType> am Lesegerät <commType>
    Wenn Das Primärsystem den PoPP-Token mit Image <eGK> vom PoPP-Service abgefragt
    Dann das PoPP-Token ist vollständig und spezifikationskonform
    Und Die Daten des Versicherten im PoPP-Token entsprechen denen auf der Karte <eGK>
    Und Die Daten der Leisungserbringerorganisation im PoPP-Token entsprichen denen auf der Karte <SMCB>
    Und die empfangenen APDUs sind korrekt <readerType>

    Beispiele:
      | readerType | commType          | eGK                              | SMCB                                       |
    #  | "Standardleser" | "kontaktbehaftet" ||SMCB_80276001011699902303_APOTHEKE_VALID|
    #  | "Standardleser" | "kontaktlos"      ||SMCB_80276001011699902303_APOTHEKE_VALID|
      | "virtuell" | "kontaktbehaftet" | "EGK_80276883110000152715_VALID" | "SMCB_80276001011699902303_APOTHEKE_VALID" |
      | "virtuell" | "kontaktlos"      | "EGK_80276883110000152715_VALID" | "SMCB_80276001011699902303_APOTHEKE_VALID" |


  @TCID:UC_PoPP_1_2a_Valid_ehealth
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION
  Szenariogrundriss: PoPP-Token erzeugen bei physischer Anwesenheit mit eGK und ehealth-Komponenten

  Dieser Testfall testet die Business Anwendungsfälle
  UC_PoPP_1a PoPP-Token erzeugen bei physischer Anwesenheit in der LEI mit eGK und
  UC_PoPP_2a PoPP-Token erzeugen bei physischer Anwesenheit außerhalb der LEI mit eGK. Beide Business Anwendungsfälle
  werden durch die gleichen technischen Anwendungsfälle realisiert.
  Ein Versicherter möchte eine Versorgung in einer LEI in Anspruch nehmen. Die LEI benötigt für den Zugriff auf die
  Daten des physisch anwesenden Versicherten einen Nachweis des Versorgungskontexts. Dazu wird die eGK des Versicherten
  an einem geeigneten Lesegerät präsentiert. Nachdem der Versicherte den Check-in-Prozess mit der eGK
  durchgeführt hat, ist dieser abgeschlossen und die LEI erhält im PS den notwendigen Nachweis des Versorgungskontexts.

  Die eGK des physisch anwesenden Versicherten wird in ein Kartenterminal eingesteckt
  (AF_10393 in ein eH-KT oder AF_10387 in ein Standard-Kartenleser).
  Das Primärsystem (PS) authentifiziert sich mit seiner SMC-B beim ZETA-Guard des PoPP-Service und
  erhält von diesem einen gültigen Access-Token (AF_10402). Das PS fragt daraufhin den PoPP-Token vom PoPP-Service ab. Der Testfall
  prüft die APDU Szenarien für die Kommunikation mit der eGK sowie die Vollständigkeit und Spezifikationskonformität
  des erhaltenen PoPP-Tokens.

    Angenommen der Versicherte in der LEI präsentiert seine eGK <readerType> am Lesegerät <commType>
    Wenn das Primärsystem den PoPP-Token vom PoPP-Service abfragt
    Dann das PoPP-Token ist vollständig und spezifikationskonform
    #Und die empfangenen APDUs sind korrekt <readerType>

    Beispiele:
      | readerType | commType          |
      | "eH-KT"    | "kontaktbehaftet" |
