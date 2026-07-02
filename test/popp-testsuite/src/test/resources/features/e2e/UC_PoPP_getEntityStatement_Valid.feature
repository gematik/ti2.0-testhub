#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP_Service
@TYPE:E2E
Funktionalität: Abfrage des EntityStatements vom PoPP-Service

  @TCID:UC_PoPP_getEntityStatement_Valid
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenario: Erfolgreiche Abfrage des EntityStatements vom PoPP-Service

  Dieser Testfall testet den Anwendungsfall Abfrage des EntityStatements vom PoPP-Service.

    Angenommen hole das EntityStatement vom Endpunkt
    Und die Anfrage liefert ein gültiges EntityStatement mit einem gültigen JWKS-Link
