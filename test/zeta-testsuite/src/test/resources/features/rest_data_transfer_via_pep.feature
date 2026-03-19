#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@rest_pep_transfer'
@PRODUKT:ZETA

Funktionalität: REST Datenübertragung zwischen Client und Server via ZETA-PEP Proxy

  # Dieses Feature testet die REST-basierte Datenübertragung über den ZETA-PEP Proxy.
  # Der HttpProxyController fängt alle Anfragen unter /** ab (außer /service/** und /.well-known/**).
  # Er prüft den Authorization-Header und leitet bei gültigem Token an das Backend (PoPP-Server) weiter.
  # Bei fehlendem oder ungültigem Token gibt der PEP direkt 401 zurück.

  Grundlage:
    Wenn TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "pepProxyUrl" auf "http://127.0.0.1:9110"

  @rest_pep_transfer
  Szenario: PEP akzeptiert gültigen Token und leitet Anfrage an Backend weiter
    Gegeben sei ein gültiger ZETA-PEP AccessToken wird erzeugt

    # Anfrage an einen existierenden Backend-Endpunkt über PEP senden
    Wenn REST sende GET Anfrage an "${pepProxyUrl}/openapi.yaml" mit Authorization "${ZETA_PEP_AUTHZ}"

    # PEP akzeptiert Token, leitet an PoPP-Server weiter, Backend antwortet mit 200
    Dann TGR finde die letzte Anfrage mit dem Pfad "/openapi.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

  @rest_pep_transfer
  Szenario: REST-Anfrage ohne Authorization wird vom PEP abgelehnt
    Wenn REST sende GET Anfrage an "${pepProxyUrl}/openapi.yaml" ohne Authorization

    # PEP muss mit 401 Unauthorized antworten
    Dann TGR finde die letzte Anfrage mit dem Pfad "/openapi.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "401"

  @rest_pep_transfer
  Szenario: REST-Anfrage mit ungültigem Token wird vom PEP abgelehnt
    Gegeben sei ein ungültiger ZETA-PEP AccessToken wird erzeugt

    Wenn REST sende GET Anfrage an "${pepProxyUrl}/openapi.yaml" mit Authorization "${ZETA_PEP_AUTHZ}"

    Dann TGR finde die letzte Anfrage mit dem Pfad "/openapi.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "401"

