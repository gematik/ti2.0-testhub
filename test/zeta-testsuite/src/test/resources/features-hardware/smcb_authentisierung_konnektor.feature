#language:de
# Szenario, das physische Geräte (echter Konnektor, SMC-B) benötigt.
# Es liegt bewusst außerhalb von src/test/resources/features, damit es weder von der
# Polarion-Synchronisation (polarion-parse/polarion-sync) noch von der CI-Pipeline erfasst wird.
@PRODUKT:ZT_Cluster
Funktionalität: SMC-B Authentisierung über echten Konnektor (Hardware)

  Grundlage:
    Gegeben sei der popp-client wurde neu gestartet, um seinen ZeTA-Token-Cache zu leeren
    Und TGR lösche aufgezeichnete Nachrichten

  @TCID:SMCB_AUTH_REAL_KONNEKTOR
  @STATUS:Implementiert
  @TESTSTUFE:2
  @PRIO:3
  @Ignore @popp @hardware
  # STATUS: @Ignore — benötigt einen echten, erreichbaren Konnektor (SMC-B via Karte,
  # nicht Keystore). Voraussetzungen zum manuellen Ausführen:
  #   1. Zertifikate besorgen: entweder eigene, echte Konnektor-Zertifikate unter
  #      infra/docker/backend/zeta/connector/private/{keystore,truststore}.p12 ablegen
  #      (siehe infra/docker/backend/zeta/connector/README.md), oder den geteilten
  #      gematik-Referenz-Testkonnektor unter no-publish/test-data/zeta/connector/
  #      verwenden.
  #   2. Env-Datei anlegen/verwenden:
  #      - eigener Konnektor: infra/docker/env-private/.my-own.env mit
  #        CONNECTOR_END_POINT_URL=https://<Konnektor-IP>:443 und den zugehörigen
  #        CONNECTOR_*/CONTEXT_*/CARD_TERMINAL_* Werten anlegen.
  #      - geteilter Testkonnektor: no-publish/test-data/zeta/connector/.shared-konnektor-kon41.env
  #        (bereits vorhanden, versioniert) verwenden.
  #   3. Stack starten (Beispiel mit geteiltem Testkonnektor):
  #      docker compose -f infra/docker/compose-local.yaml \
  #        --env-file ./infra/docker/.env \
  #        --env-file ./no-publish/test-data/zeta/connector/.shared-konnektor-kon41.env \
  #        --profile full up -d
  #   4. Diese Szenario-Tag manuell ausführen:
  #      ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@hardware'

  Szenario: PoPP-Client erzeugt PoPP-Token mit gesteckter SMC-B und eGK über echten Konnektor
    Wenn TGR sende eine POST Anfrage an "${zeta.server.poppClient.url}/token" mit ContentType "application/json" und folgenden mehrzeiligen Daten:

      """
      {"communicationType":"contact-connector","clientSessionId":"zeta-konnektor-smoketest-1"}
      """

    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    # Bei Erfolg liefert popp-client ein signiertes PoPP-Token zurück.
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.token"
