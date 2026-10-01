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
package de.gematik.ti20.simsvc.server.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class TraceIdFilterTest {

  private final TraceIdFilter filter = new TraceIdFilter();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void doFilter_setsTraceIdFromRequestHeader_andClearsMdcAfterChain() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader("x-trace-id")).thenReturn("trace-123");
    doAnswer(
            invocation -> {
              assertEquals("trace-123", MDC.get("traceId"));
              return null;
            })
        .when(chain)
        .doFilter(request, response);

    filter.doFilter(request, response, chain);

    assertNull(MDC.get("traceId"));
    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_withoutHttpServletRequest_keepsMdcEmptyAndStillCallsChain() throws Exception {
    ServletRequest request = mock(ServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertNull(MDC.get("traceId"));
    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_whenChainThrows_stillClearsMdc() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader("x-trace-id")).thenReturn("trace-456");
    doAnswer(
            invocation -> {
              throw new RuntimeException("boom");
            })
        .when(chain)
        .doFilter(request, response);

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, chain));

    assertEquals("boom", exception.getMessage());
    assertNull(MDC.get("traceId"));
  }

  @Test
  void initAndDestroy_doNothing() {
    FilterConfig filterConfig = mock(FilterConfig.class);

    assertDoesNotThrow(() -> filter.init(filterConfig));
    assertDoesNotThrow(() -> filter.destroy());
  }
}
