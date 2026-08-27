#language:de
# Einfache lokale Performance-Tests für ZeTA-Komponenten (PEP, PDP).
# Ausführen mit:
#./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@performance'
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
Funktionalität: ZeTA Komponenten - lokaler Performance-Test
  @TCID:ZETA_PERFORMANCE_BASE_RESPONSE_TIME
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @performance
  Szenariogrundriss: <Komponente> - Durchsatz und Latenz
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Wenn parallel 500 GET-Anfragen mit 20 Threads an "<URL>" gesendet werden
    Dann war die Fehlerrate unter 1.0 Prozent
    Und war der Durchsatz mindestens 50.0 Anfragen pro Sekunde
    Und war die p95-Latenz unter 500 ms

    Beispiele:
      | Komponente    | URL                                                                                             |
      | PoPP ZeTA PEP | ${zeta.paths.popp.pep.baseUrl}${zeta.paths.wellKnown.oauthProtectedResource}                    |
      | PoPP ZeTA PDP | ${zeta.paths.popp.pdp.baseUrl}${zeta.paths.pdp.realmPath}${zeta.paths.wellKnown.openidConfiguration} |
      | VSDM ZeTA PEP | ${zeta.paths.vsdm.pep.baseUrl}${zeta.paths.wellKnown.oauthProtectedResource}                    |
      | VSDM ZeTA PDP | ${zeta.paths.vsdm.pdp.baseUrl}${zeta.paths.pdp.realmPath}${zeta.paths.wellKnown.openidConfiguration} |

