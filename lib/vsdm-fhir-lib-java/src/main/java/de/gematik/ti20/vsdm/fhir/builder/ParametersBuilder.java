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
package de.gematik.ti20.vsdm.fhir.builder;

import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.bbriccs.fhir.coding.WithStructureDefinition;
import java.util.HashMap;
import java.util.Map;
import lombok.val;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

public final class ParametersBuilder extends ResourceBuilder<Parameters, ParametersBuilder> {

  WithStructureDefinition<?> definition;

  Map<String, Type> values = new HashMap<>();

  private ParametersBuilder(WithStructureDefinition<?> definition) {
    this.definition = definition;
  }

  public static ParametersBuilder create(WithStructureDefinition<?> definition) {
    return new ParametersBuilder(definition);
  }

  public ParametersBuilder set(String name, Type type) {
    this.values.put(name, type);
    return this;
  }

  public ParametersBuilder set(String name, String value) {
    return set(name, new StringType(value));
  }

  public ParametersBuilder set(String name, boolean value) {
    return set(name, new BooleanType(value));
  }

  public Parameters build() {
    val parameters = this.createResource(Parameters::new, definition.asCanonicalType());

    for (String name : this.values.keySet()) {
      parameters.setParameter(name, this.values.get(name));
    }

    return parameters;
  }
}
