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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import java.util.Objects;

public final class SimulationConfigProvider {

  private SimulationConfigProvider() {}

  private static SimulationConfigBean instance;

  public static synchronized SimulationConfigBean getInstance() {
    if (instance == null) {
      instance = SimulationConfigProvider.load();
    }
    return instance;
  }

  private static SimulationConfigBean load() {
    String configResource = System.getProperty("config.resource");

    Config root =
        ConfigFactory.parseResources(Objects.requireNonNullElse(configResource, "simulation.conf"))
            .resolve();

    Config simulation = root.getConfig("simulation");

    return ConfigBeanFactory.create(simulation, SimulationConfigBean.class);
  }
}
