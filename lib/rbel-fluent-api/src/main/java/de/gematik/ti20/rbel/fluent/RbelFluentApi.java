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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.nimbusds.jwt.SignedJWT;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.testutil.RbelElementAssertion;
import de.gematik.test.tiger.common.config.ConfigurationValuePrecedence;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.lib.exception.ValidatorAssertionError;
import de.gematik.test.tiger.lib.rbel.ModeType;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.rbel.RbelValidator;
import de.gematik.test.tiger.lib.rbel.RequestParameter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.AbstractComparableAssert;
import org.assertj.core.api.AbstractInstantAssert;
import org.assertj.core.api.Assertions;

public final class RbelFluentApi {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final SchemaRegistry SCHEMA_REGISTRY =
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);
  private static final AtomicReference<RbelComponents> DEFAULT_COMPONENTS_FOR_TESTING =
      new AtomicReference<>();
  private final RbelComponents components;

  private RbelFluentApi(final RbelComponents components) {
    this.components = Objects.requireNonNull(components);
  }

  /**
   * Finds the first request matching {@code pathRegex} (chronologically earliest match in the
   * recorded traffic). Use {@link #expectLastRequest(String)} to instead pick the most recent
   * match.
   */
  public static RequestAssertion expectRequests(final String pathRegex) {
    return expectRequests(pathRegex, defaultComponents());
  }

  public static RequestAssertion expectRequests(
      final String pathRegex, final RbelComponents components) {
    return findRequest(components, builder -> builder.path(pathRegex));
  }

  /**
   * Finds the last (most recent) request matching {@code pathRegex}, instead of the default "first
   * matching message" behaviour of {@link #expectRequests(String)}.
   */
  public static RequestAssertion expectLastRequest(final String pathRegex) {
    return expectLastRequest(pathRegex, defaultComponents());
  }

  public static RequestAssertion expectLastRequest(
      final String pathRegex, final RbelComponents components) {
    return findRequest(components, builder -> builder.path(pathRegex).filterPreviousRequest(true));
  }

  public static RequestAssertion expectRequestsWithNodeMatching(
      final String rbelPath, final String valuePattern) {
    return expectRequestsWithNodeMatching(rbelPath, valuePattern, defaultComponents());
  }

  public static RequestAssertion expectRequestsWithNodeMatching(
      final String rbelPath, final String valuePattern, final RbelComponents components) {
    return findRequest(
        components,
        builder -> builder.rbelPath(rbelPath).value(valuePattern).requireRequestMessage(false));
  }

  /**
   * Same as {@link #expectRequestsWithNodeMatching(String, String)}, but picks the last (most
   * recent) matching message instead of the first.
   */
  public static RequestAssertion expectLastRequestWithNodeMatching(
      final String rbelPath, final String valuePattern) {
    return expectLastRequestWithNodeMatching(rbelPath, valuePattern, defaultComponents());
  }

  public static RequestAssertion expectLastRequestWithNodeMatching(
      final String rbelPath, final String valuePattern, final RbelComponents components) {
    return findRequest(
        components,
        builder ->
            builder
                .rbelPath(rbelPath)
                .value(valuePattern)
                .requireRequestMessage(false)
                .filterPreviousRequest(true));
  }

  private static RequestAssertion findRequest(
      final RbelComponents components,
      final Consumer<RequestParameter.RequestParameterBuilder> parameterConfigurer) {
    return new RequestSelector(components, parameterConfigurer).find();
  }

  public static RbelFluentApi using(final RbelComponents components) {
    return new RbelFluentApi(components);
  }

  public static void useDefaultComponentsForTesting(final RbelComponents components) {
    DEFAULT_COMPONENTS_FOR_TESTING.set(Objects.requireNonNull(components));
  }

  public static void clearDefaultComponentsForTesting() {
    DEFAULT_COMPONENTS_FOR_TESTING.set(null);
  }

  private static RbelComponents defaultComponents() {
    final var override = DEFAULT_COMPONENTS_FOR_TESTING.get();
    return override == null ? RbelComponents.defaults() : override;
  }

  public static ResponseAssertion assertCurrentResponse() {
    return assertCurrentResponse(defaultComponents());
  }

  public static ResponseAssertion assertCurrentResponse(final RbelComponents components) {
    return new ResponseAssertion(components.retriever().getCurrentResponse(), null, components);
  }

  public static RequestAssertion assertCurrentRequest() {
    return assertCurrentRequest(defaultComponents());
  }

  public static RequestAssertion assertCurrentRequest(final RbelComponents components) {
    return new RequestAssertion(components.retriever().getCurrentRequest(), null, components);
  }

  public static JwtAssertion assertJwt(final String encodedJwt) {
    return new JwtAssertion(TigerGlobalConfiguration.resolvePlaceholders(encodedJwt));
  }

  public interface RbelComponents {
    static RbelComponents defaults() {
      final var retriever = RbelMessageRetriever.getInstance();
      return of(retriever, new RbelValidator());
    }

    static RbelComponents of(final RbelMessageRetriever retriever, final RbelValidator validator) {
      Objects.requireNonNull(retriever);
      Objects.requireNonNull(validator);
      return new RbelComponents() {
        @Override
        public RbelMessageRetriever retriever() {
          return retriever;
        }

        @Override
        public RbelValidator validator() {
          return validator;
        }
      };
    }

    RbelMessageRetriever retriever();

    RbelValidator validator();
  }

  /**
   * Shared assertion vocabulary for {@link RequestAssertion} and {@link ResponseAssertion}. Each
   * subclass only supplies where to look ("current request" vs. "current response"); everything
   * else - path assertions, JSON comparisons, epoch checks, tree printing - is implemented once
   * here.
   */
  public abstract static class AbstractFluentAssertion<SELF extends AbstractFluentAssertion<SELF>>
      extends RbelElementAssertion {
    final RbelComponents components;

    AbstractFluentAssertion(final RbelElement actual, final RbelComponents components) {
      super(actual);
      this.components = Objects.requireNonNull(components);
    }

    protected abstract SELF self();

    protected abstract RbelElement findElement(String rbelPath);

    protected abstract List<RbelElement> findElementsOrEmpty(String rbelPath);

    protected abstract void assertJsonAtPath(String rbelPath, ModeType mode, String expectedJson);

    @Override
    public SELF andPrintTree() {
      super.andPrintTree();
      return self();
    }

    public SELF printRbelTree() {
      return andPrintTree();
    }

    @Override
    public SELF hasStringContentEqualToAtPosition(final String rbelPath, final String expected) {
      super.hasStringContentEqualToAtPosition(rbelPath, expected);
      return self();
    }

    public SELF hasValueAtPathEqualTo(final String rbelPath, final String expected) {
      return hasStringContentEqualToAtPosition(rbelPath, expected);
    }

    public SELF hasValueAtPathMatching(final String rbelPath, final String regex) {
      final var actual = readValueAtPath(rbelPath);
      if (!actual.matches(regex)) {
        failWithMessage(
            "Expected value at path <%s> to match regex <%s> but was <%s>",
            rbelPath, regex, actual);
      }
      return self();
    }

    public SELF hasValueAtPathMatchingAnyOf(final String rbelPath, final String... expectedValues) {
      final var actual = readValueAtPath(rbelPath);
      for (final var expectedValue : expectedValues) {
        if (actual.matches(expectedValue)) {
          return self();
        }
      }
      failWithMessage(
          "Expected value at path <%s> to match any of <%s> but was <%s>",
          rbelPath, String.join(", ", expectedValues), actual);
      return self();
    }

    public ChildAssertion<SELF> childAtPath(final String rbelPath) {
      return new ChildAssertion<>(self(), readValueAtPath(rbelPath));
    }

    public RbelElement elementAtPath(final String rbelPath) {
      return readElementAtPath(rbelPath);
    }

    public String readValueAtPath(final String rbelPath) {
      final var rawStringContent = readElementAtPath(rbelPath).getRawStringContent();
      if (rawStringContent == null) {
        failWithMessage("Expected raw string content at path <%s> to not be null", rbelPath);
      }
      return rawStringContent;
    }

    private RbelElement readElementAtPath(final String rbelPath) {
      final var element = findElement(rbelPath);
      if (element == null) {
        failWithMessage("Expected element at path <%s> to exist", rbelPath);
      }
      return element;
    }

    public List<String> readValuesAtPath(final String rbelPath) {
      final var elements = findElementsOrEmpty(rbelPath);
      if (elements.isEmpty()) {
        failWithMessage("Expected at least one element at path <%s>", rbelPath);
      }
      final var values = new ArrayList<String>(elements.size());
      for (final var element : elements) {
        final var rawStringContent = element.getRawStringContent();
        if (rawStringContent == null) {
          failWithMessage("Expected raw string content at path <%s> to not be null", rbelPath);
        }
        values.add(rawStringContent);
      }
      return values;
    }

    /**
     * Reads the value at {@code rbelPath} and stores it as a Tiger global variable named {@code
     * key}, using {@link ConfigurationValuePrecedence#TEST_CONTEXT}. Equivalent to calling {@link
     * TigerGlobalConfiguration#putValue(String, Object, ConfigurationValuePrecedence)} with the
     * value read via {@link #readValueAtPath(String)}.
     */
    public SELF storeValueAtPathAs(final String rbelPath, final String key) {
      return storeValueAtPathAs(rbelPath, key, ConfigurationValuePrecedence.TEST_CONTEXT);
    }

    public SELF storeValueAtPathAs(
        final String rbelPath, final String key, final ConfigurationValuePrecedence precedence) {
      TigerGlobalConfiguration.putValue(key, readValueAtPath(rbelPath), precedence);
      return self();
    }

    public SELF hasJsonAtPathEqualTo(final String rbelPath, final String expectedJson) {
      assertJsonAtPath(rbelPath, ModeType.JSON, expectedJson);
      return self();
    }

    public SELF hasValueAtPathEqualToFile(final String rbelPath, final String filePath) {
      return hasValueAtPathEqualTo(rbelPath, readFileContent(filePath));
    }

    public SELF hasJsonAtPathEqualToFile(final String rbelPath, final String filePath) {
      return hasJsonAtPathEqualTo(rbelPath, readFileContent(filePath));
    }

    private String readFileContent(final String filePath) {
      final Path normalizedPath = TigerGlobalConfiguration.resolveRelativePathToTigerYaml(filePath);
      try {
        return TigerGlobalConfiguration.resolvePlaceholders(Files.readString(normalizedPath));
      } catch (final IOException e) {
        throw new IllegalStateException("Unable to read file content from: " + normalizedPath, e);
      }
    }

    public SELF hasEpochSecondsAtPathAfterNowMinusSeconds(
        final String rbelPath, final long seconds) {
      final var actual = Instant.ofEpochSecond(epochSecondsAtPath(rbelPath));
      final var threshold = Instant.now().minusSeconds(seconds);
      if (!actual.isAfter(threshold)) {
        failWithMessage(
            "Expected epoch at path <%s> to be after <%s> but was <%s>",
            rbelPath, threshold, actual);
      }
      return self();
    }

    public SELF hasEpochSecondsAtPathBeforeNowPlusSeconds(
        final String rbelPath, final long seconds) {
      final var actual = Instant.ofEpochSecond(epochSecondsAtPath(rbelPath));
      final var threshold = Instant.now().plusSeconds(seconds);
      if (!actual.isBefore(threshold)) {
        failWithMessage(
            "Expected epoch at path <%s> to be before <%s> but was <%s>",
            rbelPath, threshold, actual);
      }
      return self();
    }

    private long epochSecondsAtPath(final String rbelPath) {
      final var element = findElement(rbelPath);
      if (element == null) {
        failWithMessage("Expected element at path <%s> to exist", rbelPath);
      }
      final var rawStringContent = element.getRawStringContent();
      if (rawStringContent == null) {
        failWithMessage("Expected raw string content at path <%s> to not be null", rbelPath);
      }
      try {
        return Long.parseLong(rawStringContent);
      } catch (final NumberFormatException e) {
        failWithMessage(
            "Expected numeric epoch seconds at path <%s> but was <%s>", rbelPath, rawStringContent);
        return 0L;
      }
    }
  }

  /**
   * Finds the request/response pair matching the configured search parameters. Not exposed to
   * callers directly - each successful find is immediately wrapped in a {@link RequestAssertion}
   * (or, for subsequent pairs, a {@link ResponseAssertion}).
   */
  private static final class RequestSelector {
    private final RbelComponents components;
    private final Consumer<RequestParameter.RequestParameterBuilder> parameterConfigurer;
    private int consumedRequests = 0;

    private RequestSelector(
        final RbelComponents components,
        final Consumer<RequestParameter.RequestParameterBuilder> parameterConfigurer) {
      this.components = Objects.requireNonNull(components);
      this.parameterConfigurer = Objects.requireNonNull(parameterConfigurer);
    }

    RequestAssertion find() {
      search();
      return new RequestAssertion(components.retriever().getCurrentRequest(), this, components);
    }

    ResponseAssertion advanceToNextResponse() {
      search();
      return new ResponseAssertion(components.retriever().getCurrentResponse(), this, components);
    }

    private void search() {
      final var requestParameterBuilder = RequestParameter.builder();
      parameterConfigurer.accept(requestParameterBuilder);
      if (consumedRequests > 0) {
        requestParameterBuilder.startFromPreviouslyFoundMessage(true).filterPreviousRequest(true);
      }
      try {
        components
            .retriever()
            .filterRequestsAndStoreInContext(requestParameterBuilder.build().resolvePlaceholders());
      } catch (final ValidatorAssertionError e) {
        throw requestedMessageNotFound(e);
      }
      consumedRequests++;
    }

    private AssertionError requestedMessageNotFound(final ValidatorAssertionError error) {
      final var candidates =
          error.getMismatchNotes().keySet().stream()
              .map(RbelElement::printShortDescription)
              .sorted()
              .collect(Collectors.joining(System.lineSeparator()));
      final var message =
          candidates.isBlank()
              ? error.getMessage()
              : error.getMessage() + System.lineSeparator() + candidates;
      return new AssertionError(message, error);
    }
  }

  public static final class RequestAssertion extends AbstractFluentAssertion<RequestAssertion> {
    private final RequestSelector selector;

    private RequestAssertion(
        final RbelElement actual, final RequestSelector selector, final RbelComponents components) {
      super(actual, components);
      this.selector = selector;
    }

    @Override
    protected RequestAssertion self() {
      return this;
    }

    @Override
    protected RbelElement findElement(final String rbelPath) {
      return components.retriever().findElementInCurrentRequest(rbelPath);
    }

    @Override
    protected List<RbelElement> findElementsOrEmpty(final String rbelPath) {
      return components.retriever().findElementsInCurrentRequestOrEmpty(rbelPath);
    }

    @Override
    protected void assertJsonAtPath(
        final String rbelPath, final ModeType mode, final String expectedJson) {
      components
          .validator()
          .assertAttributeOfCurrentRequestMatchesAs(
              rbelPath, mode, expectedJson, components.retriever());
    }

    public ResponseAssertion nextResponse() {
      return new ResponseAssertion(
          components.retriever().getCurrentResponse(), selector, components);
    }

    public RequestAssertion nextResponse(final Consumer<ResponseAssertion> assertion) {
      Objects.requireNonNull(assertion).accept(nextResponse());
      return this;
    }
  }

  public static final class ResponseAssertion extends AbstractFluentAssertion<ResponseAssertion> {
    private final RequestSelector selector;

    private ResponseAssertion(
        final RbelElement actual, final RequestSelector selector, final RbelComponents components) {
      super(actual, components);
      this.selector = selector;
    }

    @Override
    protected ResponseAssertion self() {
      return this;
    }

    @Override
    protected RbelElement findElement(final String rbelPath) {
      return components.retriever().findElementInCurrentResponse(rbelPath);
    }

    @Override
    protected List<RbelElement> findElementsOrEmpty(final String rbelPath) {
      return components.retriever().findElementsInCurrentResponseOrEmpty(rbelPath);
    }

    @Override
    protected void assertJsonAtPath(
        final String rbelPath, final ModeType mode, final String expectedJson) {
      components
          .validator()
          .assertAttributeOfCurrentResponseMatchesAs(
              rbelPath, mode, expectedJson, "", components.retriever());
    }

    public ResponseAssertion nextResponse() {
      if (selector == null) {
        throw new IllegalStateException(
            "No parent request expectation available for nextResponse().");
      }
      return selector.advanceToNextResponse();
    }

    @Deprecated(forRemoval = false)
    public ResponseAssertion matchesAt(final String rbelPath, final String expected) {
      return hasValueAtPathEqualTo(rbelPath, expected);
    }

    @Deprecated(forRemoval = false)
    public ResponseAssertion matchesJsonAt(final String rbelPath, final String expectedJson) {
      return hasJsonAtPathEqualTo(rbelPath, expectedJson);
    }

    @Deprecated(forRemoval = false)
    public ResponseAssertion matchesAtFile(final String rbelPath, final String filePath) {
      return hasValueAtPathEqualToFile(rbelPath, filePath);
    }

    @Deprecated(forRemoval = false)
    public ResponseAssertion matchesJsonAtFile(final String rbelPath, final String filePath) {
      return hasJsonAtPathEqualToFile(rbelPath, filePath);
    }
  }

  @RequiredArgsConstructor
  public static final class ChildAssertion<P extends AbstractFluentAssertion<P>> {
    private final P parent;
    private final String value;

    public <T extends Comparable<? super T>> AbstractComparableAssert<?, T> mapTo(
        final Function<String, T> mapper) {
      return Assertions.assertThat(mapper.apply(this.value));
    }

    public AbstractInstantAssert<?> mapToInstant() {
      return Assertions.assertThat(Instant.ofEpochSecond(Long.parseLong(this.value)));
    }

    public ChildAssertion<P> satisfies(final Consumer<String> verification) {
      verification.accept(this.value);
      return this;
    }

    public JwtAssertion asJwt() {
      return new JwtAssertion(this.value);
    }

    public P and() {
      if (this.parent == null) {
        throw new IllegalStateException("No parent assertion available.");
      }
      return this.parent;
    }

    public String rawValue() {
      return this.value;
    }
  }

  public static final class JwtAssertion {
    private final String encodedJwt;

    private JwtAssertion(final String encodedJwt) {
      if (encodedJwt == null || encodedJwt.isBlank()) {
        throw new AssertionError("JWT is null or empty");
      }
      this.encodedJwt = encodedJwt;
    }

    public JwtAssertion matchesSchema(final String schemaName) {
      final var schema = loadYamlSchema(schemaName);
      assertValid(schema, decodeJwt(this.encodedJwt), schemaName);
      return this;
    }

    private static Schema loadYamlSchema(final String schemaName) {
      var normalizedPath = TigerGlobalConfiguration.resolvePlaceholders(schemaName);
      if (normalizedPath.startsWith("/")) {
        normalizedPath = normalizedPath.substring(1);
      }
      if (!normalizedPath.startsWith("schemas/")) {
        normalizedPath = "schemas/v_1_0/" + normalizedPath;
      }

      final var resource = RbelFluentApi.class.getClassLoader().getResource(normalizedPath);
      if (resource == null) {
        throw new AssertionError("Schema not found on the classpath: " + normalizedPath);
      }

      return SCHEMA_REGISTRY.getSchema(SchemaLocation.of("classpath:" + normalizedPath));
    }

    private static void assertValid(
        final Schema schema, final JsonNode jsonNode, final String schemaName) {
      final var errors = schema.validate(jsonNode.toString(), InputFormat.JSON);
      if (!errors.isEmpty()) {
        final var sb =
            new StringBuilder(
                "Validation against "
                    + schemaName
                    + " failed with "
                    + errors.size()
                    + " errors:\n");
        errors.stream()
            .sorted(java.util.Comparator.comparing(com.networknt.schema.Error::getMessage))
            .forEach(
                error -> {
                  sb.append(" - ").append(error.getMessage());
                  final var path = error.getEvaluationPath();
                  if (path != null) {
                    sb.append(" [path: ").append(path).append("]");
                  }
                  sb.append('\n');
                });
        throw new AssertionError(sb.toString());
      }
    }

    private static ObjectNode decodeJwt(final String encodedToken) {
      try {
        final SignedJWT signedJwt = SignedJWT.parse(encodedToken);
        final ObjectNode jsNode = JSON.createObjectNode();
        jsNode.set("header", JSON.valueToTree(signedJwt.getHeader().toJSONObject()));
        jsNode.set("payload", JSON.readTree(signedJwt.getPayload().toString()));
        return jsNode;
      } catch (final ParseException | JsonProcessingException e) {
        throw new AssertionError("signed JWT could not be parsed.", e);
      }
    }
  }
}
