/*-
 * #%L
 * RBEL Fluent API
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
package de.gematik.ti20.rbel.fluent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.core.RbelMismatchNoteFacet;
import de.gematik.test.tiger.lib.exception.ValidatorAssertionError;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.rbel.RbelValidator;
import de.gematik.test.tiger.lib.rbel.RequestParameter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbelFluentApiMockitoTest {
  @Mock private RbelMessageRetriever retriever;
  @Mock private RbelValidator validator;
  private static final String VALID_JWT = "eyJhbGciOiJSUzI1NiJ9.eyJmb28iOiJiYXIifQ.c2ln";

  @Test
  void shouldNavigateRequestsAndResponsesFluently() {
    final var components = RbelFluentApi.RbelComponents.of(retriever, validator);
    final var response = mock(RbelElement.class);
    final var jwtElement = mock(RbelElement.class);
    when(retriever.getCurrentResponse()).thenReturn(response);
    when(retriever.findElementInCurrentResponse("$.body.token")).thenReturn(jwtElement);
    when(jwtElement.getRawStringContent()).thenReturn(VALID_JWT);
    when(response.findRbelPathMembers("$.responseCode"))
        .thenReturn(List.of(new RbelElement("200".getBytes(StandardCharsets.UTF_8), null)));

    RbelFluentApi.expectRequests(".*/token", components)
        .nextResponse()
        .hasStringContentEqualToAtPosition("$.responseCode", "200")
        .nextResponse()
        .hasStringContentEqualToAtPosition("$.responseCode", "200");

    final var parameterCaptor = ArgumentCaptor.forClass(RequestParameter.class);
    verify(retriever, times(2)).filterRequestsAndStoreInContext(parameterCaptor.capture());

    final List<RequestParameter> captured = parameterCaptor.getAllValues();
    assertEquals(2, captured.size());
    assertEquals(".*/token", captured.getFirst().getPath());
    assertFalse(captured.getFirst().isStartFromPreviouslyFoundMessage());
    assertFalse(captured.getFirst().isFilterPreviousRequest());
    assertEquals(".*/token", captured.get(1).getPath());
    assertTrue(captured.get(1).isStartFromPreviouslyFoundMessage());
    assertTrue(captured.get(1).isFilterPreviousRequest());
    verify(retriever, times(2)).getCurrentResponse();

    RbelFluentApi.assertJwt(VALID_JWT).matchesSchema("jwt-test-schema.yaml");
    RbelFluentApi.assertCurrentResponse(components)
        .childAtPath("$.body.token")
        .asJwt()
        .matchesSchema("jwt-test-schema.yaml");
  }

  @Test
  void shouldExplainAvailableCandidatesWhenNoMatchingRequestIsFound() {
    final var components = RbelFluentApi.RbelComponents.of(retriever, validator);
    final var firstCandidate = mock(RbelElement.class);
    final var secondCandidate = mock(RbelElement.class);
    final SortedSet<RbelMismatchNoteFacet> noNotes = new TreeSet<>();
    when(firstCandidate.printShortDescription()).thenReturn("first candidate");
    when(secondCandidate.printShortDescription()).thenReturn("second candidate");
    doThrow(
            new ValidatorAssertionError(
                "Requested message not found",
                Map.of(firstCandidate, noNotes, secondCandidate, noNotes)))
        .when(retriever)
        .filterRequestsAndStoreInContext(any());

    final var error =
        assertThrows(
            AssertionError.class,
            () -> RbelFluentApi.expectRequests(".*/missing", components).nextResponse());

    assertEquals(
        "Requested message not found\nfirst candidate\nsecond candidate", error.getMessage());
  }

  @Test
  void shouldSupportMatchingAndTypedValueConvenienceMethods() {
    final var components = RbelFluentApi.RbelComponents.of(retriever, validator);
    final var response = mock(RbelElement.class);
    final var child = mock(RbelElement.class);
    final var listChild1 = mock(RbelElement.class);
    final var listChild2 = mock(RbelElement.class);

    when(retriever.getCurrentResponse()).thenReturn(response);
    when(retriever.findElementInCurrentResponse("$.status")).thenReturn(child);
    when(retriever.findElementsInCurrentResponseOrEmpty("$.items"))
        .thenReturn(List.of(listChild1, listChild2));
    when(child.getRawStringContent()).thenReturn("200");
    when(listChild1.getRawStringContent()).thenReturn("first");
    when(listChild2.getRawStringContent()).thenReturn("second");

    RbelFluentApi.assertCurrentResponse(components)
        .hasValueAtPathMatchingAnyOf("$.status", "201", "200")
        .childAtPath("$.status")
        .satisfies(value -> assertEquals("200", value))
        .mapTo(Integer::parseInt)
        .isEqualTo(200);

    assertEquals(
        List.of("first", "second"),
        RbelFluentApi.assertCurrentResponse(components).readValuesAtPath("$.items"));
    verify(validator, never())
        .assertAttributeOfCurrentResponseMatchesAs(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.eq(retriever));
  }

  @Test
  void shouldSupportInstantAndFileBasedConvenienceMethods(@TempDir Path tempDir)
      throws IOException {
    final var components = RbelFluentApi.RbelComponents.of(retriever, validator);
    final var response = mock(RbelElement.class);
    final var epoch = String.valueOf(Instant.now().getEpochSecond() - 5);
    final var json = "{\"hello\":\"world\"}";
    final var valueFile = tempDir.resolve("value.txt");
    final var jsonFile = tempDir.resolve("body.json");
    final var valueElement = new RbelElement("file-value".getBytes(StandardCharsets.UTF_8), null);

    Files.writeString(valueFile, "file-value");
    Files.writeString(jsonFile, json);

    when(retriever.getCurrentResponse()).thenReturn(response);
    when(retriever.findElementInCurrentResponse("$.iat"))
        .thenReturn(new RbelElement(epoch.getBytes(StandardCharsets.UTF_8), null));
    when(response.findRbelPathMembers("$.value")).thenReturn(List.of(valueElement));

    RbelFluentApi.assertCurrentResponse(components)
        .hasEpochSecondsAtPathAfterNowMinusSeconds("$.iat", 10)
        .hasEpochSecondsAtPathBeforeNowPlusSeconds("$.iat", 10)
        .hasValueAtPathEqualToFile("$.value", valueFile.toString())
        .hasJsonAtPathEqualToFile("$.body", jsonFile.toString());
  }

  @Test
  void shouldSupportPrintTreeAlias() {
    final var components = RbelFluentApi.RbelComponents.of(retriever, validator);
    final var response = mock(RbelElement.class);
    when(retriever.getCurrentResponse()).thenReturn(response);
    when(response.findRbelPathMembers("$.responseCode"))
        .thenReturn(List.of(new RbelElement("200".getBytes(StandardCharsets.UTF_8), null)));

    RbelFluentApi.assertCurrentResponse(components)
        .printRbelTree()
        .hasValueAtPathEqualTo("$.responseCode", "200");
  }
}
