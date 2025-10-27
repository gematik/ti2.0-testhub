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
package de.gematik.ti20.client.popp.message;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioStepTest {

  @Test
  void testDefaultConstructor() {
    ScenarioStep step = new ScenarioStep();
    assertNull(step.getCommandApdu());
    assertNull(step.getExpectedStatusWords());
  }

  @Test
  void testConstructorWithParams() {
    List<String> statusWords = Arrays.asList("9000", "6A82");
    ScenarioStep step = new ScenarioStep("00A40400", statusWords);
    assertEquals("00A40400", step.getCommandApdu());
    assertEquals(statusWords, step.getExpectedStatusWords());
  }

  @Test
  void testSettersAndGetters() {
    ScenarioStep step = new ScenarioStep();
    step.setCommandApdu("00B00000");
    step.setExpectedStatusWords(Arrays.asList("9000"));
    assertEquals("00B00000", step.getCommandApdu());
    assertEquals(Arrays.asList("9000"), step.getExpectedStatusWords());
  }

  @Test
  void testJacksonSerializationAndDeserialization() throws Exception {
    List<String> statusWords = Arrays.asList("9000", "6A82");
    ScenarioStep step = new ScenarioStep("00A40400", statusWords);
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(step);
    assertTrue(json.contains("\"commandApdu\":\"00A40400\""));
    assertTrue(json.contains("\"expectedStatusWords\":[\"9000\",\"6A82\"]"));

    ScenarioStep deserialized = mapper.readValue(json, ScenarioStep.class);
    assertEquals("00A40400", deserialized.getCommandApdu());
    assertEquals(statusWords, deserialized.getExpectedStatusWords());
  }
}
