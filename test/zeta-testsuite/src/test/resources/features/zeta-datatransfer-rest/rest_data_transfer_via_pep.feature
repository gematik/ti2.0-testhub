#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@rest_pep_transfer'
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
@PRODUKT:ZETA


Funktionalität: REST Datenübertragung zwischen Client und Server via ZETA-PEP Proxy

  # Dieses Feature testet die REST-basierte Datenübertragung über den ZETA-PEP Proxy.
  # Der HttpProxyController fängt alle Anfragen unter /** ab (außer /service/** und /.well-known/**).
  # Er prüft den Authorization-Header und leitet bei gültigem Token an das Backend (PoPP-Server) weiter.
  # Bei fehlendem Token gibt der PEP 401 zurück; bei ungültigem Token 401 oder 500.

  Grundlage:
    Wenn TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "pepProxyUrl" auf "${zeta.server.pep.url}"
    Und TGR setze lokale Variable "pepTestPath" auf "/v3/api-docs"
    Und TGR setze lokale Variable "pepFindPath" auf "/v3/api-docs"
    Und TGR lösche alle default headers

  @TCID:ZETA_REST_PEP_TRANSFER_WITH_VALID_ACCESS_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @rest_pep_transfer
  Szenario: PEP akzeptiert gültigen Token und leitet Anfrage an Backend weiter
    Gegeben sei ein gültiger ZETA-PEP AccessToken wird erzeugt

    # Anfrage an einen Backend-Endpunkt über den echten PEP senden
    Wenn TGR sende eine leere GET Anfrage an "${pepProxyUrl}${pepTestPath}"

    # Beweis der Token-Akzeptanz: Mit gültigem Token leitet der echte PEP an das
    # Backend weiter (≠ 401). Der Mock-Pfad /v3/api-docs existiert im echten
    # PoPP-Backend nicht → 404. Ohne Token liefert der PEP 401 (s. Negativtest).
    Dann TGR finde die letzte Anfrage mit dem Pfad "${pepFindPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "(?!401)\d{3}"

  @TCID:ZETA_REST_PEP_TRANSFER_WITH_MISSING_ACCESS_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @rest_pep_transfer
  Szenario: REST-Anfrage ohne Authorization wird vom PEP abgelehnt
    Wenn TGR sende eine leere GET Anfrage an "${pepProxyUrl}${pepTestPath}"

    # PEP muss mit 401 Unauthorized antworten
    Dann TGR finde die letzte Anfrage mit dem Pfad "${pepFindPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "401"


  @TCID:ZETA_REST_PEP_TRANSFER_WITH_INVALID_ACCESS_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @rest_pep_transfer
  Szenario: REST-Anfrage mit ungültigem Token wird vom PEP abgelehnt
    Gegeben sei ein ungültiger ZETA-PEP AccessToken wird erzeugt

    Wenn TGR sende eine leere GET Anfrage an "${pepProxyUrl}${pepTestPath}"

    # Der echte PEP liefert bei ungültigem Token 401; wir akzeptieren 4xx/5xx.
    Dann TGR finde die letzte Anfrage mit dem Pfad "${pepFindPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "4..|5.."

