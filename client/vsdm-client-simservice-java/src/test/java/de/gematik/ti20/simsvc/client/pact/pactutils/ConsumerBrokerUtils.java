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
package de.gematik.ti20.simsvc.client.pact.pactutils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.client.pact.PactConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ConsumerBrokerUtils {
  private static ObjectMapper objectMapper = new ObjectMapper();
  private final PactConfig pactConfig;

  public static void main(String[] args) throws IOException {
    ConsumerBrokerUtils utils = new ConsumerBrokerUtils();
    utils.publishPact();
    // TODO: do we need to wait for the contracts to be compared?
    utils.canIDeploy("test");
  }

  public ConsumerBrokerUtils() {
    this.pactConfig = PactConfig.createConfig();
  }

  public boolean canIDeploy(String environmentName) throws IOException {
    log.info("Can I deploy?");
    var url = "%s/can-i-deploy".formatted(pactConfig.getPactBrokerUrl());

    Headers headers =
        Headers.of(
            "Accept",
            "application/hal+json",
            "Authorization",
            "Bearer %s".formatted(pactConfig.getPactBrokerApiKey()));

    HttpUrl.Builder urlBuilder =
        HttpUrl.parse(url)
            .newBuilder()
            .addQueryParameter("pacticipant", pactConfig.getPactConsumerName())
            .addQueryParameter("version", pactConfig.getConsumerVersion())
            .addQueryParameter(
                "environment", StringUtils.isNotBlank(environmentName) ? environmentName : "test");

    Request.Builder requestBuilder =
        new Request.Builder().url(urlBuilder.build()).headers(headers).get();

    OkHttpClient client = new OkHttpClient();
    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      var responseBodyString = response.body().string();
      if (!response.isSuccessful()) {

        log.error(responseBodyString);
        throw new IOException(responseBodyString);
      }

      // Parse the response into the structured format
      CanIReleaseResponse pactResponse =
          objectMapper.readValue(responseBodyString, CanIReleaseResponse.class);

      log.info(pactResponse.getSummary().reason);
      log.info(pactResponse.matrix.toString());

      // Return overall deployability based on summary
      return pactResponse.getSummary().isDeployable();

    } catch (IOException e) {
      log.error("Error processing Pact Broker response", e);
      throw e;
    }
  }

  public void publishPact() throws IOException {
    String pactFile = Files.readString(pactConfig.getPactContractPath());
    var url = "%s/contracts/publish".formatted(pactConfig.getPactBrokerUrl());
    Headers headers =
        Headers.of(
            "Content-Type",
            "application/json",
            "Accept",
            "application/hal+json",
            "Authorization",
            "Bearer %s".formatted(pactConfig.getPactBrokerApiKey()));

    var publishPactRequest =
        PublishPactRequest.builder()
            .pacticipantName(pactConfig.getPactConsumerName())
            .pacticipantVersionNumber(pactConfig.getConsumerVersion())
            .tags(List.of())
            .branch(pactConfig.getConsumerBranch())
            .buildUrl(pactConfig.getConsumerBuildUrl())
            .contracts(
                List.of(
                    Contract.builder()
                        .consumerName(pactConfig.getPactConsumerName())
                        .providerName(pactConfig.getPactProviderName())
                        .content(
                            Base64.getEncoder()
                                .encodeToString(pactFile.getBytes(StandardCharsets.UTF_8)))
                        .build()))
            .build();

    RequestBody body =
        RequestBody.create(
            objectMapper.writeValueAsString(publishPactRequest),
            MediaType.parse("application/json"));

    Request.Builder requestBuilder = new Request.Builder().url(url).headers(headers).post(body);

    OkHttpClient client = new OkHttpClient();

    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      var responseBodyString = response.body().string();
      if (!response.isSuccessful()) {

        log.error(responseBodyString);
        throw new IOException(responseBodyString);
      }
      log.info("Pact contract published");
      // Parse the response into the structured format
      log.info(responseBodyString);
      // TODO: do we need any more infor from it?
      // PublishPactResponse pactResponse =
      //        objectMapper.readValue(responseBodyString, PublishPactResponse.class);

    }
  }

  @Data
  public static class CanIReleaseResponse {
    private List<MatrixEntry> matrix;
    private List<Notice> notices;
    private Summary summary;
  }

  @Data
  public static class MatrixEntry {
    private Participant consumer;
    private Pact pact;
    private Participant provider;
    private VerificationResult verificationResult;
  }

  @Data
  public static class Participant {
    private String name;
    private ParticipantVersion version;

    @JsonProperty("_links")
    private Links links;
  }

  @Data
  public static class ParticipantVersion {
    private String number;
    private String branch;
    private List<Branch> branches;
    private List<Branch> branchVersions;
    private List<Environment> environments;
    private List<String> tags;

    @JsonProperty("_links")
    private Links links;
  }

  @Data
  public static class Branch {
    private String name;
    private boolean latest;

    @JsonProperty("_links")
    private Links links;
  }

  @Data
  public static class Environment {
    private String uuid;
    private String name;
    private String displayName;
    private boolean production;
    private String createdAt;

    @JsonProperty("_links")
    private Links links;
  }

  @Data
  public static class Pact {
    private String createdAt;

    @JsonProperty("_links")
    private Links links;
  }

  @Data
  public static class VerificationResult {
    private boolean success;
    private String verifiedAt;

    @JsonProperty("_links")
    private Links links;
  }

  @Data
  public static class Links {
    private Link self;

    @JsonProperty("pfi:ui")
    private Link pfiUi;
  }

  @Data
  public static class Link {
    private String href;
    private String name;
    private boolean templated;
    private String title;
  }

  @Data
  public static class Notice {
    private String text;
    private String type;
  }

  @Data
  public static class Summary {
    private boolean deployable;
    private int failed;
    private String reason;
    private int success;
    private int unknown;
  }

  @Data
  @Builder
  public static class PublishPactRequest {
    private String pacticipantName;
    private String pacticipantVersionNumber;
    private List<String> tags;
    private String branch;
    private String buildUrl;
    private List<Contract> contracts;
  }

  @Data
  @Builder
  public static class Contract {
    private String consumerName;
    private String providerName;
    private String content;
    @Builder.Default private String contentType = "application/json";
    @Builder.Default private String specification = "pact";
  }

  // Response data objects
  @Data
  public static class PublishPactResponse {
    private List<Notice> notices;

    @JsonProperty("_embedded")
    private Embedded embedded;

    @JsonProperty("_links")
    private PublishPactLinks links;
  }

  @Data
  public static class Embedded {
    private Pacticipant pacticipant;
    private Version version;
  }

  @Data
  public static class Pacticipant {
    private String name;

    @JsonProperty("_links")
    private Links links;
  }

  @Data
  public static class Version {
    private String number;

    @JsonProperty("_links")
    private Links links;
  }

  @Data
  public static class PublishPactLinks {
    @JsonProperty("pb:pacticipant")
    private Link pbPacticipant;

    @JsonProperty("pb:pacticipant-version")
    private Link pbPacticipantVersion;

    @JsonProperty("pb:pacticipant-version-tags")
    private List<Link> pbPacticipantVersionTags;

    @JsonProperty("pb:contracts")
    private List<Link> pbContracts;
  }
}
