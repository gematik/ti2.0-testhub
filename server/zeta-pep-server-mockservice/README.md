<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# ZeTA PEP Server MockService

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

The ZeTA PEP Server MockService (zeta-pep-server-mockservice) serves as a sophisticated simulation of the ZeTA Policy
Enforcement Point (ZeTA PEP) for the TI 2.0 ecosystem. This service acts as an intelligent HTTP and WebSocket proxy with
advanced token validation and header transformation capabilities.

**Key Features:**

- **HTTP & WebSocket Proxying**: Forwards requests to backend services while transforming authentication headers
- **OAuth 2.0 Token Validation**: Validates Access Tokens using JWT with keystore-based verification
- **PoPP Token Support**: Optional Proof-of-Possession token validation with actor ID verification
- **Header Transformation**: Converts Authorization headers to ZeTA-specific headers (ZETA-User-Info,
  ZETA-PoPP-Token-Content)
- **Base64 JSON Encoding**: Transforms token claims into URL-safe Base64 encoded JSON format
- **Service Status Monitoring**: Built-in health check endpoint

This MockService is primarily intended for testing PoPP and VSDM2 scenarios and will eventually be replaced by the
actual ZeTA Guard (Zero Trust Cluster) components.

For the complete ZeTA architecture specification, visit
the [gemSpecPages](https://gemspec.gematik.de/docs/gemSpec/gemSpec_ZETA/latest/).

### Implementation Status

**Implemented Features:**

- ✅ HTTP proxy functionality with request forwarding
- ✅ WebSocket proxy support for real-time communication
- ✅ OAuth 2.0 Access Token validation using JWT with X509 certificate verification
- ✅ Optional PoPP (Proof-of-Possession) token validation with certificate chain validation
- ✅ Header transformation (Authorization → ZETA-User-Info, ZETA-PoPP-Token → ZETA-PoPP-Token-Content)
- ✅ Base64 JSON encoding of token claims (UserInfo structure with subject, identifier, professionOID)
- ✅ Actor ID verification between Access Token and PoPP Token
- ✅ Service status endpoint (`/service/status`)
- ✅ Well-known OAuth endpoint (`/.well-known/oauth-protected-resource`)
- ✅ PKCS12 keystore integration for cryptographic operations
- ✅ Professional OID extraction from tokens
- ✅ Token expiration validation (hardcoded to 5 minutes for created tokens)
- ✅ Comprehensive error handling with proper HTTP status codes (UNAUTHORIZED, BAD_REQUEST, BAD_GATEWAY)

**Missing Implementation / Limitations:**

- ❌ ZETA-Client-Data header contains placeholder "TODO ZTA Client Data"
- ❌ Service status endpoint returns empty response (200 OK) instead of service information
- ❌ Security configuration not exposed in default application.yaml (only via environment variables)
- ❌ Audience validation is bypassed (`setSkipDefaultAudienceValidation()`) in AccessToken validation
- ❌ PoPP token certificate chain validation is disabled (FIXME comment - validation skipped for testing)
- ❌ Limited error details in proxy forwarding failures (generic BAD_GATEWAY messages)
- ❌ No audit logging for security events
- ❌ No rate limiting or DoS protection

## Installation

### Prerequisites

- Java 21
- Maven 3.6 or higher
- PKCS12 keystore file for token validation

### Build the Project

```bash
mvn clean install
```

## Getting Started

### Quick Start

1. **Run the service:**
   ```bash
   mvn spring-boot:run
   ```

2. **Using Docker:**
   ```bash
   ./bin/docker-build.sh
   docker run -p 80:80 zeta-pep-server-mockservice
   ```

The service will start on port 80 by default (configurable via `SERVER_PORT` environment variable).

## Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the
(latest) releases.

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for information about changes.

## Configuration

All configuration parameters can be set in the `application.yaml` file located in the `src/main/resources` directory or
via environment variables.

### Core Configuration

| Property              | Environment Variable  | Default    | Description                                    |
|:----------------------|:----------------------|:-----------|:-----------------------------------------------|
| `server.port`         | `SERVER_PORT`         | `80`       | Server port                                    |
| `proxy.http.url`      | `PROXY_HTTP_URL`      | *Required* | HTTP URL of the backend server                 |
| `proxy.ws.url`        | `PROXY_WS_URL`        | `""`       | WebSocket URL of the backend server (optional) |
| `proxy.popp.required` | `PROXY_POPP_REQUIRED` | `false`    | Whether PoPP token validation is required      |

### Security Configuration (Not Exposed in Default Config)

The service supports PKCS12 keystore configuration through SecurityConfig, but these properties are not included in the
default application.yaml:

| Property               | Description                  |
|:-----------------------|:-----------------------------|
| `proxy.sec.store.path` | Path to PKCS12 keystore file |
| `proxy.sec.store.pass` | Keystore password            |
| `proxy.sec.key.alias`  | Key alias in keystore        |
| `proxy.sec.key.pass`   | Private key password         |

### Well-Known Endpoint Configuration

| Property              | Environment Variable | Description            |
|:----------------------|:---------------------|:-----------------------|
| `well-known.issuer`   | `WK_ISSUER`          | OAuth 2.0 issuer URL   |
| `well-known.auth_ep`  | `WK_AUTH_EP`         | Authorization endpoint |
| `well-known.token_ep` | `WK_TOKEN_EP`        | Token endpoint         |
| `well-known.nonce_ep` | `WK_NONCE_EP`        | Nonce endpoint         |
| `well-known.jwks_uri` | `WK_JWKS_URI`        | JWKS URI               |

### Example Configuration

Create an `application-local-test.yaml` file for local testing:

```yaml
server:
  port: 9103

proxy:
  http:
    url: http://localhost:8080
  ws:
    url: ws://localhost:8080/ws
  popp:
    required: true

well-known:
  issuer: https://auth.example.com
  auth_ep: https://auth.example.com/auth
  token_ep: https://auth.example.com/token
  nonce_ep: https://auth.example.com/nonce
  jwks_uri: https://auth.example.com/jwks
```

Run with: `mvn spring-boot:run -Dspring.profiles.active=local-test`

## Endpoints

| Method | Path                                    | Description                                                  |
|:-------|:----------------------------------------|:-------------------------------------------------------------|
| `GET`  | `/service/status`                       | Returns basic service status (currently empty response)      |
| `GET`  | `/.well-known/oauth-protected-resource` | OAuth 2.0 well-known endpoint configuration                  |
| `ALL`  | `/ws/**`                                | WebSocket proxy - forwards to backend WebSocket URL          |
| `ALL`  | `/**`                                   | HTTP proxy - forwards all other requests to backend HTTP URL |

### Authentication Flow

1. **Access Token**: Required in `Authorization: Bearer <token>` header
2. **PoPP Token**: Optional in `ZETA-PoPP-Token` header (if `proxy.popp.required=true`)
3. **Token Validation**:
    - **Access Token**: Validated using X509 certificate resolver from keystore
    - **PoPP Token**: Certificate chain extracted from x5c header and validated (currently bypassed with FIXME)
    - **Actor ID Check**: PoPP token's actorId must match Access Token's clientId
4. **Header Transformation**: Service transforms headers before forwarding:
    - `Authorization: Bearer <jwt>` → `ZETA-User-Info: <base64_user_info>`
    - `ZETA-PoPP-Token: <jwt>` → `ZETA-PoPP-Token-Content: <base64_popp_claims>`
    - Adds: `ZETA-Client-Data: TODO ZTA Client Data` (placeholder implementation)

**UserInfo Structure** (Base64 encoded JSON):

```json
{
  "subject": "string",
  "identifier": "string",
  "professionOID": "string"
}
```

## Examples

### 1. Service Status Check

```bash
curl -X GET 'http://localhost:80/service/status' \
  -H 'Accept: application/json'
```

**Response:**

```
200 OK (Empty body)
```

### 2. Well-Known OAuth Configuration

```bash
curl -X GET 'http://localhost:80/.well-known/oauth-protected-resource' \
  -H 'Accept: application/json'
```

**Response:**

```json
{
  "issuer": "https://auth.example.com",
  "authorization_endpoint": "https://auth.example.com/auth",
  "token_endpoint": "https://auth.example.com/token",
  "nonce_endpoint": "https://auth.example.com/nonce",
  "jwks_uri": "https://auth.example.com/jwks",
  "openid_providers_endpoint": "https://idp.app.ti-dienste.de/directory/fed_idp_list",
  "scopes_supported": [
    "zero:register",
    "zero:manage"
  ],
  "response_types_supported": [
    "code"
  ],
  "grant_types_supported": [
    "authorization_code"
  ]
}
```

### 3. Authenticated Request (Access Token Only)

```bash
curl -X GET 'http://localhost:80/api/data' \
  -H 'Authorization: Bearer eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'Accept: application/json'
```

The service will:

1. Validate the JWT token using X509 certificate verification from keystore
2. Extract UserInfo (subject, identifier/clientId, professionOID) from token claims
3. Transform the Authorization header to ZETA-User-Info with Base64-encoded UserInfo JSON
4. Add ZETA-Client-Data header with placeholder value ("TODO ZTA Client Data")
5. Forward the request to the backend with transformed headers

**Note:** Audience validation is bypassed (`setSkipDefaultAudienceValidation()`) for testing purposes.

### 4. Authenticated Request (Access Token + PoPP Token)

```bash
curl -X GET 'http://localhost:80/api/secure-data' \
  -H 'Authorization: Bearer eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'ZETA-PoPP-Token: eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'Accept: application/json'
```

The service will:

1. Validate both Access Token (X509) and PoPP Token (certificate chain from x5c header)
2. Verify that PoPP token's actorId matches Access Token's clientId
3. Extract UserInfo from Access Token and PoPP claims from PoPP Token
4. Transform both headers:
    - Authorization → ZETA-User-Info (Base64 UserInfo JSON)
    - ZETA-PoPP-Token → ZETA-PoPP-Token-Content (Base64 PoPP claims JSON)
5. Add ZETA-Client-Data header with placeholder value
6. Forward the request to the backend

**Note:** PoPP certificate chain validation is currently bypassed (FIXME in code) for testing purposes.

### Token Structures

**Access Token Claims** (validated and extracted):

```json
{
  "iss": "string",
  "aud": [
    "string"
  ],
  "sub": "string",
  "client_id": "string",
  "scope": "string",
  "cnf": {
    "jkt": "string"
  },
  "profession_oid": "string",
  "exp": 1234567890,
  "iat": 1234567890,
  "jti": "string"
}
```

**PoPP Token Claims** (validated and extracted):

```json
{
  "version": "string",
  "iss": "string",
  "iat": 1234567890,
  "proofMethod": "string",
  "patientProofTime": 1234567890,
  "patientId": "string",
  "insurerId": "string",
  "actorId": "string",
  "actorProfessionOid": "string"
}
```

**Important Notes:**

- Access Token uses X509VerificationKeyResolver with certificates from keystore
- PoPP Token validates certificate chain from x5c header (validation currently bypassed)
- Audience validation is skipped for Access Token (`setSkipDefaultAudienceValidation()`)
- Actor ID must match between tokens when PoPP is required

### Error Scenarios

**401 UNAUTHORIZED:**

- Missing or invalid Authorization header
- JWT token validation fails
- Missing PoPP token when `proxy.popp.required=true`
- Actor ID mismatch between Access Token and PoPP Token

**400 BAD_REQUEST:**

- Missing PoPP token when required
- Invalid token format

**502 BAD_GATEWAY:**

- Backend service unavailable
- Proxy forwarding fails
- Generic error processing request

## Folder Structure

| Folder                | Content                           |
|:----------------------|:----------------------------------|
| `bin/`                | Build and deployment scripts      |
| `docker/`             | Docker configuration files        |
| `src/main/java/`      | Java source code                  |
| `src/main/resources/` | Configuration files and resources |
| `src/test/java/`      | Unit and integration tests        |

## Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the
(latest) releases.

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for information about changes.
