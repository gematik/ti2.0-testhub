/*
 *
 * Copyright 2025-2026 gematik GmbH
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
/*-
 * #%L
 * ZETA Testsuite
 * %%
 * (C) achelos GmbH, 2025, licensed for gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.steps;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.core.TracingMessagePairFacet;
import de.gematik.rbellogger.facets.timing.RbelMessageTimingFacet;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.reports.SerenityReportUtils;
import io.cucumber.java.de.Dann;
import io.cucumber.java.en.Then;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber step glue for logging RBEL messages and reporting timing information between correlated
 * request/response pairs using RBEL tracing and timing facets.
 */
@Slf4j
public class TimingGlue {

  /**
   * Helper to extract a required facet from an {@link RbelElement} or throw a concise assertion
   * error.
   *
   * @param element the RBEL element providing the facet
   * @param type the facet class to extract
   * @param context human-readable context
   * @param <T> facet type
   * @return the facet instance
   * @throws AssertionError if the facet is not present on the element
   */
  private static <T> T requireFacet(
      @NonNull RbelElement element, @NonNull Class<T> type, @NonNull String context) {
    return element
        .getFacet(type)
        .orElseThrow(
            () -> new AssertionError("Missing " + type.getSimpleName() + " for " + context));
  }

  /**
   * Logs a summary of all RBEL messages captured in the current test run. Uses {@link
   * RbelMessageRetriever} to obtain all recorded messages and delegates to {@link
   * #logAllMessages(List)} for structured logging.
   */
  @Dann("logge alle Nachrichten")
  @Then("log all messages")
  public void logAllMessages() {
    var messages = RbelMessageRetriever.getInstance().getRbelMessages();
    logAllMessages(messages);
  }

  /**
   * Logs details for each RBEL message pair including request/response UUIDs and latency. If a
   * message does not contain a {@link TracingMessagePairFacet}, a note is logged.
   *
   * @param messages the list of RBEL messages to log; may be {@code null}
   */
  public void logAllMessages(List<RbelElement> messages) {
    if (messages == null) {
      log.info("RBEL messages recorded: 0");
      return;
    }
    log.info("RBEL messages recorded: {}", messages.size());
    IntStream.range(0, messages.size())
        .forEachOrdered(
            i -> {
              var m = messages.get(i);
              var tracingMessagePairFacet =
                  requireFacet(m, TracingMessagePairFacet.class, "message " + m.getUuid());
              log.info("[{}] Pair UUID {}", String.format("%03d", i), m.getUuid());
              this.getDurationBetweenRequestAndResponse(
                  tracingMessagePairFacet.getRequest(), tracingMessagePairFacet.getResponse());
            });
  }

  /**
   * Computes and reports the response time for the currently active request/response pair. Adds a
   * custom entry to the Serenity report and logs the measured latency.
   *
   * @throws AssertionError if no messages are available, no current request exists, or the required
   *     timing facets are missing.
   */
  @Dann("gebe die Antwortzeit vom aktuellen Nachrichtenpaar aus")
  @Then("show response time of current message pair")
  public void showResponseTimeOfCurrentMessagePair() {
    var messages = RbelMessageRetriever.getInstance().getRbelMessages();
    if (messages == null || messages.isEmpty()) {
      throw new AssertionError("No RBEL messages recorded – cannot determine response time.");
    }

    var request = RbelMessageRetriever.getInstance().getCurrentRequest();
    if (request == null) {
      throw new AssertionError("No current request available – cannot determine response time.");
    }

    var response = this.findResponseForRequest(messages, request);
    if (response == null) {
      throw new AssertionError("No matching response found for request " + request.getUuid());
    }

    var duration = this.getDurationBetweenRequestAndResponse(request, response);
    SerenityReportUtils.addCustomData(
        "Current response time", "The current response took " + duration.toMillis() + " ms.");
  }

  /**
   * Finds the response message that belongs to a given request by inspecting the {@link
   * TracingMessagePairFacet} of the provided messages.
   *
   * @param messages the list of RBEL messages to search
   * @param request the request to find a response for
   * @return the correlated response message, or {@code null} if none is found
   * @throws AssertionError if a message lacks the required tracing facet
   */
  public RbelElement findResponseForRequest(
      @NonNull List<RbelElement> messages, @NonNull RbelElement request) {
    return messages.isEmpty()
        ? null
        : messages.stream()
            .map(m -> requireFacet(m, TracingMessagePairFacet.class, "message " + m.getUuid()))
            .filter(
                tracingMessagePairFacet ->
                    tracingMessagePairFacet.getRequest().getUuid().equals(request.getUuid()))
            .findFirst()
            .map(TracingMessagePairFacet::getResponse)
            .orElse(null);
  }

  /**
   * Calculates the elapsed time between a request and its response using {@link
   * RbelMessageTimingFacet} on both elements and logs the result.
   *
   * @param request the request message; must provide a {@link RbelMessageTimingFacet}
   * @param response the response message; must provide a {@link RbelMessageTimingFacet}
   * @return the computed {@link Duration} between request and response transmission times
   * @throws AssertionError if either element lacks the required timing facet
   */
  private Duration getDurationBetweenRequestAndResponse(
      @NonNull RbelElement request, @NonNull RbelElement response) {
    var requestTiming =
        requireFacet(request, RbelMessageTimingFacet.class, "request " + request.getUuid());
    var responseTiming =
        requireFacet(response, RbelMessageTimingFacet.class, "response " + response.getUuid());

    var duration =
        Duration.between(requestTiming.getTransmissionTime(), responseTiming.getTransmissionTime());

    log.info(
        "Duration between request with UUID: {} and response with UUID: {} "
            + "taken from Tiger Message logs is {} ms",
        request.getUuid(),
        response.getUuid(),
        duration.toMillis());

    return duration;
  }
}
