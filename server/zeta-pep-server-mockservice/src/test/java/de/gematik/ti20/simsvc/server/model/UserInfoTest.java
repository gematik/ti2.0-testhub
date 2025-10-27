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
package de.gematik.ti20.simsvc.server.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserInfoTest {

  private UserInfo userInfo;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    userInfo = new UserInfo("test-subject", "test-identifier", "test-profession-oid");
    objectMapper = new ObjectMapper();
  }

  @Test
  void testNoArgsConstructor() {
    UserInfo userInfo = new UserInfo();

    assertNull(userInfo.getSubject());
    assertNull(userInfo.getIdentifier());
    assertNull(userInfo.getProfessionOID());
    assertNotNull(userInfo.getAdditionalProperties());
    assertTrue(userInfo.getAdditionalProperties().isEmpty());
  }

  @Test
  void testAllArgsConstructor() {
    Map<String, Object> additionalProps = new HashMap<>();
    additionalProps.put("key1", "value1");

    UserInfo userInfo = new UserInfo("subject", "identifier", "profession-oid", additionalProps);

    assertEquals("subject", userInfo.getSubject());
    assertEquals("identifier", userInfo.getIdentifier());
    assertEquals("profession-oid", userInfo.getProfessionOID());
    assertEquals(additionalProps, userInfo.getAdditionalProperties());
  }

  @Test
  void testThreeArgsConstructor() {
    UserInfo userInfo = new UserInfo("subject", "identifier", "profession-oid");

    assertEquals("subject", userInfo.getSubject());
    assertEquals("identifier", userInfo.getIdentifier());
    assertEquals("profession-oid", userInfo.getProfessionOID());
    assertNotNull(userInfo.getAdditionalProperties());
    assertTrue(userInfo.getAdditionalProperties().isEmpty());
  }

  @Test
  void testGettersAndSetters() {
    userInfo.setSubject("new-subject");
    userInfo.setIdentifier("new-identifier");
    userInfo.setProfessionOID("new-profession-oid");

    assertEquals("new-subject", userInfo.getSubject());
    assertEquals("new-identifier", userInfo.getIdentifier());
    assertEquals("new-profession-oid", userInfo.getProfessionOID());
  }

  @Test
  void testAdditionalProperties() {
    Map<String, Object> properties = userInfo.getAdditionalProperties();
    properties.put("customKey", "customValue");
    properties.put("anotherKey", 123);

    assertEquals("customValue", userInfo.getAdditionalProperties().get("customKey"));
    assertEquals(123, userInfo.getAdditionalProperties().get("anotherKey"));
    assertEquals(2, userInfo.getAdditionalProperties().size());
  }

  @Test
  void testSetAdditionalProperties() {
    Map<String, Object> newProperties = new HashMap<>();
    newProperties.put("prop1", "value1");
    newProperties.put("prop2", 42);

    userInfo.setAdditionalProperties(newProperties);

    assertEquals(newProperties, userInfo.getAdditionalProperties());
    assertEquals("value1", userInfo.getAdditionalProperties().get("prop1"));
    assertEquals(42, userInfo.getAdditionalProperties().get("prop2"));
  }

  @Test
  void testEqualsAndHashCode() {
    UserInfo userInfo1 = new UserInfo("subject", "identifier", "profession-oid");
    UserInfo userInfo2 = new UserInfo("subject", "identifier", "profession-oid");
    UserInfo userInfo3 = new UserInfo("different", "identifier", "profession-oid");

    assertEquals(userInfo1, userInfo2);
    assertEquals(userInfo1.hashCode(), userInfo2.hashCode());
    assertNotEquals(userInfo1, userInfo3);
  }

  @Test
  void testToString() {
    String toString = userInfo.toString();

    assertNotNull(toString);
    assertTrue(toString.contains("UserInfo"));
    assertTrue(toString.contains("test-subject"));
    assertTrue(toString.contains("test-identifier"));
    assertTrue(toString.contains("test-profession-oid"));
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(userInfo);

    assertNotNull(json);
    assertTrue(json.contains("\"subject\":\"test-subject\""));
    assertTrue(json.contains("\"identifier\":\"test-identifier\""));
    assertTrue(json.contains("\"professionOID\":\"test-profession-oid\""));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json =
        "{\"subject\":\"json-subject\",\"identifier\":\"json-identifier\",\"professionOID\":\"json-profession-oid\"}";

    UserInfo deserializedUserInfo = objectMapper.readValue(json, UserInfo.class);

    assertEquals("json-subject", deserializedUserInfo.getSubject());
    assertEquals("json-identifier", deserializedUserInfo.getIdentifier());
    assertEquals("json-profession-oid", deserializedUserInfo.getProfessionOID());
  }

  @Test
  void testJsonSerializationWithNullValues() throws JsonProcessingException {
    UserInfo userInfoWithNulls = new UserInfo();
    userInfoWithNulls.setSubject("test-subject");
    // identifier and professionOID are null

    String json = objectMapper.writeValueAsString(userInfoWithNulls);

    assertTrue(json.contains("\"subject\":\"test-subject\""));
    assertFalse(json.contains("identifier"));
    assertFalse(json.contains("professionOID"));
  }

  @Test
  void testJsonIncludeAnnotation() {
    assertTrue(UserInfo.class.isAnnotationPresent(JsonInclude.class));
    JsonInclude annotation = UserInfo.class.getAnnotation(JsonInclude.class);
    assertEquals(JsonInclude.Include.NON_NULL, annotation.value());
  }

  @Test
  void testJsonPropertyAnnotations() throws NoSuchFieldException {
    var subjectField = UserInfo.class.getDeclaredField("subject");
    var identifierField = UserInfo.class.getDeclaredField("identifier");
    var professionOIDField = UserInfo.class.getDeclaredField("professionOID");
    var additionalPropertiesField = UserInfo.class.getDeclaredField("additionalProperties");

    assertTrue(subjectField.isAnnotationPresent(JsonProperty.class));
    assertTrue(identifierField.isAnnotationPresent(JsonProperty.class));
    assertTrue(professionOIDField.isAnnotationPresent(JsonProperty.class));
    assertTrue(additionalPropertiesField.isAnnotationPresent(JsonProperty.class));

    assertEquals("subject", subjectField.getAnnotation(JsonProperty.class).value());
    assertEquals("identifier", identifierField.getAnnotation(JsonProperty.class).value());
    assertEquals("professionOID", professionOIDField.getAnnotation(JsonProperty.class).value());
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.simsvc.server.model", UserInfo.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(UserInfo.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    var subjectField = UserInfo.class.getDeclaredField("subject");
    var identifierField = UserInfo.class.getDeclaredField("identifier");
    var professionOIDField = UserInfo.class.getDeclaredField("professionOID");
    var additionalPropertiesField = UserInfo.class.getDeclaredField("additionalProperties");

    assertTrue(java.lang.reflect.Modifier.isPrivate(subjectField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(identifierField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(professionOIDField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(additionalPropertiesField.getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(String.class, UserInfo.class.getDeclaredField("subject").getType());
    assertEquals(String.class, UserInfo.class.getDeclaredField("identifier").getType());
    assertEquals(String.class, UserInfo.class.getDeclaredField("professionOID").getType());
    assertEquals(Map.class, UserInfo.class.getDeclaredField("additionalProperties").getType());
  }

  @Test
  void testAdditionalPropertiesDefaultInitialization() {
    UserInfo userInfo = new UserInfo();

    assertNotNull(userInfo.getAdditionalProperties());
    assertTrue(userInfo.getAdditionalProperties() instanceof HashMap);
    assertTrue(userInfo.getAdditionalProperties().isEmpty());
  }

  @Test
  void testConstructorCount() {
    var constructors = UserInfo.class.getDeclaredConstructors();
    assertEquals(3, constructors.length);
  }

  @Test
  void testNullSafety() {
    UserInfo userInfo = new UserInfo();
    userInfo.setSubject(null);
    userInfo.setIdentifier(null);
    userInfo.setProfessionOID(null);
    userInfo.setAdditionalProperties(null);

    assertNull(userInfo.getSubject());
    assertNull(userInfo.getIdentifier());
    assertNull(userInfo.getProfessionOID());
    assertNull(userInfo.getAdditionalProperties());
  }
}
