/*-
 * #%L
 * VSDM 2.0 Testsuite
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
package de.gematik.ti20.vsdm.test.load;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ZetaClientPoolTest {

  @Test
  void constructorRejectsDuplicateActorIds() {
    SimulationConfigBean.SmcbData first = new SimulationConfigBean.SmcbData();
    first.setActorId("actor-1");
    first.setKeypath("/tmp/first.p12");

    SimulationConfigBean.SmcbData second = new SimulationConfigBean.SmcbData();
    second.setActorId("actor-1");
    second.setKeypath("/tmp/second.p12");

    assertThrows(
        IllegalArgumentException.class,
        () -> new ZetaClientPool("resource", 1, List.of(first, second)));
  }

  @Test
  void constructorRejectsBlankActorId() {
    SimulationConfigBean.SmcbData smcbData = new SimulationConfigBean.SmcbData();
    smcbData.setActorId(" ");
    smcbData.setKeypath("/tmp/first.p12");

    assertThrows(
        IllegalArgumentException.class, () -> new ZetaClientPool("resource", 1, List.of(smcbData)));
  }
}
