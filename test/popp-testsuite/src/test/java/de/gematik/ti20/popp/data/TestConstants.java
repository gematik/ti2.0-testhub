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
  public static final String VALID_POPP_TOKEN_JSON_RESPONSE_FILE =
      BLUEPRINT_FOLDER + "poppTokenResponse.json";
  public static final String VALID_POPP_TOKEN_HEADER_CLAIMS_FILE =
      BLUEPRINT_FOLDER + "poppTokenHeaderClaims.json";
  public static final String VALID_POPP_TOKEN_BODY_CLAIMS_FILE =
      BLUEPRINT_FOLDER + "poppTokenBodyClaims.json";
  public static final String VALID_HASH_DB_IMPORT_RESPONSE_FILE =
      BLUEPRINT_FOLDER + "hashDbImportResponse.json";
  public static final String VALID_HASH_DB_JOB_STATUS_RESPONSE_FILE =
      BLUEPRINT_FOLDER + "hashDbJobStatusResponse.json";
  public static final String VALID_APDU_SEQUENCE_0_FILE =
      BLUEPRINT_FOLDER + "poppApdusSequence0.json";
  public static final String VALID_APDU_SEQUENCE_1_FILE =
      BLUEPRINT_FOLDER + "poppApdusSequence1.json";
  public static final String VALID_APDU_SEQUENCE_2_FILE =
      BLUEPRINT_FOLDER + "poppApdusSequence2.json";
  public static final String VALID_APDU_SEQUENCE_3_FILE =
      BLUEPRINT_FOLDER + "poppApdusSequence3.json";
  public static final String VALID_APDU_SEQUENCE_2_CONNECTOR_FILE =
      BLUEPRINT_FOLDER + "poppApdusSequence2Connector.json";

  public static final long MAX_AGE_POPP_TOKEN_IN_SECONDS = 300;
  public static final String ARBITRARY_VALUE_NOT_AFTER_FOR_HASHDB_ENTRIES = "281231235959Z";
  public static final int VERSION_E_CONTENT = 0;

  private static final String POPP_SERVICE_BASE_URL_RU =
      "https://popp-ru-dev-interim.int.epa.rise-link.de:8443";

  private static final String HASH_DB_IMPORT_PATH = "/api/v1/hash-db/import";

  public static final String URL_HASH_DB_IMPORT_RU = POPP_SERVICE_BASE_URL_RU + HASH_DB_IMPORT_PATH;

  // Test Identities
  public static final String PATH_TO_TSP_EGK_OSIG_P12 =
      "../../no-publish/test-data/p12/popp-testsuite/tsp.egk.gematik.solutions-osig-nist-popp-komp61.p12";
}
