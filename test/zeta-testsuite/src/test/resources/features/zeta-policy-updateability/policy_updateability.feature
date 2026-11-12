#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_updateability'
#
# @Ignore: Nutzt die LOKALE OPA Policy-Admin-API (PUT /v1/policies/). Die echte
# RU-DEV-OPA bietet keinen von uns nutzbaren Admin-Zugang → beim Lauf gegen echte
# Komponenten übersprungen.
@PRODUKT:ZT_Cluster
@PRODUKT:ZETA
@local

Funktionalität: Policy Hot-Reload via OPA

  # Dieses Feature testet, dass OPA Policy-Updates im laufenden Betrieb
  # erkennt und anwendet, ohne Neustart (Hot-Reload).
  #
  # Das erste Szenario nutzt die OPA Policy REST API (PUT /v1/policies/) um Policies
  # zur Laufzeit auszutauschen. In Produktion würde OPA stattdessen Policy-Bundles
  # per Bundle-Polling aus einer Local Artifact Registry laden - genau dieses
  # produktive Verhalten deckt das zweite Szenario zusätzlich ab, indem es ein echtes
  # Policy-Bundle in einer (lokalen, nginx-basierten) Local Artifact Registry
  # veröffentlicht und verifiziert, dass OPA es per Bundle-Polling automatisch lädt.

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "pdpBaseUrl" auf "http://${ports.host}:${ports.poppPdpPort}"

  # ===========================================================================
  # Policy Hot-Reload: P1 aktiv → P11 wird veröffentlicht → P11 aktiv
  # ===========================================================================
  @TCID:ZETA_UPDATEABILITY_POLICY_HOT_RELOAD
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
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

    # Aufräumen (OPA auf allow-all zurücksetzen) erfolgt automatisch über den
    # @After("@policy_updateability")-Hook in PolicyUpdateabilitySteps, und zwar auch dann, wenn
    # dieses Szenario vorzeitig fehlschlägt - so wird verhindert, dass Policy "P11" oder gar keine
    # Policy nachfolgende Szenarien (z.B. client_authentifizierung, zeta-gitti) im selben Testlauf
    # mit einer fremden professionOID/OPA-Entscheidung stört.

  # ===========================================================================
  # Policy Hot-Reload via Bundle-Polling: BP1 aktiv → BP11 wird als Bundle veröffentlicht → BP11 aktiv
  # ===========================================================================
  # Verifiziert das produktive Hot-Reload-Verhalten (Bundle-Polling aus einer Local Artifact
  # Registry), siehe PolicyBundlePollingSteps. Das Test-Bundle nutzt ein isoliertes Policy-Paket
  # ("zeta.bundletest.authz", Root "zeta/bundletest"), damit es nicht mit der produktiven
  # "zeta.authz"-Policy (siehe obiges Szenario) kollidiert. Die Verifikation erfolgt deshalb direkt
  # über die OPA Decision API statt über einen echten PS-Profil-Request an ZETA.
  @TCID:ZETA_UPDATEABILITY_POLICY_HOT_RELOAD_BUNDLE
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @policy_updateability
  @policy_updateability_bundle
  Szenario: OPA wendet Policy-Updates via Bundle-Polling aus der Local Artifact Registry zur Laufzeit an (Hot-Reload)
    # --- Vorbedingung: Local Artifact Registry ist leer, OPA hat kein Test-Bundle geladen ---
    Gegeben sei die Bundle Registry ist leer

    # --- Phase 1: Policy-Bundle BP1 in der Local Artifact Registry veröffentlichen ---
    Gegeben sei die Policy "BP1" ist als Bundle in der Local Artifact Registry verfügbar
    Und die Policy "BP11" ist als Bundle verfügbar aber NICHT in der Local Artifact Registry veröffentlicht
    Und das PS-Profil "PS1" passt zur Bundle-Policy "BP1" aber nicht zu "BP11"
    Und das PS-Profil "PS11" passt zur Bundle-Policy "BP11" aber nicht zu "BP1"

    # --- Phase 2: OPA pollt die Local Artifact Registry und aktiviert das Bundle (Hot-Reload) ---
    Dann hat ZETA die Policy "BP1" als Bundle geladen

    # --- Phase 3: PS1 wird mit BP1 akzeptiert ---
    Wenn PS-Profil "PS1" eine Anfrage an die Bundle-Policy sendet
    Dann liefert die Bundle-Policy die Entscheidung "allow"

    # --- Phase 4: PS11 wird mit BP1 abgelehnt ---
    Wenn PS-Profil "PS11" eine Anfrage an die Bundle-Policy sendet
    Dann liefert die Bundle-Policy die Entscheidung "deny"

    # --- Phase 5: Policy-Bundle BP11 wird veröffentlicht (Patch-Update, Bundle-Polling erkennt es) ---
    Wenn die Policy "BP11" als Bundle in der Local Artifact Registry veröffentlicht wird
    Dann hat ZETA die Policy "BP11" als Bundle geladen

    # --- Phase 6: PS1 wird mit BP11 abgelehnt ---
    Wenn PS-Profil "PS1" eine Anfrage an die Bundle-Policy sendet
    Dann liefert die Bundle-Policy die Entscheidung "deny"

    # --- Phase 7: PS11 wird mit BP11 akzeptiert ---
    Wenn PS-Profil "PS11" eine Anfrage an die Bundle-Policy sendet
    Dann liefert die Bundle-Policy die Entscheidung "allow"

    # Aufräumen (Bundle aus der Local Artifact Registry entfernen) erfolgt automatisch über den
    # @After("@policy_updateability_bundle")-Hook in PolicyBundlePollingSteps - auch im
    # Fehlschlagsfall. Ein OPA-seitiges Zurücksetzen ist hier (anders als beim REST-Szenario oben)
    # nicht nötig, da der isolierte Bundle-Test-Root "zeta/bundletest" von keiner echten
    # ZETA-Komponente abgefragt wird.
