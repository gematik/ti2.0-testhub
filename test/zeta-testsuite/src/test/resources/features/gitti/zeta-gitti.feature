#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@gitti'
#
# VORAUSSETZUNG: TestHub im Profil "full":
#   docker compose -f infra/docker/compose-local.yaml --profile full up -d
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
  @local
  Szenariogrundriss: Erstregistrierung eines Primärsystems an den ZETA-Guards von PoPP und VSDM 2.0

    # Der VSDM-Client führt DCR + Token-Exchange nur bei seiner ALLERERSTEN VSD-Anfrage pro
    # Container-Lebenszeit durch; danach verwendet er den intern zwischengespeicherten Token.
    # Damit dieses Szenario zuverlässig eine ECHTE Erstregistrierung beobachten kann (unabhängig
    # davon, wie oft der VSDM-Client bereits in vorherigen Testläufen registriert wurde), wird die
    # PDP-Registrierung des VSDM-Clients vor dem Trigger vollständig zurückgesetzt (siehe
    # PolicyRejectionSteps). Der popp-client führt seine DCR dagegen bei jedem Aufruf über
    # ZetaJwtTestFactory frisch durch und benötigt daher keinen Reset.
    Gegeben sei die ZeTA-PDP-Registrierung des VSDM-Clients ist vollständig zurückgesetzt
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B "<Smcb-Card>" im Slot <Smcb-Slot>
    Wenn das Primärsystem der LEI sich erstmalig am ZETA-Guard des PoPP-Service registriert
    Dann erhält das Primärystem einen gültigen Access- und Refresh-Token vom ZETA-Guard des PoPP-Service
    Wenn das Primärsystem der LEI sich erstmalig am ZETA-Guard des VSDM 2.0 Fachdienst registriert
    Dann erhält das Primärystem einen gültigen Access- und Refresh-Token vom ZETA-Guard des VSDM 2.0 Fachdienst

    Beispiele:
      | Smcb-Card                                                            | Smcb-Slot |
      | test/vsdm-testsuite/src/test/resources/private/smcb/smcbCardImage.xml | 1         |
