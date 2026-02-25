#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP
@TYPE:E2E
Funktionalität: Fehlerfälle beim TLS-Aufbau der Verbindung

  Dieses Feature testet die Fehlerfälle beim Aufbau einer TLS-Verbindung mit dem PoPP-Service.
  Die Verbindung erfordert ein gültiges C.FD.TLS-C-Zertifikat der TI-Komponenten PKI mit der Rolle `oid_tsp_egk`,
  das zeitlich gültig und nicht gesperrt ist.

  @TCID:UC_PoPP_TLS_Fehler_ZertifikatGesperrt
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:1

  Szenario: Abweisung der Verbindung bei gesperrtem Zertifikat

    Angenommen die Schnittstelle I_PoPP_EHC_CertHash_Import ist beim PoPP-Service verfügbar
    Und der TSP stellt eine TLS-Verbindung mit einem gesperrten Client-Zertifikat zum PoPP-Service her
    Wenn der TLS-Handshake durchgeführt wird
    Dann wird die Verbindung vom PoPP-Service abgelehnt
    Und der TSP erhält eine Fehlermeldung "401 Unauthorized: Zertifikat gesperrt"

  @TCID:UC_PoPP_TLS_Fehler_ZertifikatAbgelaufen
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:1

  Szenario: Abweisung der Verbindung bei abgelaufenen Zertifikat

    Angenommen die Schnittstelle I_PoPP_EHC_CertHash_Import ist beim PoPP-Service verfügbar
    Und der TSP stellt eine TLS-Verbindung mit einem abgelaufenen Client-Zertifikat zum PoPP-Service her
    Wenn der TLS-Handshake durchgeführt wird
    Dann wird die Verbindung vom PoPP-Service abgelehnt
    Und der TSP erhält eine Fehlermeldung "401 Unauthorized: Zertifikat abgelaufen"

  @TCID:UC_PoPP_TLS_Fehler_ZertifikatFalscheRolle
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:1

  Szenario: Abweisung der Verbindung bei Zertifikat mit falscher Rolle

    Angenommen die Schnittstelle I_PoPP_EHC_CertHash_Import ist beim PoPP-Service verfügbar
    Und der TSP stellt eine TLS-Verbindung mit einem Client-Zertifikat her, das nicht die Rolle `oid_tsp_egk` besitzt
    Wenn der TLS-Handshake durchgeführt wird
    Dann wird die Verbindung vom PoPP-Service abgelehnt
    Und der TSP erhält eine Fehlermeldung "401 Unauthorized: Zertifikat besitzt falsche Rolle"