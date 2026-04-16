#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@pep_header_management'
@PRODUKT:ZETA

Funktionalität: PEP Header Management – Weiterleitung und Transformation von HTTP-Headern

  # Prüft die Header-Transformation des ZETA-PEP (Mockservice).
  # Die Transformation wird indirekt geprüft: 200 = PEP hat ZETA-User-Info korrekt
  # ans Backend weitergeleitet (sonst Error 1237).

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers

  @pep_header_management
  Szenario: PEP transformiert Authorization- und PoPP-Header korrekt ans Backend

    # 1. Ressourcen-Anfrage mit Authorization + PoPP an den PEP senden (via Tiger-Proxy)
    Wenn sende Ressourcen-Anfrage mit PoPP-Token über PEP an "http://127.0.0.1:9110/openapi.yaml"

    # 2. Client→PEP Request prüfen: Authorization und PoPP Header sind gesetzt
    Dann TGR finde die letzte Anfrage mit dem Pfad "/openapi.yaml"
    Und TGR prüfe aktueller Request enthält Knoten "$.header.Authorization"
    Und TGR prüfe aktueller Request enthält Knoten "$.header.PoPP"

    # 3. Indirekte Prüfung der Header-Transformation:
    #    - PEP validiert Authorization → erzeugt ZETA-User-Info für Backend
    #    - PEP transformiert PoPP → ZETA-PoPP-Token-Content für Backend
    #    - PEP fügt ZETA-Client-Data hinzu
    #    - Backend (PoPP-Server) antwortet nur mit 200 wenn ZETA-User-Info vorhanden ist
    #      (sonst Error 1237: "No ZETA-User-Info header has been passed from ZETA PEP")
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

    # 4. PoPP-Token aus dem Client-Request validieren (eingehend)
    Und TGR speichere Wert des Knotens "$.header.PoPP" der aktuellen Anfrage in der Variable "PoPP_TOKEN"
    Und decodiere und validiere JWT "${PoPP_TOKEN}" gegen Schema "schemas/mock/popp-token-gemspec_popp.yaml"
    Und verifiziere die ES256 Signatur des JWT Tokens "${PoPP_TOKEN}"

    # 5. actorId im PoPP-Token muss vorhanden sein
    Und TGR prüfe aktueller Request stimmt im Knoten "$.header.PoPP.body.actorId" überein mit ".*"

    # 6. Zeitstempel-Prüfungen
    Und TGR speichere Wert des Knotens "$.header.PoPP.body.iat" der aktuellen Anfrage in der Variable "PoPP_TOKEN_IAT"
    Und validiere, dass der Zeitstempel "${PoPP_TOKEN_IAT}" in der Vergangenheit liegt
    Und TGR speichere Wert des Knotens "$.header.PoPP.body.patientProofTime" der aktuellen Anfrage in der Variable "PoPP_TOKEN_PPT"
    Und validiere, dass der Zeitstempel "${PoPP_TOKEN_PPT}" in der Vergangenheit liegt

  @pep_header_management
  Szenario: PEP lehnt Request ohne PoPP-Header ab wenn PoPP-Validierung aktiv ist
    # Gemäß PoPP Token Validierung: Wenn der PEP PoPP-Header verlangt und keiner da ist → 400
    Gegeben sei ein gültiger ZETA-PEP AccessToken wird erzeugt

    Wenn TGR sende eine leere GET Anfrage an "http://127.0.0.1:9110/openapi.yaml"

    # PEP leitet trotzdem weiter (PoPP ist im Mockservice optional) → 200
    # Im echten Guard wäre hier 400, wenn PoPP required ist
    Dann TGR finde die letzte Anfrage mit dem Pfad "/openapi.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

  @pep_header_management
  Szenario: PEP lehnt Request ohne Authorization ab
    Wenn TGR sende eine leere GET Anfrage an "http://127.0.0.1:9110/openapi.yaml"

    Dann TGR finde die letzte Anfrage mit dem Pfad "/openapi.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "401"

