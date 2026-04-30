#language:de
# Befehl zum Ausführen der WebSocket-Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@websocket'
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
    Gegeben sei setze Anfrage Timeout für WebSocket Verbindungen auf 10 Sekunden
    Und setze Timeout für WebSocket Nachrichten auf 10 Sekunden
    Und deaktiviere HTTP Proxy für WebSocket

  @Ignore @websocket @popp @pep
  # STATUS: @Ignore — ngx_pep:0.3.0 gibt 403 bei WebSocket-Upgrade trotz gültigem Token+DPoP.
  # Der PEP rekonstruiert bei Upgrade-Headern vermutlich ws:// statt http:// als Schema,
  # was den DPoP-htu-Vergleich fehlschlagen lässt. REST-Requests mit denselben Credentials
  # funktionieren (200 OK). Bug-Ticket: ngx_pep WebSocket-Upgrade 403
  Szenario: ZETA-PEP erlaubt WebSocket-Upgrade mit gültigem Token — PoPP StandardScenario
    # Gutfall: Gültiger AccessToken + DPoP-Proof → PEP lässt WebSocket-Upgrade durch
    # → PoPP-Server antwortet mit StandardScenario (APDU-Kommandos für eGK)
    Gegeben sei ein gültiger ZETA-PEP AccessToken wird erzeugt
    Und ein DPoP-Proof für "GET" "http://127.0.0.1:${ports.poppPepPort}/ws" wird erzeugt
    Und lösche alle WebSocket Handshake Header
    Und setze WebSocket Handshake Header "Authorization" auf "${ZETA_PEP_AUTHZ}"
    Und setze WebSocket Handshake Header "DPoP" auf "${ZETA_PEP_DPOP}"

    Wenn eine plain WebSocket Verbindung zu "ws://127.0.0.1:${ports.poppPepPort}/ws" mit den gesetzten Handshake Headern geöffnet wird

    # Start-Nachricht senden (PoPP WebSocket-Protokoll)
    Wenn eine WebSocket Nachricht gesendet wird:
      """
      {"type":"Start","version":"1.0.0","cardConnectionType":"contact-standard","clientSessionId":"zeta-ws-test-1"}
      """

    # Server antwortet mit dem ersten StandardScenario (APDU-Kommandos für eGK)
    Dann wird eine WebSocket Nachricht empfangen
    Und enthält die letzte WebSocket Nachricht den Text "\"type\":\"StandardScenario\""

    Dann wird die WebSocket Verbindung geschlossen

  @websocket @popp @pep
  Szenario: ZETA-PEP lehnt WebSocket-Handshake mit ungültigem Authorization Token ab
    # Negativtest: Ungültiges JWT → PEP lehnt Upgrade-Handshake ab
    Gegeben sei ein ungültiger ZETA-PEP AccessToken wird erzeugt
    Und lösche alle WebSocket Handshake Header
    Und setze WebSocket Handshake Header "Authorization" auf "${ZETA_PEP_AUTHZ}"

    Wenn eine plain WebSocket Verbindung zu "ws://127.0.0.1:${ports.poppPepPort}/ws" mit den gesetzten Handshake Headern fehlschlägt

  @websocket @popp @pep
  Szenario: ZETA-PEP lehnt WebSocket-Handshake ohne Authorization ab
    # Negativtest: Kein Auth-Header → PEP bricht den Handshake ab
    Gegeben sei lösche alle WebSocket Handshake Header

    Wenn eine plain WebSocket Verbindung zu "ws://127.0.0.1:${ports.poppPepPort}/ws" mit den gesetzten Handshake Headern fehlschlägt
