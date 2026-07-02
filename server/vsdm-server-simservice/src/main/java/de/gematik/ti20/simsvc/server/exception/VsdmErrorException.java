/*-
 * #%L
 * VSDM Server Simservice
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
package de.gematik.ti20.simsvc.server.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public class VsdmErrorException extends RuntimeException {
  private final ErrorCase errorCase;
  private final Map<String, String> values;

  public VsdmErrorException(ErrorCase errorCase) {
    this(errorCase, Map.of());
  }

  public VsdmErrorException(final ErrorCase errorCase, final Map<String, String> values) {
    this.errorCase = errorCase;
    this.values = values;
  }
}
