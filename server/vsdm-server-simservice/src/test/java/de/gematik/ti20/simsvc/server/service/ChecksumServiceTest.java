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

import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ChecksumServiceTest {

  private ChecksumService checksumService;

  @BeforeEach
  void setUp() {
    checksumService = new ChecksumService();
  }

  @Test
  void testAddChecksumHeader_ValidContent() {
    String content = "test content";
    HttpHeaders headers = new HttpHeaders();

    checksumService.addChecksumHeader(content, headers);

    assertTrue(headers.containsKey(ChecksumService.HEADER_NAME));
    assertNotNull(headers.getFirst(ChecksumService.HEADER_NAME));
    assertFalse(headers.getFirst(ChecksumService.HEADER_NAME).isEmpty());
  }

  @Test
  void testAddChecksumHeader_EmptyContent() {
    String content = "";
    HttpHeaders headers = new HttpHeaders();

    checksumService.addChecksumHeader(content, headers);

    assertFalse(headers.containsKey(ChecksumService.HEADER_NAME));
  }

  @Test
  void testAddChecksumHeader_NullHeaders() {
    String content = "test content";

    assertThrows(
        NullPointerException.class, () -> checksumService.addChecksumHeader(content, null));
  }

  @Test
  void testAddChecksumHeader_ConsistentChecksum() {
    String content = "test content";
    HttpHeaders headers1 = new HttpHeaders();
    HttpHeaders headers2 = new HttpHeaders();

    checksumService.addChecksumHeader(content, headers1);
    checksumService.addChecksumHeader(content, headers2);

    assertEquals(
        headers1.getFirst(ChecksumService.HEADER_NAME),
        headers2.getFirst(ChecksumService.HEADER_NAME));
  }

  @Test
  void testAddChecksumHeader_DifferentContent() {
    String content1 = "test content 1";
    String content2 = "test content 2";
    HttpHeaders headers1 = new HttpHeaders();
    HttpHeaders headers2 = new HttpHeaders();

    checksumService.addChecksumHeader(content1, headers1);
    checksumService.addChecksumHeader(content2, headers2);

    assertNotEquals(
        headers1.getFirst(ChecksumService.HEADER_NAME),
        headers2.getFirst(ChecksumService.HEADER_NAME));
  }

  @Test
  void testAddChecksumHeader_LargeContent() {
    StringBuilder largeContent = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      largeContent.append("test ");
    }
    HttpHeaders headers = new HttpHeaders();

    assertDoesNotThrow(() -> checksumService.addChecksumHeader(largeContent.toString(), headers));
    assertTrue(headers.containsKey(ChecksumService.HEADER_NAME));
  }

  @Test
  void testAddChecksumHeader_SpecialCharacters() {
    String content = "äöüÄÖÜß@#$%^&*(){}[]|\\:;\"'<>,.?/~`";
    HttpHeaders headers = new HttpHeaders();

    assertDoesNotThrow(() -> checksumService.addChecksumHeader(content, headers));
    assertTrue(headers.containsKey(ChecksumService.HEADER_NAME));
    assertNotNull(headers.getFirst(ChecksumService.HEADER_NAME));
  }

  @Test
  void testAddChecksumHeader_HeaderFormat() throws NoSuchAlgorithmException {
    String content = "test content";
    HttpHeaders headers = new HttpHeaders();

    checksumService.addChecksumHeader(content, headers);

    String checksum = headers.getFirst(ChecksumService.HEADER_NAME);
    assertNotNull(checksum);
    // SHA-256 hash should be 64 characters long when represented as hex
    assertTrue(checksum.length() >= 32); // At least MD5 length
  }
}
