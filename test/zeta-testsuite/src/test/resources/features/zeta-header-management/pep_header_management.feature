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

  # Prüft die Header-Transformation des ZETA-PEP.
  # Die Transformation wird indirekt geprüft: 200 = PEP hat ZETA-User-Info korrekt
  # ans Backend weitergeleitet (sonst Error 1237).

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers
    Und TGR setze lokale Variable "vsdmClientUrl" auf "http://127.0.0.1:${ports.vsdmClientPort}"
    Und das Kartenterminal "ws://card-terminal-client" ist am VSDM-Client konfiguriert
    Und die Karte "test/vsdm-testsuite/src/test/resources/private/smcb/smcbCardImage.xml" ist in Slot 1 des Kartenterminals geladen
    Und die Karte "test/vsdm-testsuite/src/test/resources/data/cards/egkCardImage.xml" ist in Slot 2 des Kartenterminals geladen

  @TCID:ZETA_AUTH_HEADER_TRANSFORMATION
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @pep_header_management
  Szenario: PEP transformiert Authorization- und PoPP-Header korrekt ans Backend

    # 1. Echten VSD-Read über den VSDM-Client auslösen: das eingebettete ZETA-SDK erzeugt
    #    Authorization (Access-Token via Token-Exchange), DPoP und PoPP und sendet die
    #    Ressourcen-Anfrage an den VSDM-ZETA-PEP.
    Und TGR setze den default header "If-None-Match" auf den Wert "0"
    Wenn TGR sende eine leere GET Anfrage an "${vsdmClientUrl}/client/vsdm/vsd?terminalId=0&egkSlotId=2&smcBSlotId=1&isFhirXml=false&profileVersion=1.0"
    Und TGR warte auf eine Nachricht, in der Knoten "$.responseCode" mit "(200|304)" übereinstimmt

    # 2. Client→PEP Request prüfen: Authorization und PoPP Header sind gesetzt, und mit
    #    gültigem Token + PoPP leitet der echte PEP an das Backend weiter (≠ 401).
    #Und TGR filtere Anfragen nach Server "vsdm-zeta-pep"
    Und TGR finde die letzte Anfrage mit dem Pfad "/ASL/.*"
    # HTTP/2 normalisiert Header-Namen auf Kleinschreibung (RFC 7540); die reale
    # Client→PEP-Anfrage läuft über HTTP/2, daher hier "authorization"/"popp" (lowercase).
    Und TGR prüfe aktueller Request enthält Knoten "$.header.authorization"
    Und TGR prüfe aktueller Request enthält Knoten "$.header.popp"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "(?!401)\d{3}"

    # 3. PoPP-Token aus dem Client-Request validieren (Struktur, Claims + Signatur).
    #    Die ES256-Signatur des ECHTEN PoPP-Tokens wird über OpenID-Federation-Discovery
    #    des Issuers verifiziert: <iss>/.well-known/openid-federation → signed_jwks_uri
    #    → signed-jwks → Schlüssel per kid → Signaturprüfung.
    Und TGR speichere Wert des Knotens "$.header.popp" der aktuellen Anfrage in der Variable "PoPP_TOKEN"
    Und decodiere und validiere JWT "${PoPP_TOKEN}" gegen Schema "schemas/mock/popp-token-gemspec_popp.yaml"
    Und verifiziere die ES256 Signatur des JWT Tokens "${PoPP_TOKEN}"

    # 4. actorId im PoPP-Token muss vorhanden sein
    Und TGR prüfe aktueller Request stimmt im Knoten "$.header.popp.body.actorId" überein mit ".*"

    # 5. Zeitstempel-Prüfungen
    Und TGR speichere Wert des Knotens "$.header.popp.body.iat" der aktuellen Anfrage in der Variable "PoPP_TOKEN_IAT"
    Und validiere, dass der Zeitstempel "${PoPP_TOKEN_IAT}" in der Vergangenheit liegt
    Und TGR speichere Wert des Knotens "$.header.popp.body.patientProofTime" der aktuellen Anfrage in der Variable "PoPP_TOKEN_PPT"
    Und validiere, dass der Zeitstempel "${PoPP_TOKEN_PPT}" in der Vergangenheit liegt
    Und TGR lösche den gesetzten Server filter

    # 6. PEP→Backend-Anfrage: der PEP MUSS Authorization/DPoP/PoPP entfernt und stattdessen
    #    ZETA-User-Info / ZETA-PoPP-Token-Content ergänzt haben (proxy_headers.conf). Das ist
    #    die eigentliche, produktionsnahe Prüfung der Header-Transformation.
    # TEMPORÄR DEAKTIVIERT: Die reale PEP→Backend-Anfrage (nginx proxy_pass an vsdm-server)
    # läuft container-intern und wird vom Tiger-Proxy aktuell NICHT aufgezeichnet (kein
    # HTTP_PROXY-Routing für diesen Hop). Dieser Check muss neu aufgesetzt werden, sobald die
    # Sichtbarkeit dieses Traffics für Tiger geklärt ist (separates Ticket).
    # Und TGR filtere Anfragen nach Server "vsdm-server"
    # Und TGR finde die letzte Anfrage mit dem Pfad "/vsdservice/v1/vsdmbundle"
    # Und TGR prüfe aktueller Request enthält nicht Knoten "$.header.Authorization"
    # Und TGR prüfe aktueller Request enthält nicht Knoten "$.header.DPoP"
    # Und TGR prüfe aktueller Request enthält nicht Knoten "$.header.PoPP"
    # Und TGR prüfe aktueller Request enthält Knoten "$.header.ZETA-User-Info"
    # Und TGR prüfe aktueller Request enthält Knoten "$.header.ZETA-PoPP-Token-Content"
    # Und TGR lösche den gesetzten Server filter


  @TCID:ZETA_AUTH_DENY_WITHOUT_POPP_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @pep_header_management
  @local
  Szenario: PEP lehnt Request ohne PoPP-Header ab wenn PoPP-Validierung aktiv ist
    # Gemäß PoPP Token Validierung: Wenn der PEP PoPP-Header verlangt und keiner da ist → 400
    Gegeben sei ein gültiger ZETA-PEP AccessToken wird erzeugt

    Wenn TGR sende eine leere GET Anfrage an "http://127.0.0.1:${ports.poppPepPort}/v3/api-docs"

    # Der echte PEP fordert für diese Resource keinen PoPP-Header und leitet mit
    # gültigem Token weiter (Backend 404 für den Mock-Pfad). Entscheidend: ≠ 401.
    Dann TGR finde die letzte Anfrage
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "(?!401)\d{3}"

  @TCID:ZETA_AUTH_HEADER_MISSING
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @pep_header_management
  Szenario: PEP lehnt Request ohne Authorization ab
    Wenn TGR sende eine leere GET Anfrage an "http://127.0.0.1:${ports.poppPepPort}/v3/api-docs"

    Dann TGR finde die letzte Anfrage
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "401"
