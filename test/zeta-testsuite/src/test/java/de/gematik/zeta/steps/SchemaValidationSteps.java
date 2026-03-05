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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.*;
import com.nimbusds.jwt.SignedJWT;
import io.cucumber.java.de.Dann;
import io.cucumber.java.en.Then;
import java.text.ParseException;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

/**
 * Step definitions for validating JSON instances against JSON/YAML schemas using the networknt JSON
 * Schema validator.
 */
@Slf4j
public class SchemaValidationSteps {

  private static final ObjectMapper JSON = new ObjectMapper();

  /**
   * Parses a JSON string into a JsonNode.
   *
   * @param jsonString the JSON string to parse
   * @return parsed JsonNode
   * @throws AssertionError if parsing fails
   */
  private static JsonNode parseJsonString(String jsonString) {
    if (jsonString == null || jsonString.isBlank()) {
      throw new AssertionError("JSON string is null or empty");
    }
    try {
      return JSON.readTree(jsonString);
    } catch (JsonProcessingException e) {
      throw new AssertionError("Failed to parse JSON: " + e.getMessage(), e);
    }
  }

  /**
   * Shared registry for loading schemas using the new networknt 2.x API with a Draft-7 default
   * (used when a schema does not provide $schema).
   */
  private static final SchemaRegistry SCHEMA_REGISTRY =
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);

  /**
   * Loads a YAML schema file from the classpath (resources directory).
   *
   * @param schemaName name or relative path of the schema on the classpath
   * @return {@link Schema} configured with the schema's base location
   */
  private Schema loadYamlSchema(String schemaName) {
    var normalizedPath = schemaName.startsWith("/") ? schemaName.substring(1) : schemaName;
    if (!normalizedPath.startsWith("schemas/")) {
      normalizedPath = "schemas/v_1_0/" + normalizedPath;
    }

    var resource = SchemaValidationSteps.class.getClassLoader().getResource(normalizedPath);
    if (resource == null) {
      throw new AssertionError("Schema not found on the classpath: " + normalizedPath);
    }

    var location = SchemaLocation.of("classpath:" + normalizedPath);
    return SCHEMA_REGISTRY.getSchema(location);
  }

  /**
   * Validates a JSON string against a given {@link Schema}.
   *
   * <p>With the soft option enabled, errors will be logged only and no exception is thrown.
   *
   * <p>If the JSON is empty, cannot be parsed, or does not match the schema, an {@link
   * AssertionError} is thrown with a detailed error message.
   *
   * @param schema the JSON Schema to validate against
   * @param jsonNode the JSON string to validate
   * @param schemaPath identifier or path of the schema (used for error reporting)
   * @param soft if true, errors will only be logged.
   * @throws AssertionError if validation fails
   */
  private void assertValid(Schema schema, JsonNode jsonNode, String schemaPath, boolean soft) {

    try {
      var errors = schema.validate(jsonNode);
      if (!errors.isEmpty()) {
        var sb =
            new StringBuilder(
                "Validation against "
                    + schemaPath
                    + " failed with "
                    + errors.size()
                    + " errors:\n");
        errors.stream()
            .sorted(Comparator.comparing(com.networknt.schema.Error::getMessage))
            .forEach(
                e -> {
                  sb.append(" - ").append(e.getMessage());
                  var path = e.getEvaluationPath();
                  if (path != null) {
                    sb.append(" [path: ").append(path).append("]");
                  }
                  sb.append("\n");
                });
        throw new AssertionError(sb.toString());
      } else {
        // Im Gutfall ist es egal, ob soft oder interrupting geprüft wird
        log.info("Validation passed for schema {}", schemaPath);
      }
    } catch (AssertionError | RuntimeException ex) {
      if (soft) {
        log.warn("Soft validation failed for schema {}: {}", schemaPath, ex.getMessage());
        SoftAssertionsContext.recordSoftFailure("Schema validation (soft) for " + schemaPath, ex);
      } else {
        throw ex;
      }
    }
  }

  /**
   * Cucumber step definition for validating a JSON string against a schema loaded from the
   * resources directory.
   *
   * @param jsonString the JSON string to validate
   * @param schemaPath relative path of the schema under {@code resources}
   */
  @Dann("validiere {tigerResolvedString} gegen Schema {string}")
  @Then("validate {tigerResolvedString} against schema {string}")
  public void validateJsonAgainstYamlSchema(String jsonString, String schemaPath) {
    var schema = loadYamlSchema(schemaPath);
    JsonNode jsonNode = parseJsonString(jsonString);
    assertValid(schema, jsonNode, schemaPath, false);
  }

  /**
   * Soft-asserting variant of the schema validation that collects failures until the end of the
   * scenario instead of aborting immediately.
   *
   * @param jsonString the JSON string to validate
   * @param schemaPath relative path of the schema under {@code resources}
   */
  @Dann("validiere {tigerResolvedString} soft gegen Schema {string}")
  @Then("soft-validate {tigerResolvedString} against schema {string}")
  public void softlyValidateJsonAgainstYamlSchema(String jsonString, String schemaPath) {

    var schema = loadYamlSchema(schemaPath);
    JsonNode jsonNode = parseJsonString(jsonString);
    assertValid(schema, jsonNode, schemaPath, true);
  }

  /**
   * Soft-asserting variant of the schema validation of a Base64 coded JSON string.
   *
   * @param encodedJwt the Base64 coded JWT to be validated
   * @param schemaName relative path of the schema under {@code resources}
   */
  @Dann("decodiere und validiere {tigerResolvedString} gegen Schema {string} (soft assert)")
  @Then("decode and validate {tigerResolvedString} against schema {string} (soft assert)")
  public void softlyValidateEncodedJwtAgainstYamlSchema(String encodedJwt, String schemaName) {

    var schema = loadYamlSchema(schemaName);
    var jsonNode = decodeJwt(encodedJwt);
    assertValid(schema, jsonNode, schemaName, true);
  }

  /**
   * Cucumber step definition for validating a Base64 coded JSON string against a schema loaded from
   * the resources directory.
   *
   * <p>The encoded token is expected to consist of at least two parts separated by dots:
   *
   * <ol>
   *   <li>header
   *   <li>payload
   *   <li>optional: signature
   * </ol>
   *
   * @param encodedJwt the Base64 coded JWT to be validated
   * @param schemaName relative path of the schema under {@code resources}
   */
  @Dann("decodiere und validiere {tigerResolvedString} gegen Schema {string}")
  @Then("decode and validate {tigerResolvedString} against schema {string}")
  public void validateEncodedJwtAgainstYamlSchema(String encodedJwt, String schemaName) {
    var schema = loadYamlSchema(schemaName);
    var jsonNode = decodeJwt(encodedJwt);
    assertValid(schema, jsonNode, schemaName, false);
  }

  /**
   * Decodes a Base64URL encoded JWT.
   *
   * @param encodedToken the Base64URL coded JWT to be validated
   * @return the decoded json string
   */
  private ObjectNode decodeJwt(String encodedToken) {

    try {
      SignedJWT signedJwt = SignedJWT.parse(encodedToken);

      ObjectNode jsNode = JSON.createObjectNode();
      jsNode.set("header", JSON.valueToTree(signedJwt.getHeader().toJSONObject()));
      jsNode.set("payload", JSON.readTree(signedJwt.getPayload().toString()));

      return jsNode;

    } catch (ParseException | JsonProcessingException e) {
      throw new AssertionError("signed JWT could not be parsed.");
    }
  }
}
