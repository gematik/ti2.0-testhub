/*
 *
 * Copyright 2025 gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */
package de.gematik.ti20.popp;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepsHashdb {

  @Angenommen("die Schnittstelle I_PoPP_EHC_CertHash_Import ist beim PoPP-Service verfügbar")
  public void dieSchnittstelleIPoPPEHCCertHashImportIstBeimPoPPServiceVerfuegbar() {
    // Implementierung: Überprüfe Verfügbarkeit der Schnittstelle
  }

  @Angenommen(
      "der TSP etabliert einen TLS Kanal mit dem PoPP-Service mit einem gültigen Client-Zertifikat")
  public void derTSPEtabliertEinenTLSKanalMitDemPoPPServiceMitEinemGueltigenClientZertifikat() {
    // Implementierung: TLS-Verbindung mit Client-Zertifikat aufbauen
  }

  @Wenn(
      "der TSP die Schnittstelle I_PoPP_EHC_CertHash_Import {int} mit dem DER-codierten, signierten Payload {word} aufruft")
  public void
      derTSPDieSchnittstelleIPoPPEHCCertHashImportMitDemDERCodiertenSigniertenPayloadAufruft(
          final int anzahl, final String eContent) {
    // Implementierung: Schnittstelle mit DER-codiertem Payload aufrufen
  }

  @Dann("erhält der TSP eine Rückmeldung mit den folgenden Informationen:")
  public void erhaeltDerTSPEineRueckmeldungMitDenFolgendenInformationen(
      final io.cucumber.datatable.DataTable dataTable) {
    // Implementierung: Validiere Rückmeldung mit DataTable
    final List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
    for (final Map<String, String> row : rows) {
      final String typ = row.get("Typ");
      final String anzahl = row.get("Anzahl");
      log.info("Typ {} in Rückmeldung", typ);
      log.info("Anzahl {} in Rückmeldung", anzahl);
      // Validierungslogik
    }
  }

  @Und(
      "der TSP stellt eine TLS-Verbindung mit einem gesperrten Client-Zertifikat zum PoPP-Service her")
  public void derTSPStelltEineTLSVerbindungMitEinemGesperrtenClientZertifikatZumPoPPServiceHer() {
    // Implementierung: TLS-Verbindung mit gesperrtem Zertifikat vorbereiten
  }

  @Und(
      "der TSP stellt eine TLS-Verbindung mit einem abgelaufenen Client-Zertifikat zum PoPP-Service her")
  public void derTSPStelltEineTLSVerbindungMitEinemAbgelaufenenClientZertifikatZumPoPPServiceHer() {
    // Implementierung: TLS-Verbindung mit abgelaufenem Zertifikat vorbereiten
  }

  @Und(
      "der TSP stellt eine TLS-Verbindung mit einem Client-Zertifikat her, das nicht die Rolle `oid_tsp_egk` besitzt")
  public void
      derTSPStelltEineTLSVerbindungMitEinemClientZertifikatHerDasNichtDieRolleOidTspEgkBesitzt() {
    // Implementierung: TLS-Verbindung mit Zertifikat ohne oid_tsp_egk Rolle vorbereiten
  }

  @Wenn("der TLS-Handshake durchgeführt wird")
  public void derTLSHandshakeDurchgefuehrtWird() {
    // Implementierung: TLS-Handshake ausführen
  }

  @Dann("wird die Verbindung vom PoPP-Service abgelehnt")
  public void wirdDieVerbindungVomPoPPServiceAbgelehnt() {
    // Implementierung: Validiere Verbindungsablehnung
  }

  @Und("der TSP erhält eine Fehlermeldung {string}")
  public void derTSPErhaeltEineFehlermeldung(final String fehlermeldung) {
    // Implementierung: Validiere erwartete Fehlermeldung
  }
}
