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
package de.gematik.ti20.simsvc.server.repository;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.core.RbelValueFacet;
import de.gematik.test.testdata.TestDataManager;
import de.gematik.test.testdata.exceptions.TestDataInitializationException;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.springframework.stereotype.Repository;

@Repository
public class TestDataRepository {

  @PostConstruct
  public void init() {
    String path = "de/gematik/vsdm/testdata";
    URL dir = getClass().getClassLoader().getResource(path);
    File testDataDir = new File(dir.getPath());
    if (!testDataDir.exists()) {
      testDataDir = new File("./" + path);
    }
    TestDataManager.initialize(testDataDir);
  }

  private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9äüöÄÜÖß\s]+$");

  public void validateAlias(String alias) {
    if (alias == null || !ALIAS_PATTERN.matcher(alias).matches()) {
      throw new IllegalArgumentException("Alias contains invalid characters");
    }
  }

  public Optional<RbelElement> findElementByAlias(String alias) {

    validateAlias(alias);

    var elements =
        TestDataManager.getTestDataRoot()
            .findRbelPathMembers("$..[?(@.alias.. == '" + alias + "')]");
    if (elements.isEmpty()) {
      return Optional.empty();
    }
    if (elements.size() == 1) {
      return Optional.of(elements.get(0));
    }
    throw new TestDataInitializationException(
        "Wrong testdata initialization, multiple elements found for alias " + alias);
  }

  public Optional<RbelElement> findElementByKeyValue(String key, String value) {
    var elements =
        TestDataManager.getTestDataRoot()
            .findRbelPathMembers("$..[?(@." + key + ".. == '" + value + "')]");
    if (elements.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(elements.get(0));
  }

  public Optional<String> getStringFor(RbelElement parent, String rbelPath) {
    if (parent != null) {
      var found = parent.findElement(rbelPath);
      if (found.isPresent()) {
        var facet = found.get().getFacet(RbelValueFacet.class);
        if (facet.isPresent()) {
          var val = facet.get().getValue();
          if (val != null) {
            return Optional.of(val.toString());
          }
        }
      }
    }
    return Optional.empty();
  }

  public Optional<Date> getDateFor(RbelElement parent, String rbelPath) {
    var value = getStringFor(parent, rbelPath);
    if (value.isEmpty()) {
      return Optional.empty();
    }

    String pattern = "EEE MMM dd HH:mm:ss z yyyy";
    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
    dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));

    try {
      return Optional.of(dateFormat.parse(value.get()));
    } catch (Exception e) {
      // TODO: log warning
    }
    return Optional.empty();
  }
}
