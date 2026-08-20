# Konnektor-TLS-Material für `popp-client`

Dieses Verzeichnis wird vom Service `popp-client` (Profil `full`,
`compose-popp-services.yaml`) gemountet und für die TLS-Verbindung zu einem echten
Konnektor verwendet. Der PoPP-Client wählt die SMC-B-Quelle pro Request anhand des
Felds `communicationType`: Für `contact-connector`/`contactless-connector` wird der
Konnektor verwendet, für alle anderen Werte weiterhin die Keystore-Datei
(`../smcb-private/`). Beide Konfigurationen sind gleichzeitig auf demselben Container
gesetzt, ein separater Service/Profil ist nicht nötig.

Dateien in diesem Verzeichnis (Namen können über `CONNECTOR_KEYSTORE_FILE` /
`CONNECTOR_TRUSTSTORE_FILE` überschrieben werden, Default: `keystore.p12` /
`truststore.p12`; das Verzeichnis selbst über `CONNECTOR_KEYSTORE_DIR` /
`CONNECTOR_TRUSTSTORE_DIR`, Default: `./zeta/connector`):

- `keystore.p12` – Client-Zertifikat für die TLS-Verbindung zum Konnektor
  (muss zusätzlich in der Konnektor-Clientkonfiguration hinterlegt werden).
- `truststore.p12` – Konnektor-Serverzertifikat inkl. Zertifikatskette, z. B. per
  `openssl s_client -showcerts -connect <Konnektor-IP>:<Port>` ausgelesen und via
  `keytool -import` importiert.

**Die hier abgelegten `keystore.p12`/`truststore.p12` sind selbstsignierte Platzhalter**
(Passwort `changeit`), damit der `full`-Profil-Start auch ohne echten Konnektor
funktioniert (`CONNECTOR_SECURE_ENABLE` ist standardmäßig `true`, ohne gültiges
Truststore-Material würde der Container sonst beim Start fehlschlagen). Für die
Anbindung an einen echten Konnektor müssen sie durch das tatsächliche Client-Zertifikat
sowie die Konnektor-Zertifikatskette ersetzt werden. Zwei Varianten:

- **Persönlicher/echter Konnektor:** eigene Zertifikate lokal unter `private/` ablegen
  (gitignored, siehe `.gitignore`) und in einer eigenen, nicht versionierten
  `doc/docker/env-private/.my-own.env` via `CONNECTOR_KEYSTORE_FILE=private/keystore.p12`
  bzw. `CONNECTOR_TRUSTSTORE_FILE=private/truststore.p12` referenzieren.
- **Geteilter Referenz-Konnektor (z. B. gematik-Testkonnektor):** Zertifikate liegen
  versioniert unter `no-publish/test-data/zeta/connector/` (kein echtes Geheimnis, für
  alle Projektbeteiligten nutzbar) und werden über
  `CONNECTOR_KEYSTORE_DIR`/`CONNECTOR_TRUSTSTORE_DIR` referenziert, siehe
  `no-publish/test-data/zeta/connector/.shared-konnektor-kon41.env`.

Details zur Konnektor-Anbindung siehe
[popp-sample-code README, Abschnitt "b) Konnektor"](https://github.com/gematik/popp-sample-code/blob/main/README.md#b-konnektor).
