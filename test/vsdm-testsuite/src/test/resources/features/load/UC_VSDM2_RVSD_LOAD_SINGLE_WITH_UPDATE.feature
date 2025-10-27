#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM2
@TYPE:LOAD
Funktionalität: Abfrage der Versichertenstammdaten vom Fachdienst VSDM 2.0 unter Last

  @TCID:UC_VSDM2_RVSD_LOAD_SINGLE_WITH_UPDATE
  @STATUS:InBearbeitung
  @MODUS:Halbautomatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Einmalige Abfrage der VSD mit eGK und mit VSD Update unter Last

  Dieser Testfall ermittelt die Antwortzeit für eine Abfrage der VSD vom Fachdienst VSDM 2.0 unter Lastbedingungen.
  Der VSDM Ressource Server antwortet mit HTTP Code 200 und sendet die VSD als FHIR-Datensatz an das Primärsystem.
  Der Parameter 'Load-Level' steuert die Hintergrundlast und bezieht sich auf das spezifizierte Maximum von 1.000
  Aufrufen pro Sekunde. Der Parameter 'Max-Answer-Time' definiert die maximal erlaubte Antwortzeit in Millisekunden.

  Die Hintergrundlast kann durch die Gatling-Simulation 'VsdmLoadSimulation' erzeugt werden, welche in Abhängigkeit
  von der Systemvariablen 'randomReadVsd' einen linearen oder nicht-linearen Lastverlauf simuliert. Ohne Angabe der
  Systemvariablen simuliert Gatling eine lineare Lastsimulation.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Angenommen das Primärsystem hat den Versorgungskontext als PoPP-Token gespeichert
    Angenommen der Fachdienst VSDM 2.0 befindet sich unter <Load-Level>% Maximallast
    Wenn das Primärsystem sich mit seiner SMC-B beim ZETA Guard des VSDM 2.0 Fachdienstes authentifiziert
    Dann erhält das Primärsystem einen Access- und Refresh-Token vom ZETA Guard
    Wenn das Primärsystem die VSD mittels PoPP- und Access-Token vom VSDM Ressource Server abfragt
    Und der VSDM Ressource Server beim E-Tag-Vergleich einen Unterschied feststellt
    Dann sendet der VSDM Ressource Server die aktualisierten VSD mit dem Statuscode <Http-Code> zum Primärsystem
    Und die Antwortzeit des Fachdienstes VSDM 2.0 überschreitet nicht den Maximalwert von <Max-Answer-Time> ms

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Load-Level | Max-Answer-Time | Http-Code |
      | "smcbCardImage.xml" | 2001      | "egkCardImage.xml" | 2002     | 30         | 1000            | 200       |
