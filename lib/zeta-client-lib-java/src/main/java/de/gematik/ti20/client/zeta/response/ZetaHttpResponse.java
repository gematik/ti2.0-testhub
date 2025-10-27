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
package de.gematik.ti20.client.zeta.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import okhttp3.Response;

public class ZetaHttpResponse {

  private final int code;
  private final List<Header> headers = new ArrayList<>();
  private String body;

  private final ObjectMapper jsonConverter = new ObjectMapper();

  public static class Header {

    private final String name;
    private final String value;

    public Header(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }

  public ZetaHttpResponse(final Response response) {
    this.code = response.code();
    headers.addAll(
        response.headers().names().stream()
            .map(name -> new Header(name, response.header(name)))
            .toList());
    try (var b = response.body()) {
      if (b != null) {
        body = b.string();
      }
    } catch (Exception e) {
      body = null;
    }
  }

  public ZetaHttpResponse(final int code, final Object content) {
    this.code = code;
    if (content instanceof String) {
      this.body = (String) content;
    } else {
      try {
        this.body = jsonConverter.writeValueAsString(content);
      } catch (Exception e) {
        this.body = null;
      }
    }
  }

  public int getStatusCode() {
    return code;
  }

  public boolean isSuccessful() {
    return code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE;
  }

  public Optional<String> getBody() {
    return Optional.ofNullable(body);
  }

  public <T> Optional<T> getBodyFromJson(Class<T> type) {
    try {
      return Optional.of(jsonConverter.readValue(body, type));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public Map<String, List<String>> getHeaders() {
    return headers.stream()
        .collect(
            Collectors.groupingBy(
                Header::getName, Collectors.mapping(Header::getValue, Collectors.toList())));
  }
}
