#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@gitti'
#
# VORAUSSETZUNG: TestHub im Profil "full":
#   docker compose -f doc/docker/compose-local.yaml --profile full up -d
@PRODUKT:ZETA
@TYPE:GITTI
Funktionalität: ZETA-GITTI

  Grundlage:
    Wenn TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers

  @TCID:ZETA_GITTI_ERSTREGISTRIERUNG
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @gitti
  Szenariogrundriss: Erstregistrierung eines Primärsystems an den ZETA-Guards von PoPP und VSDM 2.0

    Angenommen das Primärsystem in der LEI verwendet eine SMC-B "<Smcb-Card>" im Slot <Smcb-Slot>
    Wenn das Primärsystem der LEI sich erstmalig am ZETA-Guard des PoPP-Service registriert
    Dann erhält das Primärystem einen gültigen Access- und Refresh-Token vom ZETA-Guard des PoPP-Service
    Wenn das Primärsystem der LEI sich erstmalig am ZETA-Guard des VSDM 2.0 Fachdienst registriert
    Dann erhält das Primärystem einen gültigen Access- und Refresh-Token vom ZETA-Guard des VSDM 2.0 Fachdienst

    Beispiele:
      | Smcb-Card                                                            | Smcb-Slot |
      | test/vsdm-testsuite/src/test/resources/data/cards/smcbCardImage.xml | 1         |
