/*
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
package de.gematik.ti20.vsdm.test.load;

import java.time.Duration;
import lombok.Data;

@Data
public class SimulationConfigBean {

  private Url url;
  private Ramp ramp;
  private boolean randomReadVsd;

  @Data
  public static class Url {
    private Client client;
    private Server server;

    @Data
    public static class Client {
      private String card;
      private String vsdm;
    }

    @Data
    public static class Server {
      private String popp;
      private String vsdm;
    }
  }

  @Data
  public static class Ramp {
    private Users users;

    @Data
    public static class Users {
      private Random random;
      private Steady steady;

      @Data
      public static class Random {
        private int min;
        private int max;
        private int cycles;
        private Duration duration;
      }

      @Data
      public static class Steady {
        private int number;
        private Duration duration;
      }
    }
  }
}
