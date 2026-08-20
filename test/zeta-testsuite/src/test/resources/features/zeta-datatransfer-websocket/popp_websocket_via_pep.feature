#language:de
# Befehl zum Ausführen der WebSocket-Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@websocket' -Dzeta.env=local
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:ZETA


Funktionalität: PoPP WebSocket-Kommunikation über ZETA-PEP

  # Dieses Feature testet den ZETA-geschützten WebSocket-Datentransfer für PoPP.
  #
  # Die gesamte Kette wird geprüft:
  #   Client → PEP (Port ${ports.poppPepPort}, pep on, DPoP-Validierung) → PoPP-Server (8443/ws)
  #
  # Der PEP (ngx_pep) validiert beim WebSocket-Upgrade-Handshake:
  #   - Authorization: Bearer <access_token>  (vom PDP via Token-Exchange)
  #   - DPoP: <dpop_proof>                    (gebunden an die Request-URL)
  #
  # PoPP-Token ist NICHT erforderlich (pep_require_popp = off).
  #
  # Gutfall: Gültiges Token → PEP leitet WebSocket-Upgrade an PoPP-Server weiter
  # Negativtests: Ungültiges/fehlendes Token → PEP lehnt Handshake ab

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und setze Anfrage Timeout für WebSocket Verbindungen auf 10 Sekunden
    Und setze Timeout für WebSocket Nachrichten auf 10 Sekunden
    Und deaktiviere HTTP Proxy für WebSocket

  @TCID:ZETA_WS_HANDSHAKE_WITH_VALID_AUTH_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @websocket @popp @pep
  Szenario: ZETA-PEP erlaubt WebSocket-Upgrade mit gültigem Token — PoPP-Token über PoPP-Client
    # Gutfall: Der PoPP-Client erzeugt den Access-Token (DCR + Token-Exchange) und den
    # DPoP-Proof selbst und öffnet die WebSocket-Verbindung zum PoPP-Server über
    # wss://popp-zeta-ingress/ws → popp-zeta-pep (ZETA-PEP) → popp-server:8443/ws.
    # D.h. ein erfolgreich erzeugtes PoPP-Token beweist implizit, dass der ZETA-PEP den
    # WebSocket-Upgrade-Handshake mit einem gültigen Token durchgelassen hat.
    Wenn TGR sende eine POST Anfrage an "${tiger.popp.client.url}" mit ContentType "application/json" und folgenden mehrzeiligen Daten:
      """
      {
        "communicationType": "contact-virtual",
        "clientSessionId": "123456",
        "virtualCard": "IMG_eGK_G21_TU_root6 1.xml"
      }
      """
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.status" überein mit "OK"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.token"

    # Expliziter Nachweis des WebSocket-Upgrades über den ZETA-PEP:
    Und TGR finde die letzte Anfrage mit dem Pfad "/popp/practitioner/api/v1/token-generation-ehc"
    Und TGR prüfe aktuelle Anfrage stimmt im Knoten "$.header.Upgrade" überein mit "websocket"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "101"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.header.Upgrade" überein mit "websocket"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.header.ZETA-API-Version"

  @TCID:ZETA_WS_HANDSHAKE_WITH_INVALID_AUTH_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @websocket @popp @pep
  Szenario: ZETA-PEP lehnt WebSocket-Handshake mit ungültigem Authorization Token ab
    # Negativtest: Ungültiges JWT → PEP lehnt Upgrade-Handshake ab
    Gegeben sei ein ungültiger ZETA-PEP AccessToken wird erzeugt
    Und lösche alle WebSocket Handshake Header
    Und setze WebSocket Handshake Header "Authorization" auf "${ZETA_PEP_AUTHZ}"

    Wenn eine plain WebSocket Verbindung zu "ws://127.0.0.1:${ports.poppPepPort}/ws" mit den gesetzten Handshake Headern fehlschlägt

  @TCID:ZETA_WS_HANDSHAKE_WITH_MISSING_AUTH_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @websocket @popp @pep
  Szenario: ZETA-PEP lehnt WebSocket-Handshake ohne Authorization ab
    # Negativtest: Kein Auth-Header → PEP bricht den Handshake ab
    Gegeben sei lösche alle WebSocket Handshake Header

    Wenn eine plain WebSocket Verbindung zu "ws://127.0.0.1:${ports.poppPepPort}/ws" mit den gesetzten Handshake Headern fehlschlägt
