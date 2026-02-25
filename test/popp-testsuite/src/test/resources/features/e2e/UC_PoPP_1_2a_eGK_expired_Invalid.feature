#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP
@TYPE:E2E
Funktionalität: PoPP-Token erzeugen mit eGK bei physischer Anwesenheit

  @TCID:UC_PoPP_1_2a_eGK_expired_Invalid
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenariogrundriss: PoPP-Token erzeugen bei physischer Anwesenheit mit abgelaufener eGK

  Dieser Testfall testet die Business Anwendungsfälle
  UC_PoPP_1a PoPP-Token erzeugen bei physischer Anwesenheit in der LEI mit eGK und
  UC_PoPP_2a PoPP-Token erzeugen bei physischer Anwesenheit außerhalb der LEI mit abgelaufener eGK.
  Ein Versicherter möchte eine Versorgung in einer LEI in Anspruch nehmen. Die LEI benötigt für den Zugriff auf die
  Daten des physisch anwesenden Versicherten einen Nachweis des Versorgungskontexts. Dazu wird die abgelaufene eGK des Versicherten
  an einem geeigneten Lesegerät präsentiert. Der Versicherte kann den Check-in-Prozess mit der abgelaufenen eGK
  nicht durchgeführen und die LEI erhält im PS den notwendigen Nachweis des Versorgungskontexts nicht.

  Die abgelaufene eGK des physisch anwesenden Versicherten wird in ein Kartenterminal eingesteckt.
  Das Primärsystem (PS) authentifiziert sich mit seiner SMC-B beim ZETA-Guard des PoPP-Service und
  erhält von diesem einen gültigen Access-Token. Das PS fragt daraufhin den PoPP-Token vom PoPP-Service ab. Der Testfall
  prüft die Fehlermeldung vom PoPP-Service und dass kein PoPP Token ausgestell wird.

    Angenommen der Versicherte in der LEI präsentiert seine eGK <readerType> am Lesegerät <commType>
    Wenn das Primärsystem den PoPP-Token vom PoPP-Service abfragt
    #Angenommen das Primärsystem hat einen gültigen Access- und Refresh-Token vom ZETA Guard
    Dann erhält das Primärsystem den Fehlercode <errorCode> und den Text "ERROR" vom PoPP-Service
   # Und das PoPP-Token wird nicht ausgestellt
    Beispiele:
      | readerType      | commType          | errorCode |
     # | "eH-KT"         | "kontaktbehaftet" | ""        |
      | "Standardleser" | "kontaktbehaftet" | ""        |
    #  | "virtuell"      | "kontaktbehaftet" | ""        |
    #  | eH-KT           | kontaktlos        | @run      |
    #  | "Standardleser" | "kontaktlos"      | @run      |