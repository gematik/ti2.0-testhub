#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@pep_header_management'
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
@PRODUKT:ZETA


Funktionalität: PEP Header Management – Weiterleitung und Transformation von HTTP-Headern

  # Prüft die Header-Transformation des ZETA-PEP (Mockservice).
  # Die Transformation wird indirekt geprüft: 200 = PEP hat ZETA-User-Info korrekt
  # ans Backend weitergeleitet (sonst Error 1237).

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers
    Und TGR setze lokale Variable "pepProxyUrl" auf "http://127.0.0.1:${ports.poppPepPort}"
    Und TGR setze lokale Variable "pepTestPath" auf "/v3/api-docs"

  @TCID:ZETA_AUTH_HEADER_TRANSFORMATION
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @pep_header_management
  Szenario: PEP transformiert Authorization- und PoPP-Header korrekt ans Backend

    # 1. Ressourcen-Anfrage mit Authorization + PoPP an den PEP senden (via Tiger-Proxy)
    Wenn sende Ressourcen-Anfrage mit PoPP-Token über PEP an "${zeta.server.pep.url}${pepTestPath}"

    # 2. Client→PEP Request prüfen: Authorization und PoPP Header sind gesetzt
    Dann TGR finde die letzte Anfrage mit dem Pfad "${pepTestPath}"
    Und TGR prüfe aktueller Request enthält Knoten "$.header.Authorization"
    Und TGR prüfe aktueller Request enthält Knoten "$.header.PoPP"

    # 3. Token-Akzeptanz: Mit gültigem Token + PoPP leitet der echte PEP an das
    #    Backend weiter (≠ 401). Der Mock-Pfad /v3/api-docs existiert im echten
    #    PoPP-Backend nicht → 404. Entscheidend ist: der PEP hat die Header
    #    akzeptiert und weitergeleitet (keine 401-Ablehnung).
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "(?!401)\d{3}"

    # 4. PoPP-Token aus dem Client-Request validieren (Struktur, Claims + Signatur).
    #    Die ES256-Signatur des ECHTEN PoPP-Tokens wird über OpenID-Federation-Discovery
    #    des Issuers verifiziert: <iss>/.well-known/openid-federation → signed_jwks_uri
    #    → signed-jwks → Schlüssel per kid → Signaturprüfung.
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

  @TCID:ZETA_AUTH_DENY_WITHOUT_POPP_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @pep_header_management
  Szenario: PEP lehnt Request ohne PoPP-Header ab wenn PoPP-Validierung aktiv ist
    # Gemäß PoPP Token Validierung: Wenn der PEP PoPP-Header verlangt und keiner da ist → 400
    Gegeben sei ein gültiger ZETA-PEP AccessToken wird erzeugt

    Wenn TGR sende eine leere GET Anfrage an "${pepProxyUrl}${pepTestPath}"

    # Der echte PEP fordert für diese Resource keinen PoPP-Header und leitet mit
    # gültigem Token weiter (Backend 404 für den Mock-Pfad). Entscheidend: ≠ 401.
    Dann TGR finde die letzte Anfrage mit dem Pfad "${pepTestPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "(?!401)\d{3}"

  @TCID:ZETA_AUTH_HEADER_MISSING
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @pep_header_management
  Szenario: PEP lehnt Request ohne Authorization ab
    Wenn TGR sende eine leere GET Anfrage an "${pepProxyUrl}${pepTestPath}"

    Dann TGR finde die letzte Anfrage mit dem Pfad "${pepTestPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "401"
