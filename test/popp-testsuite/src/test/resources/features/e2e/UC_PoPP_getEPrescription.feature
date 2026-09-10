#language: de
@PRODUKT:KT,Konnektor\sVSDM,Konnektor\seHealth,CMS,UFS,VSDD,Intermediär\sVSDM
Funktionalität: E2E Test Anwendung VSDM Online

  @MODUS:Automatisch
  @TESTSTUFE:3 @PRIO:1 @TESTFALL:Positiv
  @STATUS:InBearbeitung
  @DESCRIPTION
  @TCID:UC_PoPP_E-Rezept
  Szenariogrundriss: Einlösen E-Rezept Kasse mit PoPP-Token

    Angenommen der Versicherte in der LEI präsentiert seine eGK <readerType> am Lesegerät <commType>
    Und stelle ein E-Rezept für folgende eGK ein: <eGK>
    Wenn Das Primärsystem den PoPP-Token mit Image <eGK> vom PoPP-Service abgefragt
    Und löse das E-Rezept mit PoPP-Token ein


    Beispiele:
      | readerType      | commType          | eGK                              |
      | "virtuell"      | "kontaktbehaftet" | "EGK_80276001042001660952_TK" |
