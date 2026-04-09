/*-
 * #%L
 * PoPP Testsuite
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.popp;

import static de.gematik.test.tiger.lib.TigerHttpClient.executeCommandWithContingentWait;
import static de.gematik.test.tiger.lib.TigerHttpClient.givenDefaultSpec;
import static de.gematik.ti20.popp.data.TestConstants.ARBITRARY_VALUE_NOT_AFTER_FOR_HASHDB_ENTRIES;
import static de.gematik.ti20.popp.data.TestConstants.PATH_TO_TSP_EGK_OSIG_P12;
import static de.gematik.ti20.popp.data.TestConstants.URL_HASH_DB_IMPORT_RU;
import static de.gematik.ti20.popp.data.TestConstants.VALID_HASH_DB_IMPORT_RESPONSE_FILE;
import static de.gematik.ti20.popp.data.TestConstants.VALID_HASH_DB_JOB_STATUS_RESPONSE_FILE;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.glue.TigerProxyGlue;
import de.gematik.test.tiger.lib.rbel.ModeType;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.rbel.RbelValidator;
import de.gematik.test.tiger.lib.rbel.RequestParameter;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.restassured.http.Method;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.htmlunit.http.HttpStatus;

@Slf4j
public class StepsHashdb {

  private static final String ALIAS_FOR_TRUSTSTORE = "alias";
  static KeyStore tspEntrySigner;
  TigerProxyGlue tigerProxyGlue = new TigerProxyGlue();
  private final RbelMessageRetriever rbelMessageRetriever;
  private final RbelValidator rbelValidator;

  public StepsHashdb(final RbelMessageRetriever rbelMessageRetriever) {
    this.rbelMessageRetriever = rbelMessageRetriever;
    this.rbelValidator = new RbelValidator();
  }

  public StepsHashdb() {
    this(RbelMessageRetriever.getInstance());
  }

  static {
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    Security.insertProviderAt(new BouncyCastleProvider(), 1);
    final byte[] p12AsBytes;
    try {
      p12AsBytes = Files.readAllBytes(Path.of(PATH_TO_TSP_EGK_OSIG_P12));
      tspEntrySigner = KeyStore.getInstance("pkcs12", new BouncyCastleProvider());
      tspEntrySigner.load(new ByteArrayInputStream(p12AsBytes), "00".toCharArray());
    } catch (final IOException
        | NoSuchAlgorithmException
        | CertificateException
        | KeyStoreException e) {
      throw new RuntimeException("Error loading keystore at " + PATH_TO_TSP_EGK_OSIG_P12, e);
    }
  }

  @Angenommen("der TSP sendet den signierten eContent an den PoPP Service")
  public void sendSignedEContent() {
    final byte[] eContentPayload =
        signEntryForHashDb(
            "4D516D65BA306BC9AF747E03BE24D57E905A70D0CF5BBD97783E6CA77AF201EE",
            "19AAF229FDDA443B6B39AA349E621E007A851FDBE92F8235DC4C6A80F68237C8",
            "import");
    executeCommandWithContingentWait(
        () -> givenDefaultSpec().body(eContentPayload).request(Method.POST, URL_HASH_DB_IMPORT_RU));
  }

  @Wenn("der TSP fragt den Status seines Imports ab")
  public void sendStatusRequest() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(".*/api/v1/hash-db/import").build().resolvePlaceholders());
    final String jobId =
        this.rbelMessageRetriever
            .findElementInCurrentResponse("$.body.jobId")
            .getRawStringContent();

    executeCommandWithContingentWait(
        () ->
            givenDefaultSpec()
                .request(Method.GET, URL_HASH_DB_IMPORT_RU + "/" + jobId + "/status"));
  }

  @Und("der TSP verwendet die Client Identität {string} für die mTLS-Verbindung zum PoPP-Service")
  public void configureTlsClientIdentity(final String identityFileName) {
    final String prefixWithFileLocation = "../../no-publish/test-data/p12/popp-testsuite/";
    final String suffixWithPassword = ".p12;00";
    tigerProxyGlue.setLocalTigerProxyForwardMutualTlsIdentity(
        prefixWithFileLocation + identityFileName + suffixWithPassword);
  }

  @Dann("wird die Verbindung vom PoPP-Service abgelehnt")
  public void wirdDieVerbindungVomPoPPServiceAbgelehnt() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(".*/api/v1/hash-db/import").build().resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.responseCode",
        String.valueOf(HttpStatus.UNAUTHORIZED_401),
        true,
        this.rbelMessageRetriever);
  }

  @Dann("der TSP erhält eine positive Rückmeldung mit einer jobID")
  public void checkLastResponseForSuccess() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(".*/api/v1/hash-db/import").build().resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.responseCode", String.valueOf(HttpStatus.CREATED_201), true, this.rbelMessageRetriever);
    this.rbelValidator.assertAttributeOfCurrentResponseMatchesAs(
        "$.body",
        ModeType.JSON,
        TigerGlobalConfiguration.resolvePlaceholders(
            "!{file('" + VALID_HASH_DB_IMPORT_RESPONSE_FILE + "')}"),
        "",
        this.rbelMessageRetriever);
  }

  @Dann("der TSP erhält Informationen über den Status seines Imports")
  public void checkLastResponseForStatus() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder()
            .path(".*/api/v1/hash-db/import/.*/status")
            .build()
            .resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.responseCode", String.valueOf(HttpStatus.OK_200), true, this.rbelMessageRetriever);
    this.rbelValidator.assertAttributeOfCurrentResponseMatchesAs(
        "$.body",
        ModeType.JSON,
        TigerGlobalConfiguration.resolvePlaceholders(
            "!{file('" + VALID_HASH_DB_JOB_STATUS_RESPONSE_FILE + "')}"),
        "",
        this.rbelMessageRetriever);
  }

  @SneakyThrows
  public static byte[] signEntryForHashDb(
      final String autHashAsHexString, final String cvcHashAsHexString, final String operation) {

    final CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
    final ContentSigner contentSigner =
        new JcaContentSignerBuilder("SHA256withECDSA")
            .setProvider("BC")
            .build(
                (java.security.PrivateKey)
                    tspEntrySigner.getKey(ALIAS_FOR_TRUSTSTORE, "00".toCharArray()));
    generator.addSignerInfoGenerator(
        new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
            .build(
                contentSigner,
                (X509Certificate) tspEntrySigner.getCertificate(ALIAS_FOR_TRUSTSTORE)));
    generator.addCertificates(
        new JcaCertStore(
            Collections.singletonList(tspEntrySigner.getCertificate(ALIAS_FOR_TRUSTSTORE))));
    final EgkInfo egkInfo =
        new EgkInfo(
            operation,
            Hex.decode(autHashAsHexString),
            Hex.decode(cvcHashAsHexString),
            ARBITRARY_VALUE_NOT_AFTER_FOR_HASHDB_ENTRIES);

    final EContent eContent = new EContent(List.of(egkInfo));
    final byte[] dataEncoded = eContent.getEncoded("DER");
    final CMSProcessableByteArray cmsData = new CMSProcessableByteArray(dataEncoded);
    final CMSSignedData signedData = generator.generate(cmsData, true);
    return signedData.getEncoded();
  }
}
