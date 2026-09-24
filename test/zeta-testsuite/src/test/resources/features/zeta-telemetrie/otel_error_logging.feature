#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@telemetrie and not @Ignore' -Dzeta.env=local
@PRODUKT:ZT_Cluster
@PRODUKT:ZETA
@PRODUKT:Telemetrie
Funktionalität: Telemetrie & Observability - Erfassung von 4xx-Fehlern im OpenTelemetry Collector

  Grundlage:
    Wenn TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers
    Und TGR setze lokale Variable "pepProxyUrl" auf "${zeta.server.pep.url}"
    Und TGR setze lokale Variable "pepTestPath" auf "/v3/api-docs"


  @TCID:ZETA_OTEL_PEP_401_LOGGING
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:1
  @PRIO:1
  @telemetrie @local
  Szenario: Nicht autorisierte Anfrage an den PEP erzeugt HTTP 401 und wird als Telemetrie-Event erfasst
    # 1. Anfrage ohne Authorization-Header an den PEP senden
    Wenn TGR sende eine leere GET Anfrage an "${pepProxyUrl}${pepTestPath}"

    # 2. Prüfen, dass der PEP mit 401 ablehnt
    Dann TGR finde die letzte Anfrage mit dem Pfad "${pepTestPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "401"

    # 3. Prüfen, dass der Otel-Collector den 401-Fehler protokolliert hat
    Und die Fehlermeldung mit Statuscode "401" ist in den Logs des Containers "otel-collector" vorhanden

    # 4. Zusätzlich die im Span/Resource enthaltenen Telemetrie-Attribute prüfen
    # Hinweis: "http.route" liefert bei nginx das gematchte Location-Pattern (hier "/", da
    # keine eigene Location für "${pepTestPath}" existiert) - deshalb wird stattdessen die
    # tatsächliche Request-URI über "http.target" geprüft.
    Und die folgenden Telemetrie-Attribute sind in den Logs des Containers "otel-collector" vorhanden:
      | service.name: Str(popp-zeta-pep)     |
      | http.method: Str(GET)                |
      | http.target: Str(${pepTestPath})     |
      | net.host.name: Str(popp-zeta-pep)    |
      | http.status_code: Int(401)           |
      | http.scheme: Str(http)               |

  @TCID:ZETA_OTEL_PDP_INVALID_GRANT_LOGGING
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:1
  @PRIO:1
  @telemetrie @local
  Szenario: Ungültiger Token-Request am PDP erzeugt 4xx und wird als Telemetrie-Event erfasst
    # 1. Ungültigen Token-Request direkt an den Keycloak Token-Endpunkt senden
    Wenn TGR sende eine POST Anfrage an "${zeta.server.pdp.tokenUrl}" mit ContentType "application/x-www-form-urlencoded" und folgenden mehrzeiligen Daten:
      """
      grant_type=invalid_grant_type&client_id=invalid-client
      """

    # 2. Prüfen, dass der PDP mit 400 Bad Request oder 401 antwortet
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.vsdm.tokenEndpointPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "400|401"

    # 3. Telemetrie-Nachweis im Otel-Collector verifizieren
    Und das Fehler-Event "invalid_grant" oder Statuscode "400" ist in den Logs des Containers "otel-collector" vorhanden

    # 4. Zusätzlich die im Span/Resource enthaltenen Telemetrie-Attribute prüfen
    # Hinweis: Keycloak/Quarkus nutzt hier bereits die neuen OpenTelemetry-HTTP-Semantic-Conventions
    # ("http.request.method"/"url.path" statt der beim nginx-PEP verwendeten "http.method"/"http.target").
    Und die folgenden Telemetrie-Attribute sind in den Logs des Containers "otel-collector" vorhanden:
      | service.name: Str(popp-zeta-pdp)                     |
      | service.version: Str(1.3.1)                          |
      | http.request.method: Str(POST)                       |
      | url.path: Str(${zeta.paths.vsdm.tokenEndpointPath})  |
