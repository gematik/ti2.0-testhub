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
package de.gematik.ti20.client.zeta.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig.UserAgentConfig;
import de.gematik.ti20.client.zeta.exception.ZetaHttpResponseException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ZetaHttpRequest {

  private String traceId;
  private final String url;
  private final List<Header> headers = new ArrayList<>();
  private HttpMethod method = HttpMethod.GET;
  private RequestBody body;

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

  public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
  }

  public static final class ContentType {

    public static final String JSON = "application/json; charset=utf-8";
    public static final String TEXT = "text/plain; charset=utf-8";
    public static final String XML = "application/xml; charset=utf-8";
    public static final String FHIRJSON = "application/fhir+json; charset=utf-8";
    public static final String FHIRXML = "application/fhir+xml; charset=utf-8";
    public static final String FORM = "application/x-www-form-urlencoded";
  }

  public enum HeaderName {
    Authorization("Authorization"),
    ContentType("Content-Type"),
    Accept("Accept"),
    UserAgent("User-Agent"),
    CacheControl("Cache-Control"),
    ContentLength("Content-Length"),
    Host("Host"),
    Referer("Referer"),
    Connection("Connection"),
    TraceId("X-Trace-Id");

    private final String name;

    HeaderName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  public enum AuthorizationType {
    BEARER("Bearer"),
    DPOP("DPoP");

    private final String type;

    AuthorizationType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }

  public ZetaHttpRequest(String url) {
    traceId = generateTraceId();
    this.setHeader(HeaderName.TraceId.getName(), traceId);
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public RequestBody getBody() {
    return body;
  }

  public boolean hasBody() {
    return body != null;
  }

  public boolean hasHeader(String name) {
    return headers.stream().anyMatch(header -> header.name.equals(name));
  }

  public String getHeader(String name) {
    return headers.stream()
        .filter(header -> header.name.equals(name))
        .map(header -> header.value)
        .findFirst()
        .orElse(null);
  }

  public List<Header> getHeaders() {
    return headers;
  }

  public void addHeader(String name, String value) {
    headers.add(new Header(name, value));
  }

  public void setHeader(String name, String value) {
    removeHeader(name);
    headers.add(new Header(name, value));
  }

  public void removeHeader(String name) {
    headers.removeIf(header -> header.name.equals(name));
  }

  public void copyHeaderFrom(ZetaHttpRequest request, String name) {
    var value = request.getHeader(name);
    if (value != null) {
      setHeader(name, value);
    }
  }

  public void setHeaderAuthorization(AuthorizationType type, String token) {
    setHeader(HeaderName.Authorization.getName(), type.getType() + " " + token);
  }

  public void setHeaderUserAgent(String appName, String appVersion) {
    setHeader(HeaderName.UserAgent.getName(), appName + "/" + appVersion);
  }

  public void setHeaderUserAgent(UserAgentConfig config) {
    setHeader(HeaderName.UserAgent.getName(), config.getUserAgent());
  }

  public void post(String body, String contentType) {
    method = HttpMethod.POST;
    this.body = createRequestBody(body, contentType);
  }

  public void postJson(Object body) throws JsonProcessingException {
    method = HttpMethod.POST;
    this.body = createRequestBodyJson(body);
  }

  public void postValues(Map<String, String> values) throws UnsupportedEncodingException {
    method = HttpMethod.POST;
    this.setHeader(HeaderName.ContentType.getName(), ContentType.FORM);
    this.body = createRequestBody(encodeParams(values), ContentType.FORM);
  }

  private RequestBody createRequestBody(String body, String contentType) {
    return RequestBody.create(body, MediaType.get(contentType));
  }

  private RequestBody createRequestBodyJson(Object body) throws JsonProcessingException {
    var json = jsonConverter.writeValueAsString(body);
    this.setHeader(HeaderName.ContentType.getName(), ContentType.JSON);
    return createRequestBody(json, ContentType.JSON);
  }

  public void put(String body, String contentType) {
    method = HttpMethod.PUT;
    this.body = createRequestBody(body, contentType);
  }

  public void putJson(Object body) throws JsonProcessingException {
    method = HttpMethod.PUT;
    this.body = createRequestBodyJson(body);
  }

  public void delete() {
    method = HttpMethod.DELETE;
  }

  public Request build() throws ZetaHttpResponseException {

    try {
      this.validate();
    } catch (Exception e) {
      throw new ZetaHttpResponseException(
          HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage(), e, this);
    }

    var requestBuilder = new Request.Builder().url(url);

    if (url.startsWith("ws://") || url.startsWith("wss://")) {
      // nothing to do currently
    } else {
      requestBuilder.method(method.name(), body);
    }

    headers.forEach(header -> requestBuilder.addHeader(header.name, header.value));

    return requestBuilder.build();
  }

  protected void validate() throws IllegalStateException, URISyntaxException {
    if (method == HttpMethod.POST || method == HttpMethod.PUT) {
      if (body == null) {
        throw new IllegalStateException("Request body is required for POST and PUT requests");
      }
      if (getHeader(HeaderName.ContentType.getName()) == null) {
        throw new IllegalStateException(
            "Content-Type header is required for POST and PUT requests");
      }
    }

    if (getHeader(HeaderName.UserAgent.getName()) == null) {
      throw new IllegalStateException("User-Agent header is required");
    }

    URI uri = new URI(url);

    if (!uri.isAbsolute()) {
      throw new URISyntaxException(url, "URL must be absolute, not relative");
    }

    String scheme = uri.getScheme();
    switch (scheme) {
      case "http":
      case "https":
      case "ws":
      case "wss":
        break;
      default:
        throw new URISyntaxException(url, "Invalid scheme");
    }

    if (uri.getHost() == null) {
      throw new URISyntaxException(url, "Host is required");
    }
  }

  private String generateTraceId() {
    return UUID.randomUUID().toString();
  }

  private String encodeParams(Map<String, String> params) throws UnsupportedEncodingException {
    StringJoiner sj = new StringJoiner("&");
    for (Map.Entry<String, String> entry : params.entrySet()) {
      sj.add(
          URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
              + "="
              + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return sj.toString();
  }
}
