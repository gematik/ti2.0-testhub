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

import static org.openapitools.codegen.utils.ModelUtils.unaliasSchema;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.utils.ModelUtils;

@Slf4j
public class StaticExampleExtractor {

  private static final String OUTPUT_DIR = "src/test/resources/expectedResponses";
  private static final String OPENAPI_FILE = "src/test/resources/vsdm-provider-contract.json";

  public static void main(String[] args) throws IOException {
    Path specPath = Paths.get(OPENAPI_FILE);
    Path outDir = Paths.get(OUTPUT_DIR);
    Files.createDirectories(outDir);

    OpenAPI openApi = loadSpec(specPath);
    Map<String, String> examplesByOpId = extractResponseExamplesByOperationId(openApi);
    for (var entry : examplesByOpId.entrySet()) {
      String exampleName = entry.getKey();
      String example = entry.getValue();
      Files.write(
          outDir.resolve(exampleName + ".json"),
          List.of(example),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    }
  }

  private static Map<String, String> extractResponseExamplesByOperationId(OpenAPI openApi) {
    Objects.requireNonNull(openApi);
    if (openApi.getPaths() == null) {
      return Map.of();
    }

    Map<String, String> out = new HashMap<>();

    openApi
        .getPaths()
        .forEach(
            (path, pathItem) -> {
              if (pathItem == null) return;

              pathItem
                  .readOperationsMap()
                  .forEach(
                      (method, operation) -> {
                        if (operation == null
                            || operation.getResponses() == null
                            || operation.getResponses().isEmpty()) return;

                        String opId =
                            operation.getOperationId() != null
                                ? operation.getOperationId()
                                : method + "_" + path;

                        for (String code : operation.getResponses().keySet()) {
                          var apiResponse =
                              "default".equals(code)
                                  ? operation.getResponses().getDefault()
                                  : operation.getResponses().get(code);

                          if (apiResponse == null
                              || apiResponse.getContent() == null
                              || apiResponse.getContent().isEmpty()) continue;

                          // Prefer application/json, else first available
                          String mediaType =
                              apiResponse.getContent().containsKey("application/json")
                                  ? "application/json"
                                  : apiResponse.getContent().keySet().iterator().next();
                          Schema schema =
                              unaliasSchema(
                                  openApi,
                                  ModelUtils.getSchemaFromResponse(openApi, apiResponse),
                                  Map.of());

                          var examples =
                              new ArrayList<Object>(
                                  extractExistingMediaTypeExample(apiResponse, mediaType));

                          // Also collect schema-level examples if present
                          examples.addAll(extractExistingSchemaExamples(schema));
                          // If nothing at schema level, we check if there is a reference
                          if (examples.isEmpty() && schema.get$ref() != null) {
                            examples.addAll(
                                extractExistingSchemaExamples(
                                    ModelUtils.getReferencedSchema(openApi, schema)));
                          }
                          if (examples.isEmpty()) {
                            // Log info when no explicit examples are found for this
                            // operation/response code
                            log.info("No example found for {} and {}", method + " " + path, code);
                          }
                          for (int i = 0; i < examples.size(); i++) {
                            out.put(opId + "_" + code + "_" + i, Json.pretty(examples.get(i)));
                          }
                        }
                      });
            });
    return out;
  }

  private static OpenAPI loadSpec(Path specPath) {
    String normalized = specPath.toAbsolutePath().toString().replace('\\', '/');

    return new OpenAPIV3Parser().read(normalized);
  }

  private static List<Object> extractExistingMediaTypeExample(
      ApiResponse response, String mediaType) {
    Objects.requireNonNull(response);
    Objects.requireNonNull(mediaType);
    var mediaTypeNode = response.getContent().get(mediaType);
    if (mediaTypeNode == null) {
      return List.of();
    }
    var examples = new ArrayList<Object>();
    if (mediaTypeNode.getExample() != null) {
      examples.add(mediaTypeNode.getExample());
    }
    if (mediaTypeNode.getExamples() != null && !mediaTypeNode.getExamples().isEmpty()) {
      examples.addAll(mediaTypeNode.getExamples().values());
    }
    return examples;
  }

  private static List<Object> extractExistingSchemaExamples(Schema schema) {
    if (schema == null) {
      return List.of();
    }
    var examples = new ArrayList<Object>();

    Object ex = schema.getExample();
    if (ex != null) {
      examples.add(ex);
    }
    List<Object> additionalExamples = schema.getExamples();
    if (additionalExamples != null) {
      additionalExamples.stream().filter(Objects::nonNull).forEach(examples::add);
    }
    return examples;
  }
}
