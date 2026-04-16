#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@smcb_authentisierung and not @Ignore'
#
# Hinweis: Diese Tests weisen die SMC-B-Authentisierung des ZETA-Clients nach.
# Ein echtes, per SMC-B-Zertifikat signiertes subject_token wird erzeugt und zusammen
# mit einer client_assertion über den Tiger-Proxy an den ZETA-PDP-Mock gesendet.
# Der mitgeschnittene Traffic wird geprüft.
@PRODUKT:ZETA

Funktionalität: SMC-B Authentisierung - ZETA-Client Authentisierung mittels SMC-B

  Diese Tests sollen nachweisen,
  dass der ZETA-Client sich korrekt mittels SMC-B-Zertifikat authentisiert,
  indem ein echtes SMC-B-signiertes subject_token erzeugt und der
  Token-Exchange-Flow gegen den PDP-Mock geprüft wird.

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten

  # ===========================================================================
  # Szenario 1: Gutfall - Token Exchange mit echtem SMC-B subject_token
  # ===========================================================================
  @smcb_authentisierung
  Szenario: Token Exchange mit SMC-B-signiertem subject_token liefert Access-Token (Gutfall)
    # Dieser Test prüft den erfolgreichen Token-Exchange-Flow:
    # 1. Echtes SMC-B-Zertifikat aus smcb_private.p12 wird geladen
    # 2. subject_token wird mit Brainpool P-256 R1 signiert (via BouncyCastle)
    # 3. client_assertion JWT (ES256/P-256) wird erzeugt
    # 4. Token-Exchange-Request wird über Tiger-Proxy an PDP-Mock gesendet
    # 5. PDP-Mock liest sub (TelematikID) und professionOid aus dem subject_token
    # 6. PDP-Mock stellt ein Access-Token aus

    # Token-Exchange mit echtem SMC-B subject_token und client_assertion über Tiger-Proxy senden
    Wenn sende SMC-B Token-Exchange-Request an "http://127.0.0.1:9112/token" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Request muss im Tiger-Proxy aufgezeichnet worden sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token" überein mit ".*"

    # Access-Token Schema-Validierung
    Und TGR speichere Wert des Knotens "$.body.access_token" der aktuellen Antwort in der Variable "ACCESS_TOKEN_JWT"
    Und decodiere und validiere JWT aus der aktuellen Antwort Knoten "$.body.access_token" gegen Schema "schemas/v_1_0/access-token.yaml" soft assert

  # ===========================================================================
  # Szenario 2: Token Exchange Request Body enthält alle erforderlichen Felder
  # ===========================================================================
  @smcb_authentisierung
  Szenario: Token Exchange Request Body enthält alle erforderlichen Felder (RFC 8693)
    # Dieser Test prüft, dass der Token-Exchange-Request alle Pflichtfelder enthält:
    # grant_type, subject_token_type, client_id, client_assertion, client_assertion_type

    # SMC-B Token-Exchange senden
    Wenn sende SMC-B Token-Exchange-Request an "http://127.0.0.1:9112/token" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Exchange-Request im Traffic finden
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."

    ## Request Body - grant_type und subject_token_type prüfen (RFC 8693)
    Und TGR prüfe aktueller Request enthält Knoten "$.body.grant_type"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.grant_type" überein mit "urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.subject_token_type"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.subject_token_type" überein mit "urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Ajwt"

    ## client_id vorhanden
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_id"

    ## client_assertion vorhanden und Typ korrekt
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion_type"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion_type" überein mit "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"

    ## client_assertion Schema-Validierung
    Und decodiere und validiere JWT aus dem aktuellen Request Knoten "$.body.client_assertion" gegen Schema "schemas/v_1_0/client-assertion-jwt.yaml"

  # ===========================================================================
  # Szenario 3: Client Assertion JWT Validierung
  # ===========================================================================
  @smcb_authentisierung
  Szenario: Client Assertion JWT enthält korrekte Struktur und gültige Signatur
    # Dieser Test weist nach, dass die client_assertion die korrekte Struktur hat,
    # mit ES256 signiert ist und die client_id als iss und sub enthält.
    # Ergänzt um Schema-Validierung, audience- und exp-Prüfung

    # SMC-B Token-Exchange senden
    Wenn sende SMC-B Token-Exchange-Request an "http://127.0.0.1:9112/token" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Exchange-Request im Traffic finden
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."

    # client_id und client_assertion extrahieren
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_id"
    Und TGR speichere Wert des Knotens "$.body.client_id" der aktuellen Anfrage in der Variable "CLIENT_ID"

    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion"
    Und TGR speichere Wert des Knotens "$.body.client_assertion" der aktuellen Anfrage in der Variable "CLIENT_ASSERTION_JWT"

    # Schema-Validierung der client_assertion gegen client-assertion-jwt.yaml
    Und decodiere und validiere JWT aus dem aktuellen Request Knoten "$.body.client_assertion" gegen Schema "schemas/v_1_0/client-assertion-jwt.yaml"

    ## Client Assertion Header
    # Algorithmus ist ES256
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.header.alg" überein mit "ES256"
    # Typ ist JWT
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.header.typ" überein mit "(?i)jwt"

    ## Client Assertion JWT Key
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.header.jwk.use" überein mit "sig"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.header.jwk.kty" überein mit "EC"

    # ES256-Signatur verifizieren (direkt aus dem Request-Knoten, um RBel-Serialisierungsprobleme zu vermeiden)
    Und verifiziere die ES256 Signatur des JWT aus dem aktuellen Request Knoten "$.body.client_assertion"

    ## Client Assertion Payload - issuer und subject müssen client_id entsprechen
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.body.iss" überein mit "${CLIENT_ID}"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.body.sub" überein mit "${CLIENT_ID}"

    ## Client Assertion Payload - audience enthält den Token-Endpoint
    # Hinweis: RBel unwrapped Single-Element-Arrays zu Strings, daher ohne .0 Zugriff
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion.body.aud"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.body.aud" überein mit "http://127.0.0.1:9112/token"

    ## Client Assertion Payload - exp muss in der Zukunft liegen
    Und TGR speichere Wert des Knotens "$.body.client_assertion.body.exp" der aktuellen Anfrage in der Variable "CLIENT_ASSERTION_EXP"
    Und validiere, dass der Zeitstempel "${CLIENT_ASSERTION_EXP}" in der Zukunft liegt

  # ===========================================================================
  # Szenario 4: subject_token enthält korrekte SMC-B-Daten (Bindung TelematikID)
  # ===========================================================================
  @smcb_authentisierung
  Szenario: SMC-B subject_token enthält TelematikID, korrekte Struktur und gültige Signatur
    # Dieser Test weist nach, dass das erzeugte subject_token
    # die korrekte Struktur hat, gegen das Schema validiert,
    # eine gültige ES256-Signatur besitzt und die TelematikID
    # aus dem SMC-B-Zertifikat enthält.

    # SMC-B Token-Exchange senden
    Wenn sende SMC-B Token-Exchange-Request an "http://127.0.0.1:9112/token" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Exchange-Request im Traffic finden
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."

    # grant_type und subject_token_type prüfen
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.grant_type" überein mit "urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.subject_token_type" überein mit "urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Ajwt"

    # subject_token als JWT extrahieren und gegen Schema validieren
    # (direkt aus dem Request-Knoten, um RBel-Serialisierungsprobleme zu vermeiden)
    Und decodiere und validiere JWT aus dem aktuellen Request Knoten "$.body.subject_token" gegen Schema "schemas/v_1_0/smb-id-token-jwt.yaml"

    # ES256-Signatur des subject_token verifizieren (SMC-B Brainpool P-256 R1)
    Und verifiziere die ES256 Signatur des JWT aus dem aktuellen Request Knoten "$.body.subject_token"

    # SMC-B Zertifikat aus dem x5c-Header extrahieren und Daten auslesen
    Und TGR speichere Wert des Knotens "$.body.subject_token.header.x5c.0" der aktuellen Anfrage in der Variable "smcbCertificate"
    Und schreibe Daten aus dem SMC-B Zertifikat "${smcbCertificate}" in die Variable "SMCB-INFO"

    # client_id extrahieren
    Und TGR speichere Wert des Knotens "$.body.client_id" der aktuellen Anfrage in der Variable "clientId"

    # iss im subject_token muss mit client_id übereinstimmen
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.subject_token.body.iss" überein mit "${clientId}"

    # client_id im Request Body muss übereinstimmen
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_id" überein mit "${clientId}"

    # iss in der client_assertion muss mit client_id übereinstimmen
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.body.iss" überein mit "${clientId}"

    # sub-Claim im subject_token muss mit der TelematikID aus dem Zertifikat übereinstimmen
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.subject_token.body.sub" überein mit "${SMCB-INFO.telematikId}"

  # ===========================================================================
  # Szenario 5: Access-Token enthält TelematikID und professionOID
  # ===========================================================================
  @smcb_authentisierung
  Szenario: Access-Token nach SMC-B Token Exchange enthält korrekte User-Info Claims
    # Dieser Test prüft End-to-End, dass der PDP-Mock die TelematikID und
    # professionOID aus dem SMC-B subject_token korrekt in das Access-Token übernimmt.
    # Hinweis: Der PDP-Mock speichert die TelematikID im clientId-Claim des Access-Tokens
    # und die professionOID im professionOid-Claim.

    # Token-Exchange mit echtem SMC-B subject_token und client_assertion
    Wenn sende SMC-B Token-Exchange-Request an "http://127.0.0.1:9112/token" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Prüfe, dass der Token-Request erfolgreich war
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."

    # SMC-B-Daten aus dem subject_token extrahieren
    Und TGR speichere Wert des Knotens "$.body.subject_token.header.x5c.0" der aktuellen Anfrage in der Variable "smcbCertificate"
    Und schreibe Daten aus dem SMC-B Zertifikat "${smcbCertificate}" in die Variable "SMCB-INFO"

    # Prüfe, dass sub im subject_token der TelematikID entspricht
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.subject_token.body.sub" überein mit "${SMCB-INFO.telematikId}"

    # Prüfe, dass das Access-Token vorhanden ist
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.access_token"

    # Access-Token Schema-Validierung gegen access-token.yaml
    Und TGR speichere Wert des Knotens "$.body.access_token" der aktuellen Antwort in der Variable "ACCESS_TOKEN_JWT"
    Und decodiere und validiere JWT aus der aktuellen Antwort Knoten "$.body.access_token" gegen Schema "schemas/v_1_0/access-token.yaml" soft assert

    # Prüfe, dass das Access-Token die TelematikID als clientId enthält
    # (Der PDP-Mock übergibt die TelematikID aus sub des subject_token als clientId im Access-Token)
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token.body.client_id" überein mit "${SMCB-INFO.telematikId}"

    # Prüfe, dass das Access-Token die professionOID enthält
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token.body.profession_oid" überein mit "${SMCB-INFO.professionId}"

  # ===========================================================================
  # Szenario 6: Client Assertion enthält Client Statement mit Attestation-Daten
  # ===========================================================================
  @smcb_authentisierung
  Szenario: Client Assertion JWT enthält Client Statement mit Attestation-Daten
    # Dieser Test prüft, dass die client_assertion ein client_statement mit
    # den erforderlichen Attestation-Daten enthält (platform, sub, attestation_timestamp, posture).

    # SMC-B Token-Exchange senden
    Wenn sende SMC-B Token-Exchange-Request an "http://127.0.0.1:9112/token" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Exchange-Request im Traffic finden
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."

    # client_id extrahieren
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_id"
    Und TGR speichere Wert des Knotens "$.body.client_id" der aktuellen Anfrage in der Variable "CLIENT_ID"

    # client_assertion muss vorhanden sein
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion"

    # client_statement muss im client_assertion Payload vorhanden sein
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion.body.client_statement"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.body.client_statement.platform" überein mit "linux"

    # sub im client_statement muss client_id entsprechen
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_assertion.body.client_statement.sub" überein mit "${CLIENT_ID}"

    # attestation_timestamp muss vorhanden sein und in der Vergangenheit liegen
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion.body.client_statement.attestation_timestamp"
    Und TGR speichere Wert des Knotens "$.body.client_assertion.body.exp" der aktuellen Anfrage in der Variable "CLIENT_ASSERTION_EXP"
    Und TGR speichere Wert des Knotens "$.body.client_assertion.body.client_statement.attestation_timestamp" der aktuellen Anfrage in der Variable "CLIENT_ASSERTION_TIMESTAMP"
    Und validiere, dass der Zeitstempel "${CLIENT_ASSERTION_EXP}" später als "${CLIENT_ASSERTION_TIMESTAMP}" liegt
    Und validiere, dass der Zeitstempel "${CLIENT_ASSERTION_TIMESTAMP}" in der Vergangenheit liegt

    # posture muss vorhanden sein mit attestation_challenge und public_key
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion.body.client_statement.posture"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion.body.client_statement.posture.attestation_challenge"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_assertion.body.client_statement.posture.public_key"

