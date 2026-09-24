#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite verify -Dskip.inttests=false -Dzeta.env=local -Dcucumber.filter.tags='@smcb_max_clients'
@PRODUKT:ZT_Cluster
@PRODUKT:ZETA

Funktionalität: SMC-B maxClients Limit und LRU Eviction

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers
    Und TGR setze lokale Variable "pdpBaseUrl" auf "${zeta.server.pdp.issuer}"
    Und TGR setze lokale Variable "tigerProxyUrl" auf "http://localhost:${tiger.tigerProxy.proxyPort}"
    Und Keycloak unmanaged attributes sind für SMC-B Attribute aktiviert

  # ===========================================================================
  # Erfolgreicher Token-Exchange mit derselben SMC-B rund um die max_client_ids Grenze
  # ===========================================================================

  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:1
  @PRIO:1
  @client_registrierung @smcb_max_clients
  Szenariogrundriss: Erfolgreicher Token-Exchange mit derselben SMC-B rund um die max_client_ids Grenze
    # Bootstrap legt den User korrekt an (Federated Identity). Danach werden <client_ids_count>
    # client_ids per Admin API gesetzt. Ein neuer DCR-Client + Token-Exchange muss weiterhin
    # akzeptiert werden (HTTP 2xx) — unterhalb der Grenze ohne Eviction, an der Grenze dank
    # automatischer LRU-Eviction des am längsten ungenutzten Clients (A_25748-02) statt Ablehnung.

    Wenn ich eine neue Dynamic Client Registration erzwinge
    Wenn sende Token-Exchange-Request für Client "zeta-client" an "${zeta.server.pdp.tokenUrl}" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"
    Dann TGR finde die letzte Anfrage mit dem Pfad "/auth/realms/zeta-guard/protocol/openid-connect/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Wenn ich <client_ids_count> Client-IDs für denselben SMC-B User im Keycloak vorbereite
    Wenn ich eine neue Dynamic Client Registration erzwinge
    Wenn sende Token-Exchange-Request für Client "zeta-client" an "${zeta.server.pdp.tokenUrl}" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"
    Dann TGR finde die letzte Anfrage mit dem Pfad "/auth/realms/zeta-guard/protocol/openid-connect/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token" überein mit ".*"
    Und die Anzahl der Client-IDs des SMC-B Users beträgt höchstens <configured_max_client_ids_count>

    @TCID:ZETA_SMCB_MAX_CLIENTS_BELOW_LIMIT
    Beispiele: Unter der Grenze — keine Eviction nötig
      | client_ids_count | configured_max_client_ids_count |
      | 255              | 256                              |

    @TCID:ZETA_SMCB_MAX_CLIENTS_LIMIT_REACHED_WITH_LRU_EVICTION
    Beispiele: An der Grenze — Erfolg dank LRU-Eviction
      | client_ids_count | configured_max_client_ids_count |
      | 256              | 256                              |

