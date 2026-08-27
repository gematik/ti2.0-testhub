#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_ablehnungen and not @Ignore'
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
@PRODUKT:ZETA

Funktionalität: Client-Registrierungs-Policy und OPA-Integration (Policy-Ablehnungen)

  Grundlage:
    Gegeben sei TGR sende eine leere GET Anfrage an "${zeta.paths.client.reset}"
    Und TGR setze lokale Variable "proxy" auf "http://${zeta_proxy_url}"
    Und TGR setze lokale Variable "tigerProxyUrl" auf "http://localhost:${tiger.tigerProxy.proxyPort}"

  # Siehe README.md für Details zum Mechanismus (Remote-TigerProxy-Manipulation,
  # DNS-Interception, PDP-Registrierungs-Reset) und die getesteten Ablehnungsgründe.

  @TCID:ZETA_REGISTRATION_POLICY_DENIES
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @local
  @client_registrierung @policy_ablehnungen
  Szenariogrundriss: Client-Registrierung wird wegen Client Policy abgelehnt und begründet
    # Setzt die PDP-seitige Client-Registrierung zurück (löscht DCR-Client, startet
    # vsdm-client neu) und konfiguriert Kartenterminal/Karten erst DANACH neu, da der
    # Neustart die bestehende WebSocket-Verbindung zum Kartenterminal invalidiert.
    Und die ZeTA-PDP-Registrierung des VSDM-Clients ist vollständig zurückgesetzt
    Und das Kartenterminal "ws://card-terminal-client" ist am VSDM-Client konfiguriert
    Und die Karte "test/vsdm-testsuite/src/test/resources/private/cards/smcbCardImage.xml" ist in Slot 1 des Kartenterminals geladen
    Und die Karte "test/vsdm-testsuite/src/test/resources/data/cards/egkCardImage.xml" ist in Slot 2 des Kartenterminals geladen

    # Manipulation direkt auf der Remote-TigerProxy-Admin-API registrieren (siehe Hinweis oben)
    Und TGR setze lokale Variable "zeta.paths.tigerProxy.baseUrl" auf "http://${ports.host}:${ports.remoteTigerProxyAdminPort}"
    Und TGR setze lokale Variable "opaCondition" auf "isRequest && request.path =~ '.*${zeta.paths.opa.decisionPath}'"
    Dann Setze im TigerProxy für die Nachricht "${opaCondition}" die Manipulation auf Feld "<OpaInputField>" und Wert "<NeuerWert>" und 1 Ausführungen

    Wenn TGR sende eine leere GET Anfrage an "${zeta.paths.client.vsdRequest}"

    # Anfrage muss wegen des (von der OPA-Policy nicht zugelassenen) manipulierten Werts
    # mit HTTP 403 abgelehnt werden
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.client.vsdRequestPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "403"
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "body"
    Und validiere "${body}" gegen Schema "schemas/v_1_0/zeta-error.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.error" überein mit "access_denied"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.error_description" überein mit "policy_denied"

    Beispiele: Ungültige Policy-Werte
      | OpaInputField                                         | NeuerWert                    |
      | $.body.input.user_info.professionOID                  | 1.2.276.0.76.4.999           |
      | $.body.input.authorization_request.scopes.0           | invalid_scope_xyz            |
      | $.body.input.authorization_request.audience.0         | https://evil.example.com/api |
      | $.body.input.client_registration_data.product_id      | unknown-client               |
      | $.body.input.client_registration_data.product_version | 99.99.99                     |
