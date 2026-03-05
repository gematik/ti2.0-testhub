#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@client_registrierung and not @Ignore'
#
# Hinweis: Diese Tests verwenden den Tiger-Proxy für HTTP-Traffic-Mitschnitt.
@PRODUKT:ZETA

Funktionalität: Client-Registrierung über ZETA-PDP

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "proxy" auf "http://${zeta_proxy_url}"

  @staging @client_registrierung
  Szenario: ZETA-PDP erlaubt Zugriff bei gültiger Policy (Gutfall)
    # Dieser Test prüft den erfolgreichen PDP-Flow:
    # 1. Test sendet Token-Request an ZETA-PDP über Tiger-Proxy
    # 2. PDP stellt Access-Token aus
    #
    # HINWEIS: Der PDP-Mock (zeta-pdp-server-mockservice) verwendet aktuell
    # keine echte OPA-Policy. Die authz.rego gibt immer allow=true zurück.

    # Token-Request über Tiger-Proxy an ZETA-PDP senden
    Wenn sende Token-Exchange-Request für Client "zeta-client" an "http://127.0.0.1:9112/token" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Request muss erfolgreich sein (2xx)
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token" überein mit ".*"

  @Ignore @staging @client_registrierung
  Szenariogrundriss: Client-Registrierung wird wegen Client Policy abgelehnt und begründet
    # STATUS: Nicht implementiert
    # HINWEIS: Dieser Test benötigt eine echte OPA-Policy in authz.rego,
    # die Input-Werte validiert. Aktuell gibt der Mock immer allow=true zurück.

    Gegeben sei TGR sende eine leere GET Anfrage an "${zeta.paths.client.reset}"

    # OPA Request manipulieren - ungültige Werte setzen
    # Der Guard sendet diese Daten an OPA, wir manipulieren den Request um Policy-Ablehnungen zu testen
    Und TGR setze lokale Variable "opaCondition" auf "isRequest && request.path =~ '.*${zeta.paths.opa.decisionPath}'"
    Dann Setze im TigerProxy für die Nachricht "${opaCondition}" die Manipulation auf Feld "<OpaInputField>" und Wert "<NeuerWert>" und 1 Ausführungen

    Wenn TGR sende eine leere GET Anfrage an "${zeta.paths.client.vsdRequest}"

    # OPA Decision prüfen - sollte allow=false liefern
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.opa.decisionPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.result.allow" überein mit "false"

    # Registrierungs-/Token-Request muss mit 403 abgelehnt werden
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.guard.tokenEndpointPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "403"
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "body"
    Und validiere "${body}" gegen Schema "schemas/v_1_0/zeta-error.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.error_description" überein mit ".*<ErwarteterHinweis>.*"

    Beispiele: Ungültige Policy-Werte
      | OpaInputField                                         | NeuerWert                    | ErwarteterHinweis                                               |
      | $.body.input.user_info.professionOID                  | 1.2.276.0.76.4.999           | ${testdata.policy_rejection.invalid_profession_oid_error_hint}  |
      | $.body.input.client_assertion.posture.product_id      | unknown_product              | ${testdata.policy_rejection.invalid_product_id_error_hint}      |
      | $.body.input.client_assertion.posture.product_version | 99.99.99                     | ${testdata.policy_rejection.invalid_product_version_error_hint} |
      | $.body.input.authorization_request.scopes.0           | invalid_scope_xyz            | ${testdata.policy_rejection.invalid_scope_error_hint}           |
      | $.body.input.authorization_request.aud.0              | https://evil.example.com/api | ${testdata.policy_rejection.invalid_audience_error_hint}        |
