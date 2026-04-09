#language:de
# Befehl zum ausführen des Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@smoke'
@PRODUKT:ZETA

Funktionalität: Smoke Tests

  @smoke
  Szenariogrundriss: Availability check ZETA Komponenten (PDP / PEP / Ingress)
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Wenn TGR sende eine leere GET Anfrage an "<Ressource>"
    Dann TGR finde die letzte Anfrage mit dem Pfad "<Pfad>"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.httpVersion" überein mit "HTTP/1.1"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und gebe die Antwortzeit vom aktuellen Nachrichtenpaar aus

  Beispiele:
    | Ressource                              | Pfad                                    | #Ressource         |
    | ${smoke.endpoints.poppZetaPdp.url}     | ${smoke.endpoints.poppZetaPdp.path}     | #PoPP ZeTA PDP     |
    | ${smoke.endpoints.poppZetaPep.url}     | ${smoke.endpoints.poppZetaPep.path}     | #PoPP ZeTA PEP     |
    | ${smoke.endpoints.vsdmZetaIngress.url} | ${smoke.endpoints.vsdmZetaIngress.path} | #VSDM ZeTA Ingress |
