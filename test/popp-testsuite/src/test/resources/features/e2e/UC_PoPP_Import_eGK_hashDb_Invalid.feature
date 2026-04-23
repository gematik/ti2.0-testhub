#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP
@TYPE:E2E
@HashDbInvalid
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

    Angenommen TGR lösche aufgezeichnete Nachrichten
    Und der TSP verwendet die Client Identität "tspEgkTlsRevoked" für die mTLS-Verbindung zum PoPP-Service
    Wenn der TSP sendet den signierten eContent "80276883110000144098.eContent-signed" zum importieren an den PoPP Service
    Dann wird die Verbindung vom PoPP-Service abgelehnt

  @TCID:UC_PoPP_TLS_Fehler_ZertifikatAbgelaufen
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:1

  Szenario: Abweisung der Verbindung bei abgelaufenen Zertifikat

    Angenommen TGR lösche aufgezeichnete Nachrichten
    Und der TSP verwendet die Client Identität "tspEgkTlsExpired" für die mTLS-Verbindung zum PoPP-Service
    Wenn der TSP sendet den signierten eContent "80276883110000144098.eContent-signed" zum importieren an den PoPP Service
    Dann wird die Verbindung vom PoPP-Service abgelehnt

  @TCID:UC_PoPP_TLS_Fehler_ZertifikatFalscheRolle
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:1

  Szenario: Abweisung der Verbindung bei Zertifikat mit falscher Rolle

    Angenommen TGR lösche aufgezeichnete Nachrichten
    Und der TSP verwendet die Client Identität "tspEgkTlsInvalidCertType" für die mTLS-Verbindung zum PoPP-Service
    Wenn der TSP sendet den signierten eContent "80276883110000144098.eContent-signed" zum importieren an den PoPP Service
    Dann wird die Verbindung vom PoPP-Service abgelehnt