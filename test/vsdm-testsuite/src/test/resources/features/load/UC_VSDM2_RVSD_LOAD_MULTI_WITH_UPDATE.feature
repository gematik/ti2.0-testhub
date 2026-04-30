#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_26991-01
@TYPE:LOAD
Funktionalität: Abfrage der Versichertenstammdaten vom Fachdienst VSDM 2.0 unter Last

  @TCID:UC_VSDM2_RVSD_LOAD_MULTI_WITH_UPDATE
  @STATUS:Implementiert
  @MODUS:Halbautomatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Mehrfache Abfrage der VSD mit eGK und mit VSD Update unter Last

  Dieser Testfall ermittelt die Antwortzeiten für die Abfrage der VSD vom Fachdienst VSDM 2.0 unter Lastbedingungen.
  Der VSDM Ressource Server antwortet mit HTTP Code 200 und sendet die VSD als FHIR-Datensatz an das Primärsystem.
  Der Parameter 'Load-Level' steuert die Hintergrundlast und bezieht sich auf das spezifizierte Maximum von 1.000
  Aufrufen pro Sekunde. Der Parameter 'Number-Calls' definiert die Anzahl der VSD Abfragen, die ausgeführt werden und
  in die Berechnung einfließen. Der Parameter 'Max-Answer-Time' definiert die maximal erlaubte Antwortzeit in der
  Einheit Millisekunden.

  Die Hintergrundlast kann durch die Gatling-Simulation 'VsdmLoadSimulation' erzeugt werden, welche in Abhängigkeit
  von der Systemvariablen 'randomReadVsd' einen linearen oder nicht-linearen Lastverlauf simuliert. Ohne Angabe der
  Systemvariablen simuliert Gatling eine lineare Lastsimulation.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Angenommen das Primärsystem hat den Versorgungskontext als PoPP-Token gespeichert
    Angenommen der Fachdienst VSDM 2.0 wird mit <Calls-Per-Sec> Aufrufen pro Sekunde unter Last gesetzt
    Wenn das Primärsystem <Number-Calls> Anfragen mit VSD Update an den Fachdienst VSDM 2.0 sendet
    Dann überschreiten die Antworten des Fachdienstes VSDM 2.0 nicht den Maximalwert von <Max-Answer-Time> ms

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Calls-Per-Sec | Number-Calls | Max-Answer-Time |
      | "smcbCardImage.xml" | 2005      | "egkCardData.json" | 2006     | 100           | 100          | 1000            |
