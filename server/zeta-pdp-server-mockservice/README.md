<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# ZeTA PDP Server MockService

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a></li>
    <li><a href="#installation">Installation</a></li>
    <li><a href="#getting-started">Getting Started</a></li>
    <li><a href="#configuration">Configuration</a></li>
    <li><a href="#endpoints">Endpoints</a></li>
    <li><a href="#examples">Examples</a></li>
    <li><a href="#folder-structure">Folder Structure</a></li>
    <li><a href="#release-notes">Release Notes</a></li>
    <li><a href="#changelog">Changelog</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

## About the Project

The ZeTA PDP Server MockService (zeta-pdp-server-mockservice) serves as a limited imitation of the ZeTA Policy Decision
Point (ZeTA PDP) within the Zero Trust Architecture (ZeTA) framework.
It replicates only some selected aspects of the ZeTA PDP and is primarily intended for temporary use in testing PoPP and
VSDM2 integration scenarios.

This MockService provides essential policy decision functionality to enable end-to-end testing of the TI 2.0 ecosystem
without requiring the full ZeTA Guard infrastructure. It will be replaced by the actual components of the ZeTA Guard
(Zero Trust Cluster) once they are provided by the manufacturer.

For the complete ZeTA architecture specification, visit
the [gemSpecPages](https://gemspec.gematik.de/docs/gemSpec/gemSpec_ZETA/latest/).

### Implementation Status

**Implemented Features:**

- ✅ OAuth 2.0 Token Exchange endpoint (`POST /token`) with RFC 8693 support
- ✅ Service status monitoring (`GET /service/status`)
- ✅ JWT token creation with proper signing (ES256 algorithm)
- ✅ PKCS12 keystore integration for cryptographic operations
- ✅ SMC-B access token processing and validation (with signature verification bypass for testing)
- ✅ Professional OID extraction and forwarding from SMC-B tokens
- ✅ Telematik-specific JWT format (`vnd.telematik.access+jwt`)
- ✅ Token exchange flow for ZeTA PDP simulation
- ✅ Form-encoded request handling
- ✅ Configurable security parameters via application profiles

**Not Implemented / Limitations:**

- ❌ Full signature verification (bypassed for mock purposes with `setSkipSignatureVerification()`)
- ❌ Audience validation (bypassed with `setSkipDefaultAudienceValidation()`)
- ❌ Real policy evaluation engine
- ❌ Production-grade security validation
- ❌ Client assertion validation
- ❌ Basic error handling - throws RuntimeException for any service error
- ❌ Token revocation mechanisms
- ❌ Refresh token implementation (returns placeholder "TODO REFRESHTOKEN")
- ❌ Token expiration time is hardcoded to 5 minutes instead of configurable
- ❌ Empty/incomplete client_id, scope, and jkt fields in generated tokens
- ❌ Service status endpoint returns empty body instead of actual status information
- ❌ Audit logging and monitoring
- ❌ Rate limiting and DoS protection

**Note:** This is a sophisticated mock service that implements core ZeTA PDP token exchange functionality but bypasses
security validations for testing purposes. It should never be used in production environments.

## Installation

### Prerequisites

- Java 21
- Maven 3.6 or higher
- Valid keystore with security certificates

### Build from Source

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd zeta-pdp-server-mockservice
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Build without tests (faster)**
   ```bash
   mvn clean install -DskipTests
   ```

### Docker Build

Use the provided script to build a Docker image:

```bash
./bin/docker-build.sh
```

## Getting Started

### Starting the Service

**Option 1: Run with Maven**

```bash
mvn spring-boot:run
```

**Option 2: Run with local profile**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Option 3: Run the JAR file**

```bash
java -jar target/zeta-pdp-server-mockservice-*.jar
```

**Option 4: Use Docker**

```bash
docker run -p 9102:9102 zeta-pdp-server-mockservice
```

**Option 5: Integrated Setup**
The zeta-pdp-server-mockservice will automatically be built and started when the rebuild/restart script for the VSDM
test setup is executed. (See the README.md in the root directory of the repository for more information.)

### Verify Installation

Once the server is running:

1. **Check service status:**
   ```bash
   curl http://localhost:9102/service/status
   ```

   Expected response: `200 OK` with empty body

2. **Access Swagger UI:**
   Open http://localhost:9102/swagger-ui/index.html in your browser to explore the API via the integrated OpenAPI UI.

3. **View OpenAPI specification:**
   ```bash
   curl http://localhost:9102/openapi.yaml
   ```

### Quick Test

Test the token exchange endpoint (requires valid SMC-B token):

```bash
curl -X 'POST' \
  'http://localhost:9102/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=urn:ietf:params:oauth:grant-type:token-exchange&subject_token=VALID_SMC_B_TOKEN&subject_token_type=urn:ietf:params:oauth:token-type:access_token'
```

**Expected Response:**

```json
{
  "access_token": "eyJhbGciOiJFUzI1NiIs...",
  "refreshToken": "TODO REFRESHTOKEN"
}
```

**Note:** You need a valid SMC-B JWT token with a `professionOid` claim to test the endpoint successfully. The service
will throw a RuntimeException if the token is missing or malformed.

## Configuration

All configuration parameters for the project can be set in the `application.yaml` file located in the
`src/main/resources` directory. You can set the server port by modifying the `server.port` property.

### Security Configuration

The service requires keystore configuration for token signing:

| Name                 | Description                                                                  |
|:---------------------|------------------------------------------------------------------------------|
| authz.sec.store.path | Specifies the file path to the keystore containing the security certificates |
| authz.sec.store.pass | Password to access the keystore file                                         |
| authz.sec.key.alias  | Alias name for the key used within the keystore                              |
| authz.sec.key.pass   | Password for accessing the specific key in the keystore                      |

### Example Configuration

An example configuration is provided in the `application-local-test.yaml` file:

```yaml
server:
  port: 9102

authz:
  sec:
    store:
      path: "./zetakeystore.p12"
      pass: "testpassword"
    key:
      alias: "zetamock"
      pass: "testpassword"

logging:
  level:
    root: INFO
    de.gematik: DEBUG
```

To use this configuration, specify the `spring.profiles.active=local` property when starting the server.

## Endpoints

The server exposes the following endpoints:

| Endpoint          | Method | Description                                                                       |
|:------------------|:-------|:----------------------------------------------------------------------------------|
| `/token`          | POST   | OAuth 2.0 token exchange endpoint - generates JWT access tokens from SMC-B tokens |
| `/service/status` | GET    | Health check endpoint - returns 200 OK with empty body                            |
| `/openapi.yaml`   | GET    | OpenAPI specification endpoint - serves the API documentation                     |

## Examples

### Token Exchange (OAuth 2.0 RFC 8693)

#### ZeTA PDP Token Exchange Request

```bash
curl -X 'POST' \
  'http://localhost:9102/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'accept: */*' \
  -d 'grant_type=urn:ietf:params:oauth:grant-type:token-exchange&requested_token_type=urn:ietf:params:oauth:token-type:access_token&subject_token=eyJhbGciOiJFUzI1NiIsInR5cCI6InZuZC50ZWxlbWF0aWsuYWNjZXNzK2p3dCJ9...&subject_token_type=urn:ietf:params:oauth:token-type:access_token'
```

**Parameters:**

- `grant_type`: `urn:ietf:params:oauth:grant-type:token-exchange` (RFC 8693)
- `requested_token_type`: `urn:ietf:params:oauth:token-type:access_token`
- `subject_token`: Valid SMC-B access token (JWT format)
- `subject_token_type`: `urn:ietf:params:oauth:token-type:access_token`

**Response:**

```json
{
  "access_token": "eyJhbGciOiJFUzI1NiIsInR5cCI6InZuZC50ZWxlbWF0aWsuYWNjZXNzK2p3dCIsImtpZCI6InpldGFtb2NrIn0.eyJpc3MiOiJQRFAtTW9ja1NlcnZpY2UiLCJhdWQiOlsiUEVQLU1vY2tTZXJ2aWNlIl0sInN1YiI6IjEtSE1ULVRlc3QiLCJjbGllbnRfaWQiOiIiLCJzY29wZSI6IiIsImprdCI6IiIsInByb2Zlc3Npb25PaWQiOiIxLjIuMjc2LjAuNzYuNC40OSJ9.signature",
  "refreshToken": "TODO REFRESHTOKEN"
}
```

#### Decoded Token Structure

The returned JWT contains these claims:

```json
{
  "iss": "PDP-MockService",
  "aud": [
    "PEP-MockService"
  ],
  "sub": "1-HMT-Test",
  "client_id": "",
  "scope": "",
  "cnf": {
    "jkt": ""
  },
  "profession_oid": "1.2.276.0.76.4.49",
  "exp": 1234567890,
  "iat": 1234567590,
  "jti": "unique-jwt-id"
}
```

**Important Notes:**

- The service processes SMC-B tokens to extract `professionOid` and `subject` from the input token
- Signature verification is **bypassed** with `setSkipSignatureVerification()` for testing purposes
- Audience validation is **bypassed** with `setSkipDefaultAudienceValidation()`
- Token expiration is **hardcoded** to 5 minutes from issuance
- `client_id`, `scope`, and `jkt` fields are intentionally empty strings in this mock implementation
- The returned token is properly signed with **ES256** using the configured keystore

### Service Status

```bash
curl -X 'GET' \
  'http://localhost:9102/service/status' \
  -H 'accept: */*'
```

**Response:**

```
200 OK (empty body)
```

**Note:** The service status endpoint returns only an HTTP 200 status code without a JSON response body.

### Error Handling

#### Missing Subject Token

```bash
curl -X 'POST' \
  'http://localhost:9102/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=urn:ietf:params:oauth:grant-type:token-exchange'
```

**Response:**

```
500 Internal Server Error
```

The service throws a `RuntimeException` wrapping any exception from token processing. This includes:

- Missing `subject_token` parameter
- Invalid JWT format
- Missing required claims (e.g., `professionOid`)
- JWT parsing errors

**Note:** Error handling is basic - all exceptions are wrapped in RuntimeException without detailed error messages in
the response.

#### Port Configuration Note

The actual service default port is **80** with configuration options:

- **Default:** port 80 (via `SERVER_PORT` environment variable with fallback)
- **Local test profile:** port 9102 (in `application-local-test.yaml`)
- **OpenAPI spec:** documents port 9102 (in `openapi.yaml`)

Run with custom port:

```bash
SERVER_PORT=9102 mvn spring-boot:run
# or
mvn spring-boot:run -Dspring-boot.run.profiles=local-test
```

**Note:** The service performs actual JWT processing and will fail if required parameters are missing, unlike a simple
always-successful mock.

## Folder Structure

This project has the following folders:

| Folder | Content                               |
|:-------|---------------------------------------|
| bin    | scripts relating to the build process |
| docker | docker file                           |
| src    | source files of the project           | 

## Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the
(latest) releases.

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for information about changes.

## Contributing

If you want to contribute, please check our [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

Copyright 2025 gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
