#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_updateability'
@PRODUKT:ZETA

Funktionalität: Policy Hot-Reload via OPA

  # Dieses Feature testet, dass OPA Policy-Updates im laufenden Betrieb
  # erkennt und anwendet, ohne Neustart (Hot-Reload).
  #
  # Dieser Test nutzt die OPA Policy REST API (PUT /v1/policies/) um Policies
  # zur Laufzeit auszutauschen. In Produktion würde OPA stattdessen Policy-Bundles
  # per Bundle-Polling aus einer Local Artifact Registry laden.

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "pdpBaseUrl" auf "http://127.0.0.1:${ports.poppPdpPort}"

  # ===========================================================================
  # Policy Hot-Reload: P1 aktiv → P11 wird veröffentlicht → P11 aktiv
  # ===========================================================================

  @policy_updateability
  Szenario: OPA wendet Policy-Updates zur Laufzeit an (Hot-Reload)
    # --- Vorbedingung: OPA hat keine anwendungsspezifische Policy geladen ---
    Gegeben sei die OPA Policy Registry ist leer

    # --- Phase 1: Policy P1 in OPA Registry veröffentlichen ---
    Gegeben sei die Policy "P1" ist in der OPA Registry verfügbar
    Und die Policy "P11" ist verfügbar aber NICHT in der OPA Registry veröffentlicht
    Und das PS-Profil "PS1" passt zu Policy "P1" aber nicht zu "P11"
    Und das PS-Profil "PS11" passt zu Policy "P11" aber nicht zu "P1"

    # --- Phase 2: OPA ist gestartet und hat P1 geladen ---
    Gegeben sei OPA ist erreichbar
    Dann hat ZETA die Policy "P1" geladen

    # --- Phase 3: PS1 wird mit P1 akzeptiert ---
    Wenn PS-Profil "PS1" eine Anfrage an ZETA sendet
    Dann antwortet ZETA mit Status "2xx"

    # --- Phase 4: PS11 wird mit P1 abgelehnt ---
    Wenn PS-Profil "PS11" eine Anfrage an ZETA sendet
    Dann antwortet ZETA mit Status "4xx"

    # --- Phase 5: Policy P11 wird veröffentlicht (Patch-Update) ---
    Wenn die Policy "P11" in der OPA Registry veröffentlicht wird
    Dann hat ZETA die Policy "P11" geladen

    # --- Phase 6: PS1 wird mit P11 abgelehnt ---
    Wenn PS-Profil "PS1" eine Anfrage an ZETA sendet
    Dann antwortet ZETA mit Status "4xx"

    # --- Phase 7: PS11 wird mit P11 akzeptiert ---
    Wenn PS-Profil "PS11" eine Anfrage an ZETA sendet
    Dann antwortet ZETA mit Status "2xx"
