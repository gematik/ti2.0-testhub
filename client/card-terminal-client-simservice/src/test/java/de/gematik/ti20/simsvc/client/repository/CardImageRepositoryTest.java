/*-
 * #%L
 * Card Terminal Simulator
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
package de.gematik.ti20.simsvc.client.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.ti20.simsvc.client.config.CardTerminalConfiguration;
import de.gematik.ti20.simsvc.client.model.EgkIdentifier;
import de.gematik.ti20.simsvc.client.model.SmcbIdentifier;
import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import de.gematik.ti20.simsvc.client.service.EgkInfoService;
import de.gematik.ti20.simsvc.client.service.SmcBInfoService;
import de.gematik.ti20.simsvc.client.service.VirtualCardImageLoader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.xml.sax.SAXException;

class CardImageRepositoryTest {

  private final EgkIdentifier identifier = new EgkIdentifier("109500969", "X110639491");
  private final CardTerminalConfiguration configuration = new CardTerminalConfiguration();

  @Test
  void beanCreation_initializesConfiguredClasspathDirectories() {
    configuration.setCardImagePaths(List.of("attached_assets"));
    final CardImageRepository repository =
        spy(
            new CardImageRepository(
                new VirtualCardImageLoader(),
                new EgkInfoService(),
                new SmcBInfoService(),
                configuration));
    try (final AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext()) {
      context.registerBean(CardImageRepository.class, () -> repository);
      context.registerBean(SmcBInfoService.class);
      context.refresh();

      verify(repository).initialize(List.of("attached_assets"));
      assertTrue(
          repository.findByEgkIdentifier(identifier).stream()
              .anyMatch(
                  card ->
                      card.path().equals("attached_assets/EGK_80276883110000168583_gema5.xml")));
    }
  }

  @Test
  void initialize_scansFilesystemSubfolders(@TempDir final Path directory) throws Exception {
    final Path nestedDirectory = Files.createDirectory(directory.resolve("nested"));
    final Path file = nestedDirectory.resolve("egk image.xml");
    try (final InputStream input = new ClassPathResource("egkCardImage.xml").getInputStream()) {
      Files.copy(input, file);
    }
    final CardImageRepository repository =
        new CardImageRepository(
            new VirtualCardImageLoader(),
            new EgkInfoService(),
            new SmcBInfoService(),
            configuration);

    repository.initialize(List.of(directory.toUri().toString()));

    final List<CardImageIdentifier> cards = repository.findByEgkIdentifier(identifier);
    assertEquals(1, cards.size());
    assertEquals(
        Files.readString(file),
        new DefaultResourceLoader()
            .getResource(cards.get(0).path())
            .getContentAsString(StandardCharsets.UTF_8));
  }

  @Test
  void initialize_loadsEgksAndSmcbsFromClasspath() {
    final CardImageRepository repository =
        new CardImageRepository(
            new VirtualCardImageLoader(),
            new EgkInfoService(),
            new SmcBInfoService(),
            configuration);

    repository.initialize(List.of("missing-directory", "cards/", ""));

    final List<CardImageIdentifier> cards = repository.findByEgkIdentifier(identifier);
    assertTrue(cards.stream().anyMatch(card -> card.path().equals("egkCardImage.xml")));
    assertTrue(cards.stream().anyMatch(card -> card.path().equals("egkCardImageInvalid.xml")));
    assertTrue(
        repository
            .findBySmcbIdentifier(new SmcbIdentifier("1-SMC-B-Testkarte--883110000168650"))
            .contains(
                new SmcbCardImageIdentifier(
                    "1-SMC-B-Testkarte--883110000168650", "SMC_B_80276883110000168650_gema5.xml")));
    assertTrue(repository.findBySmcbIdentifier(new SmcbIdentifier("unknown-actor")).isEmpty());
  }

  @Test
  void initialize_skipsSmcbsWithoutTelematikId() throws Exception {
    final VirtualCardImageLoader loader = mock(VirtualCardImageLoader.class);
    final SmcBInfoService infoService = mock(SmcBInfoService.class);
    final VirtualCardImageData card = new VirtualCardImageData(Map.of("EF.C.HCI.ENC.E256", "data"));
    when(loader.load(anyString())).thenReturn(card);
    final CardImageRepository repository =
        new CardImageRepository(loader, new EgkInfoService(), infoService, configuration);

    for (final SmcBInfoDto info : List.of(new SmcBInfoDto(), smcbInfoWithTelematikId(" "))) {
      when(infoService.extractSmcBInfo(card)).thenReturn(info);
      repository.initialize(List.of("cards"));
      assertTrue(repository.findBySmcbIdentifier(new SmcbIdentifier("actor-1")).isEmpty());
    }

    when(infoService.extractSmcBInfo(card)).thenReturn(null);
    repository.initialize(List.of("cards"));
    assertTrue(repository.findBySmcbIdentifier(new SmcbIdentifier("actor-1")).isEmpty());
  }

  @Test
  void initialize_skipsUnreadableAndMalformedXml() throws Exception {
    for (final Exception failure :
        List.of(new IOException("Unreadable"), new SAXException("Malformed XML"))) {
      final VirtualCardImageLoader loader = mock(VirtualCardImageLoader.class);
      final EgkInfoService infoService = mock(EgkInfoService.class);
      when(loader.load(anyString())).thenThrow(failure);
      final CardImageRepository repository =
          new CardImageRepository(loader, infoService, new SmcBInfoService(), configuration);

      assertDoesNotThrow(() -> repository.initialize(List.of("cards")));

      assertTrue(repository.findByEgkIdentifier(identifier).isEmpty());
      verifyNoInteractions(infoService);
    }
  }

  @Test
  void initialize_storesFullPath() throws Exception {
    final VirtualCardImageLoader loader = mock(VirtualCardImageLoader.class);
    final EgkInfoService infoService = mock(EgkInfoService.class);
    final VirtualCardImageData card = new VirtualCardImageData(Map.of("EF.PD", "data"));
    final EgkInfoDto info = new EgkInfoDto();
    info.setIknr(identifier.iknr());
    info.setKvnr(identifier.kvnr());
    when(loader.load(anyString())).thenReturn(card);
    when(infoService.extractEgkInfo(card)).thenReturn(info);
    final CardImageRepository repository =
        new CardImageRepository(loader, infoService, new SmcBInfoService(), configuration);

    repository.initialize(List.of("cards/nested/"));

    assertEquals(
        List.of(
            new EgkCardImageIdentifier(
                identifier.iknr(), identifier.kvnr(), "cards/nested/card-image.xml")),
        repository.findByEgkIdentifier(identifier));
  }

  @Test
  void initialize_scansSubfoldersAndPreservesLoadablePaths() throws Exception {
    final VirtualCardImageLoader loader = mock(VirtualCardImageLoader.class);
    final EgkInfoService infoService = mock(EgkInfoService.class);
    final VirtualCardImageData card = new VirtualCardImageData(Map.of("EF.PD", "data"));
    final EgkInfoDto info = new EgkInfoDto();
    info.setIknr(identifier.iknr());
    info.setKvnr(identifier.kvnr());
    when(loader.load(anyString())).thenReturn(card);
    when(infoService.extractEgkInfo(card)).thenReturn(info);

    for (final String directory : List.of("cards", "cards/")) {
      final CardImageRepository repository =
          new CardImageRepository(loader, infoService, new SmcBInfoService(), configuration);
      repository.initialize(List.of(directory));

      final List<String> paths =
          repository.findByEgkIdentifier(identifier).stream()
              .map(CardImageIdentifier::path)
              .sorted()
              .toList();
      assertEquals(List.of("cards/nested/card-image.xml", "cards/slot-controller-card.xml"), paths);
      for (final String path : paths) {
        assertTrue(
            new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8)
                .contains("<xml/>"));
      }
    }
  }

  @Test
  void initialize_scansSubfoldersInsideJar(@TempDir final Path temporaryDirectory)
      throws Exception {
    final Path jar = temporaryDirectory.resolve("cards.jar");
    final String cardPath = "jar-cards/nested/card-image.xml";
    try (final JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
      output.putNextEntry(new JarEntry("jar-cards/"));
      output.closeEntry();
      output.putNextEntry(new JarEntry(cardPath));
      output.write("<xml/>".getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
    }

    final VirtualCardImageLoader loader = mock(VirtualCardImageLoader.class);
    final EgkInfoService infoService = mock(EgkInfoService.class);
    final VirtualCardImageData card = new VirtualCardImageData(Map.of("EF.PD", "data"));
    final EgkInfoDto info = new EgkInfoDto();
    info.setIknr(identifier.iknr());
    info.setKvnr(identifier.kvnr());
    when(loader.load("<xml/>")).thenReturn(card);
    when(infoService.extractEgkInfo(card)).thenReturn(info);
    final CardImageRepository repository =
        new CardImageRepository(loader, infoService, new SmcBInfoService(), configuration);
    final Thread thread = Thread.currentThread();
    final ClassLoader originalClassLoader = thread.getContextClassLoader();
    try (final URLClassLoader classLoader =
        new URLClassLoader(new URL[] {jar.toUri().toURL()}, originalClassLoader)) {
      thread.setContextClassLoader(classLoader);

      repository.initialize(List.of("jar-cards"));

      assertEquals(
          List.of(new EgkCardImageIdentifier(identifier.iknr(), identifier.kvnr(), cardPath)),
          repository.findByEgkIdentifier(identifier));
      assertEquals(
          "<xml/>",
          new ClassPathResource(cardPath, classLoader).getContentAsString(StandardCharsets.UTF_8));
    } finally {
      thread.setContextClassLoader(originalClassLoader);
    }
  }

  @Test
  void initialize_skipsEgksWithoutIdentifiers() throws Exception {
    final VirtualCardImageLoader loader = mock(VirtualCardImageLoader.class);
    final EgkInfoService infoService = mock(EgkInfoService.class);
    final VirtualCardImageData card = new VirtualCardImageData(Map.of("EF.PD", "data"));
    when(loader.load(anyString())).thenReturn(card);
    when(infoService.extractEgkInfo(card)).thenReturn(new EgkInfoDto());
    final CardImageRepository repository =
        new CardImageRepository(loader, infoService, new SmcBInfoService(), configuration);

    repository.initialize(List.of("cards"));

    assertTrue(repository.findByEgkIdentifier(identifier).isEmpty());
  }

  @Test
  void put_storesIdentifiers() {
    final CardImageRepository repository =
        new CardImageRepository(
            new VirtualCardImageLoader(),
            new EgkInfoService(),
            new SmcBInfoService(),
            configuration);
    final EgkCardImageIdentifier first =
        new EgkCardImageIdentifier(identifier.iknr(), identifier.kvnr(), "cards/first.xml");
    final EgkCardImageIdentifier second =
        new EgkCardImageIdentifier(identifier.iknr(), identifier.kvnr(), "cards/second.xml");

    repository.put(first);
    repository.put(second);

    assertEquals(List.of(first, second), repository.findByEgkIdentifier(identifier));
  }

  private SmcBInfoDto smcbInfoWithTelematikId(final String telematikId) {
    final SmcBInfoDto info = new SmcBInfoDto();
    info.setTelematikId(telematikId);
    return info;
  }
}
