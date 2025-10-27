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
package de.gematik.ti20.client.zeta.auth;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

public class ClientAssertionToken extends JsonWebSignature {

  public static ClientAssertionToken create(String nonce, KeyPair dpopKeyPair)
      throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] publicKeyHash = digest.digest(dpopKeyPair.getPublic().getEncoded());
    String jkt = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKeyHash);

    JwtClaims claims = new JwtClaims();
    claims.setClaim("name", "Client Instance Name");
    claims.setClaim("client_id", "client-123");
    claims.setClaim("product_id", "product-456");
    claims.setClaim("product_name", "Product Name");
    claims.setClaim("product_version", "1.0.0");
    claims.setClaim("manufacturer_id", "manufacturer-789");
    claims.setClaim("manufacturer_name", "Manufacturer Name");
    claims.setClaim("owner", "Owner Information");
    claims.setClaim("owner_mail", "owner@example.com");
    claims.setClaim("registration_timestamp", System.currentTimeMillis() / 1000);
    claims.setClaim("platform", "Platform Name");
    claims.setClaim("platform_product_id", "platform-product-id");
    claims.setClaim("posture", "Posture Information");
    claims.setClaim("attestation", "Attestation Information");
    claims.setClaim("jkt", jkt);

    ClientAssertionToken token = new ClientAssertionToken();
    token.setPayload(claims.toJson());
    token.setAlgorithmHeaderValue("HS256"); // HMAC mit SHA-256
    // card.setKey(new HmacKey(key));

    return token;
  }
}
