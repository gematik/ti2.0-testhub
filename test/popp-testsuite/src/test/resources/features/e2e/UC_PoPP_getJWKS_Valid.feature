#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP_Service
@TYPE:E2E
Funktionalität: Prüfen des JWKS vom PoPP-Service

  @TCID:UC_PoPP_getJWKS_Valid
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenario: Erfolgreiche Prüfung des JWKS vom PoPP-Service

  Dieser Testfall testet den Anwendungsfall Signaturprüfung des PoPP-Token über JWKS vom PoPP-Service.

    Angenommen hole das EntityStatement vom Endpunkt
    Und TGR finde die erste Anfrage mit Pfad "/.well-known/openid-federation"
    Wenn frage das JWKS über den JWKS Link "!{rbel:currentResponseAsString('$..signed_jwks_uri')}" aus dem EntityStatement ab
    Und validiere das JWKS