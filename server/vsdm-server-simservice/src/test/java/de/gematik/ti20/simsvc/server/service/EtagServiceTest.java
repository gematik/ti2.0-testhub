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
package de.gematik.ti20.simsvc.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class EtagServiceTest {

  @Mock private HttpServletRequest request;

  private EtagService etagService;

  @BeforeEach
  void setUp() {
    etagService = new EtagService();
  }

  @Test
  void testAddEtagHeader_ValidInput() {
    String kvnr = "X123456789";
    String encodedResponse = "{\"resourceType\":\"Bundle\"}";
    HttpHeaders headers = new HttpHeaders();

    etagService.addEtagHeader(kvnr, encodedResponse, headers);

    assertTrue(headers.containsKey(EtagService.HEADER_NAME));
    assertNotNull(headers.getFirst(EtagService.HEADER_NAME));
    assertFalse(headers.getFirst(EtagService.HEADER_NAME).isEmpty());
  }

  @Test
  void testAddEtagHeader_EmptyResponse() {
    String kvnr = "X123456789";
    String encodedResponse = "";
    HttpHeaders headers = new HttpHeaders();

    etagService.addEtagHeader(kvnr, encodedResponse, headers);

    assertFalse(headers.containsKey(EtagService.HEADER_NAME));
  }

  @Test
  void testAddEtagHeader_NullResponse() {
    String kvnr = "X123456789";
    HttpHeaders headers = new HttpHeaders();

    etagService.addEtagHeader(kvnr, null, headers);

    assertFalse(headers.containsKey(EtagService.HEADER_NAME));
  }

  @Test
  void testAddEtagHeader_EmptyKvnr() {
    String encodedResponse = "{\"resourceType\":\"Bundle\"}";
    HttpHeaders headers = new HttpHeaders();

    etagService.addEtagHeader("", encodedResponse, headers);

    assertFalse(headers.containsKey(EtagService.HEADER_NAME));
  }

  @Test
  void testAddEtagHeader_NullKvnr() {
    String encodedResponse = "{\"resourceType\":\"Bundle\"}";
    HttpHeaders headers = new HttpHeaders();

    etagService.addEtagHeader(null, encodedResponse, headers);

    assertFalse(headers.containsKey(EtagService.HEADER_NAME));
  }

  @Test
  void testAddEtagHeader_SameKvnrReturnsSameEtag() {
    String kvnr = "X123456789";
    String encodedResponse = "{\"resourceType\":\"Bundle\"}";
    HttpHeaders headers1 = new HttpHeaders();
    HttpHeaders headers2 = new HttpHeaders();

    etagService.addEtagHeader(kvnr, encodedResponse, headers1);
    etagService.addEtagHeader(kvnr, encodedResponse, headers2);

    assertEquals(
        headers1.getFirst(EtagService.HEADER_NAME), headers2.getFirst(EtagService.HEADER_NAME));
  }

  @Test
  void testCheckEtag_ValidMatch() {
    String kvnr = "X123456789";
    String encodedResponse = "{\"resourceType\":\"Bundle\"}";
    HttpHeaders headers = new HttpHeaders();

    // Generate etag first
    etagService.addEtagHeader(kvnr, encodedResponse, headers);
    String etag = headers.getFirst(EtagService.HEADER_NAME);

    boolean result = etagService.checkEtag(kvnr, etag);

    assertTrue(result);
  }

  @Test
  void testCheckEtag_NoMatch() {
    String kvnr = "X123456789";
    String encodedResponse = "{\"resourceType\":\"Bundle\"}";
    HttpHeaders headers = new HttpHeaders();

    // Generate etag first
    etagService.addEtagHeader(kvnr, encodedResponse, headers);

    boolean result = etagService.checkEtag(kvnr, "different-etag");

    assertFalse(result);
  }

  @Test
  void testCheckEtag_NoRequestEtag() {
    String kvnr = "X123456789";
    String encodedResponse = "{\"resourceType\":\"Bundle\"}";
    HttpHeaders headers = new HttpHeaders();

    // Generate etag first
    etagService.addEtagHeader(kvnr, encodedResponse, headers);

    boolean result = etagService.checkEtag(kvnr, null);

    assertFalse(result);
  }

  @Test
  void testCheckEtag_EmptyKvnr() {
    boolean result = etagService.checkEtag("", "0");

    assertFalse(result);
  }

  @Test
  void testCheckEtag_NullKvnr() {
    boolean result = etagService.checkEtag(null, "0");

    assertFalse(result);
  }

  @Test
  void testCheckEtag_NoStoredEtag() {
    String kvnr = "X999999999";
    boolean result = etagService.checkEtag(kvnr, "0");

    assertFalse(result);
  }

  @Test
  void testEtagConsistency_DifferentKvnr() {
    String kvnr1 = "X123456789";
    String kvnr2 = "X987654321";
    String encodedResponse1 = "{\"kvnr\":\"X123456789\"}";
    String encodedResponse2 = "{\"kvnr\":\"X987654321\"}";
    HttpHeaders headers1 = new HttpHeaders();
    HttpHeaders headers2 = new HttpHeaders();

    etagService.addEtagHeader(kvnr1, encodedResponse1, headers1);
    etagService.addEtagHeader(kvnr2, encodedResponse2, headers2);

    assertNotEquals(
        headers1.getFirst(EtagService.HEADER_NAME), headers2.getFirst(EtagService.HEADER_NAME));
  }

  @Test
  void testEtagStore_Persistence() {
    String kvnr = "X123456789";
    String encodedResponse = "{\"resourceType\":\"Bundle\"}";
    HttpHeaders headers = new HttpHeaders();

    // Generate etag
    etagService.addEtagHeader(kvnr, encodedResponse, headers);
    String firstEtag = headers.getFirst(EtagService.HEADER_NAME);

    // Check etag exists in store
    assertTrue(etagService.checkEtag(kvnr, firstEtag));

    // Generate another etag for same kvnr - should return same etag
    HttpHeaders headers2 = new HttpHeaders();
    etagService.addEtagHeader(kvnr, "{\"different\":\"content\"}", headers2);
    String secondEtag = headers2.getFirst(EtagService.HEADER_NAME);

    assertEquals(firstEtag, secondEtag);
  }
}
