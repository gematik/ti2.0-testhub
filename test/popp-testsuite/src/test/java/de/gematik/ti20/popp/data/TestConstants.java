/*
 *
 * Copyright 2026 gematik GmbH
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

package de.gematik.ti20.popp.data;

public class TestConstants {

  private static final String BLUEPRINT_FOLDER = "src/test/resources/blueprints/";
  public static final String VALID_POPP_TOKEN_JSON_RESPONSE =
      BLUEPRINT_FOLDER + "poppTokenResponse.json";
  public static final String VALID_POPP_TOKEN_HEADER_CLAIMS =
      BLUEPRINT_FOLDER + "poppTokenHeaderClaims.json";
  public static final String VALID_POPP_TOKEN_BODY_CLAIMS =
      BLUEPRINT_FOLDER + "poppTokenBodyClaims.json";

  public static final long MAX_AGE_POPP_TOKEN_IN_SECONDS = 300;
}
