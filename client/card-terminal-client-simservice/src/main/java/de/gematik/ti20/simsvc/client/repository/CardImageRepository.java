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

import de.gematik.ti20.simsvc.client.config.CardTerminalConfiguration;
import de.gematik.ti20.simsvc.client.model.EgkIdentifier;
import de.gematik.ti20.simsvc.client.model.SmcbIdentifier;
import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import de.gematik.ti20.simsvc.client.service.EgkInfoService;
import de.gematik.ti20.simsvc.client.service.SmcBInfoService;
import de.gematik.ti20.simsvc.client.service.VirtualCardImageLoader;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

@Repository
@Slf4j
public class CardImageRepository {

  private final List<EgkCardImageIdentifier> egks = new ArrayList<>();
  private final List<SmcbCardImageIdentifier> smcbs = new ArrayList<>();

  private final VirtualCardImageLoader cardImageLoader;
  private final EgkInfoService egkInfoService;
  private final SmcBInfoService smcBInfoService;
  private final CardTerminalConfiguration configuration;

  public CardImageRepository(
      final VirtualCardImageLoader cardImageLoader,
      final EgkInfoService egkInfoService,
      final SmcBInfoService smcBInfoService,
      final CardTerminalConfiguration configuration) {
    this.cardImageLoader = cardImageLoader;
    this.egkInfoService = egkInfoService;
    this.smcBInfoService = smcBInfoService;
    this.configuration = configuration;
  }

  @PostConstruct
  public void initialize() {
    initialize(configuration.getCardImagePaths());
  }

  /**
   * Adds EGK and SMC-B images recursively from classpath directories or filesystem directories
   * prefixed with {@code file:}. Relative filesystem paths are resolved from the working directory.
   */
  public void initialize(final List<String> paths) {
    final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    for (final String path : paths) {
      scanDirectory(resolver, path);
    }
  }

  private void scanDirectory(
      final PathMatchingResourcePatternResolver resolver, final String path) {
    final String directory = path.isEmpty() ? path : withTrailingSlash(path);
    final boolean filesystem = directory.startsWith("file:");
    try {
      for (final Resource root :
          resolver.getResources(filesystem ? directory : "classpath*:" + directory)) {
        if (!root.exists()) {
          log.warn("Card image directory does not exist: {}", path);
          continue;
        }
        scanRoot(resolver, root, directory, filesystem);
      }
    } catch (final IOException e) {
      log.warn("Could not scan card image directory {}: {}", path, e.getMessage());
    }
  }

  private void scanRoot(
      final PathMatchingResourcePatternResolver resolver,
      final Resource root,
      final String directory,
      final boolean filesystem)
      throws IOException {
    final String rootDirectoryUrl = withTrailingSlash(root.getURL().toExternalForm());
    for (final Resource resource : resolver.getResources(rootDirectoryUrl + "**/*.xml")) {
      addCardImage(
          resource, resolveCardImagePath(resource, directory, rootDirectoryUrl, filesystem));
    }
  }

  private String resolveCardImagePath(
      final Resource resource,
      final String directory,
      final String rootDirectoryUrl,
      final boolean filesystem)
      throws IOException {
    final String resourceUrl = resource.getURL().toExternalForm();
    final String relativePath =
        StringUtils.uriDecode(
            resourceUrl.substring(rootDirectoryUrl.length()), StandardCharsets.UTF_8);
    return filesystem ? resourceUrl : directory + relativePath;
  }

  private String withTrailingSlash(final String path) {
    return path.endsWith("/") ? path : path + "/";
  }

  private void addCardImage(final Resource resource, final String path) {
    try {
      final VirtualCardImageData card =
          cardImageLoader.load(resource.getContentAsString(StandardCharsets.UTF_8));
      if ("EGK".equals(card.cardType())) {
        addEgkCardImage(resource, path, card);
        return;
      }

      if ("SMCB".equals(card.cardType())) {
        addSmcbCardImage(resource, path, card);
        return;
      }

      log.debug("Skipping unsupported card image {}", resource);
    } catch (final IOException
        | SAXException
        | ParserConfigurationException
        | IllegalArgumentException
        | IndexOutOfBoundsException e) {
      log.warn("Skipping unreadable card image {}: {}", resource, e.getMessage());
    }
  }

  private void addEgkCardImage(
      final Resource resource, final String path, final VirtualCardImageData card) {
    final EgkInfoDto info = egkInfoService.extractEgkInfo(card);
    if (info == null
        || info.getIknr() == null
        || info.getIknr().isBlank()
        || info.getKvnr() == null
        || info.getKvnr().isBlank()) {
      log.warn("Skipping EGK card image {} without IKNR or KVNR", resource);
      return;
    }

    put(new EgkCardImageIdentifier(info.getIknr(), info.getKvnr(), path));
    log.info(
        "Added EGK card image {} with IKNR {} and KVNR {}",
        resource,
        info.getIknr(),
        info.getKvnr());
  }

  private void addSmcbCardImage(
      final Resource resource, final String path, final VirtualCardImageData card) {
    final SmcBInfoDto info = smcBInfoService.extractSmcBInfo(card);
    if (info == null) {
      log.debug("Skipping non-SMCB card image {}", resource);
      return;
    }

    if (info.getTelematikId() == null || info.getTelematikId().isBlank()) {
      log.warn("Skipping SMCB card image {} without Telematik-ID", resource);
      return;
    }

    smcbs.add(new SmcbCardImageIdentifier(info.getTelematikId(), path));
    log.info("Added SMCB card image {} with Telematik-ID {}", resource, info.getTelematikId());
  }

  public void put(final EgkCardImageIdentifier cardImageIdentifier) {
    egks.add(cardImageIdentifier);
  }

  public List<CardImageIdentifier> findByEgkIdentifier(final EgkIdentifier egkIdentifier) {
    return egks.stream()
        .filter(identifier -> identifier.matches(egkIdentifier))
        .map(identifier -> (CardImageIdentifier) identifier)
        .toList();
  }

  public List<CardImageIdentifier> findBySmcbIdentifier(final SmcbIdentifier smcbIdentifier) {
    return smcbs.stream()
        .filter(identifier -> identifier.matches(smcbIdentifier))
        .map(identifier -> (CardImageIdentifier) identifier)
        .toList();
  }
}
