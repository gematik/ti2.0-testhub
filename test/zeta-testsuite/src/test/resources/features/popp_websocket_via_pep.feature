#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@websocket'
#
# Hinweis: Diese Tests verwenden KEINEN Tiger-Proxy, da WebSocket-Traffic nicht über den
# Tiger-Proxy geroutet werden kann. Die Kommunikation erfolgt direkt zum ZETA-PEP (Port 9110).
@PRODUKT:ZETA

Funktionalität: PoPP WebSocket über ZETA-PEP (Auth + Header-Transformation)

  # Dieses Feature testet den "ZETA-PEP Flow" (Mockservice), nicht den ZETA-Client. Der ZETA-Client wird in den E2E-Tests der PoPP-Testsuite getestet.
  # - Client baut WebSocket direkt zu ZETA-PEP auf (WebSocket-Traffic kann nicht über Tiger-Proxy geroutet werden)
  # - Client liefert Authorization: Bearer <JWT> im WS-Handshake
  # - ZETA-PEP validiert JWT (Signatur via zetakeystore.p12) und erzeugt daraus ZETA-User-Info
  # - ZETA-PEP öffnet WebSocket zum PoPP Backend und reicht ZETA-User-Info im Handshake weiter
  # - PoPP erstellt danach eine TokenMessage (statt Error 1237)

  @websocket @popp @pep
  Szenario: TokenMessage kommt über ZETA-PEP Proxy wenn Authorization im Handshake gesetzt ist
    Gegeben sei setze Anfrage Timeout für WebSocket Verbindungen auf 10 Sekunden
    Und setze Timeout für WebSocket Nachrichten auf 10 Sekunden
    Und deaktiviere HTTP Proxy für WebSocket

    Und ein gültiger ZETA-PEP AccessToken wird erzeugt
    Und lösche alle WebSocket Handshake Header
    Und setze WebSocket Handshake Header "Authorization" auf "${ZETA_PEP_AUTHZ}"

    # Verbinde direkt zu popp-zeta-pep auf Port 9110
    Wenn eine plain WebSocket Verbindung zu "ws://127.0.0.1:9110/ws/popp/practitioner/api/v1/token-generation-ehc" mit den gesetzten Handshake Headern geöffnet wird

    # 1) Start
    Wenn eine WebSocket Nachricht gesendet wird:
      """
      {"type":"StartMessage","version":"1.0.0","cardConnectionType":"contact-simsvc","clientSessionId":"pep-token-session-1"}
      """

    # 2) Server schickt StandardScenario
    Dann wird eine WebSocket Nachricht empfangen
    Und enthält die letzte WebSocket Nachricht den Text "\"type\":\"StandardScenario\""

    # 3) Client beantwortet das Szenario
    Wenn eine WebSocket Nachricht gesendet wird:
      """
      {"type":"ScenarioResponse","steps":["4B564E523D583B494B4E523D583B9000"]}
      """

    # 4) Über PEP muss jetzt TokenMessage kommen (kein 1237)
    Dann wird eine WebSocket Nachricht empfangen
    Und enthält die letzte WebSocket Nachricht den Text "\"type\":\"Token\""
    Dann wird die WebSocket Verbindung geschlossen

  @websocket @popp @pep
  Szenario: ZETA-PEP lehnt WebSocket-Handshake mit ungültigem Authorization Token ab
    Gegeben sei setze Anfrage Timeout für WebSocket Verbindungen auf 10 Sekunden
    Und setze Timeout für WebSocket Nachrichten auf 10 Sekunden
    Und deaktiviere HTTP Proxy für WebSocket

    Und ein ungültiger ZETA-PEP AccessToken wird erzeugt
    Und lösche alle WebSocket Handshake Header
    Und setze WebSocket Handshake Header "Authorization" auf "${ZETA_PEP_AUTHZ}"

    Wenn eine plain WebSocket Verbindung zu "ws://127.0.0.1:9110/ws/popp/practitioner/api/v1/token-generation-ehc" mit den gesetzten Handshake Headern fehlschlägt

  @websocket @popp @pep
  Szenario: ZETA-PEP lehnt WebSocket-Handshake ohne Authorization ab
    Gegeben sei setze Anfrage Timeout für WebSocket Verbindungen auf 10 Sekunden
    Und setze Timeout für WebSocket Nachrichten auf 10 Sekunden
    Und deaktiviere HTTP Proxy für WebSocket

    Und lösche alle WebSocket Handshake Header

    # Erwartung: PEP bricht den Handshake mit 401 ab
    Wenn eine plain WebSocket Verbindung zu "ws://127.0.0.1:9110/ws/popp/practitioner/api/v1/token-generation-ehc" mit den gesetzten Handshake Headern fehlschlägt
