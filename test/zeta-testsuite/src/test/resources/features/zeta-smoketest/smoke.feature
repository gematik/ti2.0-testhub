#language:de
# Befehl zum ausführen des Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@smoke'
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:ZETA

@smoke
Funktionalität: Smoke Tests mit PoPP und VSDM2

  @TCID:ZETA_SMOKE_CLUSTER_AVAILABILITY_POPP
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  Szenariogrundriss: Availability check ZETA Komponenten (PDP / PEP / Ingress) für PoPP
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Wenn TGR sende eine leere GET Anfrage an "<Ressource>"
    Dann TGR finde die letzte Anfrage mit dem Pfad "<Pfad>"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.httpVersion" überein mit "HTTP/1.1"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und gebe die Antwortzeit vom aktuellen Nachrichtenpaar aus

  # Ressource = Basis-URL der Komponente + Well-known-Pfad, Pfad = derselbe
  # Well-known-Pfad. Beides kommt aus tiger/paths.yaml, damit Port und Pfad
  # nur an einer Stelle gepflegt werden.
  Beispiele:
    | Ressource                                                                                       | Pfad                                                                | #Ressource         |
    | ${zeta.paths.popp.pdp.baseUrl}${zeta.paths.pdp.realmPath}${zeta.paths.wellKnown.openidConfiguration} | ${zeta.paths.pdp.realmPath}${zeta.paths.wellKnown.openidConfiguration} | #PoPP ZeTA PDP     |
    | ${zeta.paths.popp.pep.baseUrl}${zeta.paths.wellKnown.oauthProtectedResource}                    | ${zeta.paths.wellKnown.oauthProtectedResource}                      | #PoPP ZeTA PEP     |
    | ${zeta.paths.popp.ingress.baseUrl}${zeta.paths.wellKnown.oauthProtectedResource}                | ${zeta.paths.wellKnown.oauthProtectedResource}                      | #PoPP ZETA Ingress |
    | ${zeta.paths.vsdm.pdp.baseUrl}${zeta.paths.pdp.realmPath}${zeta.paths.wellKnown.openidConfiguration} | ${zeta.paths.pdp.realmPath}${zeta.paths.wellKnown.openidConfiguration} | #VSDM ZETA PDP     |
    | ${zeta.paths.vsdm.pep.baseUrl}${zeta.paths.wellKnown.oauthProtectedResource}                    | ${zeta.paths.wellKnown.oauthProtectedResource}                      | #VSDM ZETA PEP     |
    | ${zeta.paths.vsdm.ingress.baseUrl}${zeta.paths.wellKnown.oauthProtectedResource}                | ${zeta.paths.wellKnown.oauthProtectedResource}                      | #VSDM ZeTA Ingress |
