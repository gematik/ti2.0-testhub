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
package de.gematik.ti20.simsvc.client.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;

class LoggingUtilsTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void testLogWithContext_setsAndRemovesMdc() {
    Logger logger = mock(Logger.class);
    Map<String, String> context = new HashMap<>();
    context.put("user", "alice");
    context.put("session", "123");

    LoggingUtils.logWithContext(logger, LoggingUtils.LogLevel.INFO, "Test message", context);

    // Prüfe, dass Logger aufgerufen wurde
    verify(logger).info("Test message");

    // Nach dem Aufruf sollte MDC leer sein
    assertNull(MDC.get("user"));
    assertNull(MDC.get("session"));
  }

  @Test
  void testExecuteWithLogging_setsAndRemovesMdc_andReturnsValue() {
    Logger logger = mock(Logger.class);
    Map<String, String> context = new HashMap<>();
    context.put("op", "test");

    AtomicBoolean executed = new AtomicBoolean(false);

    String result =
        LoggingUtils.executeWithLogging(
            logger,
            LoggingUtils.LogLevel.DEBUG,
            "myOp",
            context,
            () -> {
              executed.set(true);
              // Während der Ausführung ist MDC gesetzt
              assertEquals("test", MDC.get("op"));
              return "done";
            });

    assertTrue(executed.get());
    assertEquals("done", result);

    // Prüfe, dass Logger für Start und Ende aufgerufen wurde
    verify(logger).debug("Starting operation: {}", "myOp");
    verify(logger).debug(startsWith("Completed operation: {} in "), eq("myOp"), anyLong());

    // Nach dem Aufruf sollte MDC leer sein
    assertNull(MDC.get("op"));
  }
}
