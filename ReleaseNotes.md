<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# Release Notes TI 2.0 TestHub

## Release 3.13.0

- TESTHUB-202: update FHIR schema to match latest version of VSDM2 specification 1.1.0
- TESTHUB-205: allow for multiple SMCB certificates in the VSDM2 load tests
- TESTHUB-208: avoid writing resolv.conf in the host system

## Release 3.12.0

### Changes

- PTVSDM-1690: add preprocessing and use of etags in load tests for VSDM2 server implementation
- TESTHUB-201: merge popp-clients of erp-testsuite and reuse existing popp-client
- TESTHUB-210: make server validation configurable for zeta sdk client
- TESTHUB-221: Update PoPP dependencies to 2.7.1 (contains Zeta Guard 1.2.3 and ZETA SDK 1.2.5)
- ZTI-4568: Added websocket-upgrade tests for ZETA testsuite
- ZTI-4335: Added testcase that uses a real connector for popp token retrieval
- ZTI-4346: Added GIT-TI test for ZETA

## Release 3.11.0

### Update Notes

We removed the `INTERCEPT_STORAGE` configuration that was used to store token information. This
configuration was meant to be used for load tests, but since we have to use ASL it is not possible
anymore to create tokens on the fly. To change the storage configuration can have unwanted side
effects, so we removed the configuration to avoid confusion.

We are still experiencing a regression with ASL decryption in the Tiger Logs.

### Changes

- ANFTI2-731: Mention known issue with test certificates for ASL
- TESTHUB-206: remove INTERCEPT_STORAGE configuration that cannot be used for load tests anymore

## Release 3.10.0

### Changes

- TESTHUB-185: updated load test scaffolding to work with active ASL channel
- TESTHUB-198: fix error message for invalid if-none-match header in vsdm-server
- TESTHUB-199: fix backend-only profile to start all the backend components
- changed zeta-popp certificates (RSA to ECC migration because of TR-02102-1)
- Dependency Updates:
    - org.bouncycastle:bcprov-jdk18on from 1.84 to 1.85
    - org.bouncycastle:bcpkix-jdk18on from 1.84 to 1.85
    - ch.qos.logback:logback-core from 1.5.38 to 1.6.0
- Updated OCSP signers list
- ZTI-4346: Added GIT-TI test for ZETA

## Release 3.9.0

### Changes

- TESTHUB-165: refactor retrieval of popp tokens
- various bugfixes

## Release 3.8.0

### Update Notes

#### Canopy replaces Docker network construct

Previously we used complicated Docker network configurations to get our services to use Tiger
proxies for traffic recording. To simplify the setup we are now using Canopy. Canopy is a DNS server
that helps manage the network traffic between services without us having to touch Docker networks.

### Changes

- TESTHUB-178: simplify Tiger proxy configuration by introducing Canopy DNS server
- TESTHUB-190: Update PoPP dependencies to 2.7.0
- TESTHUB-194: (vsdm) Update ZETA client to 1.2.5
- Dependency Updates:
    - gatling-maven-plugin from 4.21.5 to 4.21.8
    - logback:logback-core from 1.5.32 to 1.5.38

## Release 3.7.0

### Changes

- TESTHUB-184: add support for eGK G2.1, PTV_4.7.0 card images in card terminal client
- TESTHUB-186: Upgrade ZETA components (VSDM)

## Release 3.6.0

### Changes

- TESTHUB-112: add documentation about how to use podman
- TESTHUB-165: refactor retrieval of popp tokens
- TESTHUB-169: update BDE messages and resolve placeholder values in vsdm-testsuite
- TESTHUB-175: add documentation about testing configuration with .env files
- TESTHUB-181: update Popp components to use Zeta 1.2.x
- PTVSDM-1637: load test preparations for provider approval phase

## Release 3.5.0

### Update Notes

#### Policy changes for VSDM service

OPA is used to enforce authorization policies. In this version we have added a policy to verify that
authorization tokens use the scope defined in the VSDM 2.0 specification: `vsdservice`. Ensure you
are updating custom code that uses the old `zero:audience` scope. Otherwise, you will see error
messages mentioning an invalid, unknown or forbidden scope.

The PDP (Keycloak) configuration was adjusted too.

### Changes

- TESTHUB-99: simplify proxy env variable definition in Docker Compose
- TESTHUB-164: add optional virtualCard parameter in request to popp-client
- TESTHUB-166: remove unused smcSlotId parameter from vsdm-client interface
- TESTHUB-172: update OPA policies for vsdm-server to validate scope in token
- PTVSDM-1632: make profileVersion parameter optional in vsdm-client to allow for error condition

## Release 3.4.0

### Update Notes

#### ZETA-Guard changes regarding certificate validation

With the version 1.2.0 ZETA-Guard is configured to validate the SMCB certificate that is used. In
case you want to disable the validation refer to the user manual. Error messages regarding invalid
certificates can be found in the logs of the VSDM PDP container.

### Changes

- TESTHUB-156: Upgrade to ZETA 1.2.0 (VSDM)

## Release 3.3.0

### Changes

- TESTHUB-147: simplify setup of performance profile
- TESTHUB-152: simplify feature test parametrization and component startup
- TESTHUB-154: ensure if-none-match header is quoted

## Release 3.2.0

### Update Notes

For licensing reasons the docker image names were changed. Make sure to remove the old images to
avoid confusion.

### Changes

- TESTHUB-134: Clarify documentation for configuration of external VSDM server
- TESTHUB-148: Rename docker images to follow the pattern `testhub-<component>`
- TESTHUB-150: Clarify documentation for configuration of external Popp server
- TESTHUB-153: Bump Tiger version to 4.3.0
- PTVSDM-1605: Publishing VSDM error tests on GitHub
- PTVSDM-1619: Correcting VSDM coverage assertion
- PTVSDM-1620: Fix handling of Zeta related error: missing or invalid zeta-popp-token,
  zeta-user-info
- PTVSDM-1621: Configure charset for VSDM server responses
- ZTI-4062: Add performance tests to ZETA Testsuite
- TESTHUB-18: VSDM Integration Tests in CI

## Release 3.1.0

### Changes

- TESTHUB-145: Fix classpath issues with Gatling tests in VSDM testsuite
- TESTHUB-146: Fix handling of actor ID in VSDM load tests
- PTVSDM-1618: Fix handling of FHIR profile versions in VSDM2 server implementation

## Release 3.0.0

### Changes

- TESTHUB-132: Update Spring Boot to 4.0.6 and introduce spring-boot-dependencies (BOM), several
  more dependency updates
- TESTHUB-137: Add Delete endpoint to clear PoppToken-Cache

## Release 2.5.0

### Changes

- TESTHUB-136: Integrate Zeta Guard 1.0.1 (MS4)
- TESTHUB-129: Remove mock-popp profiles because it is similar to the perf profile
- TESTHUB-122: Profiles documentation
- TESTHUB-121: Update to latest version 2.3.0 of the Popp Sample Implementation
- TKK-3899: Read popp server url from env-file, refactor jwt-validation
- TKK-3877: Added Testcases for EntityStatement and JWKS
- TKK-3791: Enable integration of Real-World Popp-servers via tiger-proxy
- PTVSDM-1585: Introduce an endpoint supporting FHIR profile versions
- ZTI-4059: Add asl Tests to Zeta Testsuite

## Release 2.4.0

### Changes

- TESTHUB-126: Update VsdmBackgroundLoadSimulation to send load directly to the Zeta guard using
  access and dpop tokens.
- TESTHUB-127: Update project dependencies to latest versions.
- PTVSDM-1585: update VSDM2 server implementation with required profileVersion parameter

## Release 2.3.0

### Update Notes

#### Support for JSON Card Data in card-terminal-client

This change allows the use of card data in JSON format within the card-terminal-client component
instead only the premade XML card images. Tests can now change the data to any desired value.
Examples of valid JSON card data can be found in the
`test/vsdm-testsuite/src/test/resource/data/cards` directory.

### Changes

- TESTHUB-117: extract port configuration to a separate file. The port configuration can be found in
  `doc/docker/.env`.
- TESTHUB-123: allow JSON card data in card-terminal-client
- PTVSDM-1598: update Fhir schema to match latest version of VSDM2 specification 1.0.0

## Release 2.2.0

### Update Notes

#### Upgrade to ZETA 0.5.x (PoPP)

When upgrading your installation you might encounter issues with a database migration in the PoPP
stack. To fix the issue, delete the ZETA *PoPP* PDP database volume:

1. Ensure the relevant containers are stopped
2. Delete the volume `testhub-local_popp-zeta-postgres-db-data`, e.g.:
    ```
    docker volume rm testhub-local_popp-zeta-postgres-db-data
    ```

#### JWKS Endpoint compatibility

The popp-sample-implementation currently only supports the legacy `/jwks.json`
endpoint. If you haven't changed the default PoPP stack configuration, then
popp-sample-implementation and popp-token-generator use the same key material and
popp-token-generator can be used for the `pep_popp_issuer` setting.

See *Known Issues* in the user manual.

#### ASL for VSDM

This release enables ASL for the communication of the VSDM components. The behavior is activated by
default to follow the VSDM specification. Tiger does automatically decrypt the traffic when running
E2E tests for VSDM. Visit [the Testhub user manual](https://gematik.github.io/ti2.0-testhub/) to
learn how to disable ASL.

### Changes

- TESTHUB-26: Enable ASL by default for VSDM ZETA.
- TESTHUB-96: Update popp-client/popp-server dependencies to use latest version of Zeta (0.5.x).
- TESTHUB-107: add traffic protocol for E2E tests
- TESTHUB-111: remove zeta-client-lib. The library is no longer needed as all mocked Zeta components
  have been removed.
- TESTHUB-115: Update Tiger to version 4.2.6.
  See [Tiger Release Notes](https://github.com/gematik/app-Tiger/blob/e3aef5b012a9f65894b19b3b1448837bb98b3a21/ReleaseNotes.md#release-426)
  for more information.
- TESTHUB-116: refactor handling of attached card and PoPP token (contributed by
  [@prat023](https://github.com/prat023))
- ZTI-4187: Add policy hot-reload test verifying that OPA applies policy changes at runtime without
  restart.

## Release 2.1.0

### Update Notes

#### Upgrade to ZETA 0.5.x (VSDM)

The ZETA Guard PEP validates PoPP tokens. In order to validate the token's signature a public keys
is required. The public key is published by the PoPP issuer. With 0.5.1 changes have been made to
how the public key is retrieved. If you are using a custom PoPP implementation you will have to
ensure that you support the new flow. Refer to
the [relevant code in the PEP for more information](https://github.com/gematik/zeta-guard-ngx-pep/blob/22888ebecf9019d2102503a8e0761e32f37e12d2/src/jwk_cache.rs#L205).

If you are changing the keys in the PoPP sample implementation you will have to update the key for
the PoPP Token Generator. In order for the new public key to work with Testhub components we are
using the PoPP Token Generator as PoPP issuer until the sample implementation can catch up.

Refer to the projects release notes to find out what changed:

- https://github.com/gematik/zeta-guard-keycloak/blob/main/ReleaseNotes.md
- https://github.com/gematik/zeta-guard-ngx-pep/blob/main/ReleaseNotes.md
- https://github.com/gematik/zeta-sdk/blob/main/ReleaseNotes.md

### Changes

- TESTHUB-85: validate all required fields of the UserInfo header. The required fields are defined
  in https://raw.githubusercontent.com/gematik/zeta/refs/heads/main/src/schemas/zeta-user-info.yaml
- TESTHUB-98: add mock-popp profile which starts only the components required for the PoPP token
  generator.
- TESTHUB-101: simplify switching between PoppTokenGenerator and PoppExampleImplementation
  components.
- TESTHUB-103: Update VSDM ZETA components to 0.5.x
- TESTHUB-105: refer to user manual for information about profiles
- ZTI-4056: add SMC-B token exchange tests for ZETA client

## Release 2.0.0

This release includes the integration of the Popp Sample Implementation into the TestHub. The Popp
Sample Implementation is an implementation of a Proof of Possession (PoPP) token generator, which
can be used for testing and development purposes. It provides a simple way to generate PoPP tokens
that can be used in the TestHub for various test scenarios.

Through configuration the PoPP tokens can also be generated by the PoppTokenGenerator as before.
Please refer to the user manual (chapter 6) for more details on how to use the Popp Example
Implementation and how to configure the TestHub to use it for PoPP token generation.

### Known issues

The ngx_pep (ZETA PEP, version 0.3.0) rejects WebSocket upgrade requests with HTTP 403 even when the
Access Token and DPoP proof are valid. Regular REST requests with the same credentials work
correctly (HTTP 200). The WebSocket integration test (`popp_websocket_via_pep.feature`) is therefore
marked as
`@Ignore` until the issue is resolved in a future ngx_pep release.

It is possible that PoPP token generation will lead to timeouts on systems with high load. We are
aware and are going to investigate possible solutions.

PoPP token generation with external services is not yet supported in this version.

Gatling performance tests currently have limited utility due to poor performance of PoPP token
generation.

### Changes

- LART-1474:
  add [popp example implementation](https://github.com/gematik/popp-sample-code). Uses an older ZETA
  version.
- TESTHUB-53: Replace license plugin by license-maven-plugin
- ZTI-4099: Added TLS Guard conformance tests to ZETA Testsuite (BSI TR-02102-2)
- ZTI-4375: migrate ZETA testsuite from mock services to Docker-based ngx_pep + Keycloak PDP. Token
  creation now uses real OAuth 2.0 Token Exchange with SMC-B authentication and DPoP binding.
  Updated tests for REST data transfer, header management, client registration and WebSocket
  communication.

## Release 1.1.15

### Update Notes

#### Upgrade to ZETA 0.4.x

When upgrading your installation you will encounter issues with a database migration. To fix the
issue, delete the ZETA PDP database volume:

1. Ensure the relevant containers are stopped
2. Delete the volume `testhub-local_postgres_data`, e.g.:
    ```
    docker volume rm testhub-local_postgres_data 
    ```

Refer to the projects release notes to find out what changed:

- https://github.com/gematik/zeta-guard-keycloak/blob/main/ReleaseNotes.md
- https://github.com/gematik/zeta-guard-ngx-pep/blob/main/ReleaseNotes.md
- https://github.com/gematik/zeta-sdk/blob/main/ReleaseNotes.md

### Changes

- PTVSDM-1549: update BDE constants for missing patient record version
- PTVSDM-1573: relax validation of VSDM response, order is not relevant
- PTVSDM-1574: ensure that Vsdm-PZ has the specified length
- TESTHUB-77: Update ZETA components from 0.3.x to 0.4.x for VSDM.
- TESTHUB-87: add Windows related troubleshooting to the user manual.
- TESTHUB-88: remove `CHANGELOG.md`s. Relevant information can be found in the
  `ReleaseNotes.md`

## Release 1.1.14

### Update Notes

#### Docker Compose Profiles (TESTHUB-64)

Docker Compose now supports profiles for different startup configurations:

- `full`: Full stack with Tiger-Proxies and clients
- `perf`: Performance mode - clients connect directly to backend, bypassing Tiger-Proxy
- `backend-only`: Backend services only, no Tiger-Proxies or clients

Example usage:

```bash
docker compose -f doc/docker/compose-local.yaml --profile full up -d          # full stack 
docker compose -f doc/docker/compose-local.yaml --profile perf up -d          # performance testing
docker compose -f doc/docker/compose-local.yaml --profile backend-only up -d  # backend only
```

### Changes

- TESTHUB-64: Add Docker Compose profiles for performance testing (`full`,
  `perf`, `backend-only`)
- TESTHUB-64: Reintroduce Tiger-Proxy into the communication between client and server
- TESTHUB-62: Add additional troubleshooting tips to the user manual.
- TESTHUB-81: Allow application/fhir+xml content type

## Release 1.1.11

### Changes

- TESTHUB-76: remove empty script files from `doc/bin` and improve documentation with
  troubleshooting tips.
- ZTI-4057: Initial client registration in zeta-testsuite: Service Discovery (well-known) and
  Dynamic Client Registration (POST /register)
- ZTI-4057: New `/register` mock endpoint in zeta-pdp-server-mockservice with RFC 7591 input
  validation and error response

## Release 1.1.10

### Update Notes

#### Obsolete SMCB Files (TESTHUB-54)

The content of the following files has been moved to environment variables and the files can be
removed:

- doc/docker/backend/zeta/smcb-private/smcb_private.alias.txt
- doc/docker/backend/zeta/smcb-private/smcb_private.pw.txt

#### Removed Bash Script Files (TESTHUB-55)

The Bash script files in `doc/bin/` have been removed. Use regular commands as documented in the
`README.md`. For example:

- `docker-compose-local-restart.sh` can be replaced with
  ```bash
  docker compose -f ./doc/docker/compose-local.yaml down -v
  docker compose -f ./doc/docker/compose-local.yaml --profile full up -d --remove-orphans
  ```
- `test-with-compose-local-rebuild.sh` can be replaced with
  ```bash
  ./mvnw clean install -Pdocker -DskipTests
  docker compose -f ./doc/docker/compose-local.yaml down -v
  docker compose -f ./doc/docker/compose-local.yaml --profile full up -d --remove-orphans
  ./mvnw -pl test/vsdm-testsuite/ -Dit.test="Vsdm*IT" -Dskip.inttests=false verify
  ```

### Changes

- TESTHUB-47: Enable feature flag for client attestation for PEP and tone down log levels. Feature
  flag will be removed in future versions.
- TESTHUB-50: Introduce JWK endpoint for PoPP server and set `pep_require_popp
  on;`. Should reduce `during jwk cache refresh (popp): error decoding response
  body` errors in PEP.
- TESTHUB-54: Simplify handling of SMCB-B certificate in configuration
- TESTHUB-55: Replace shell scripts with direct maven and docker commands
- TESTHUB-59: Additional troubleshooting steps in the user manual, add 'Getting Help' to README.md
- TESTHUB-72: Adapt Vsdm-Server implementation to accept PoppToken as Base64 encoded claims
- ZTI-3856: add ZeTA Testsuite with test cases for WebSocket communication via PEP, client
  registration and smoke tests
- ZTI-3904: add PEP header management tests for ZeTA Testsuite
- ZTI-4055: add websocket test from PoPP via ZETA-PEP

## Release 1.1.9

- TESTHUB-28: (docs) Update user manual with corrections and simplifications
- Make Healthcheck of VSDM Server Simulator more robust (migration to wget)

## Release 1.1.8

- LART-1161: (VSDM) allow test data to be read using gem-testdata
- ZTI-4104: update ZETA components to MS3a release

## Release 1.1.7

- TESTHUB-16: adjust README and ensure a index.html is present in user manual.

## Release 1.1.6

- TESTHUB-16: fix Github Workflow for Github Pages deployment
- fix SMCB path in README
- Update README of VSDM2 testsuite

## Release 1.1.5

- TESTHUB-16: add user manual and deploy is Github Pages

## Release 1.1.3

- Introduced Tiger-Proxy into the PS - to - Server communication
- Adds ZETA Guard MS2 integration for VSDM

## Release 1.1.2

- Changing vsdm-client-simservice interface, removing forceUpdate parameter
- Adding component titles to swagger UI
- Correcting component ports listed in README
- Fixed handling of Etag header

## Release 1.1.0

- Integration of ZETA prototype
- Introducing Serenity's screen play pattern
-

## Release 1.0.3

- Minor corrections
-

## Release 1.0.2

- Add more load tests in vsdm-testsuite.
- Add synthetic test data in vsdm-server.
- Remove BDE test cases from vsdm-testsuite.
- Fix issues in build script.

## Release 1.0.1

- Improving README of VSDM testsuite to provide further test execution examples.

## Release 1.0.0

- First release with major version number greater than zero.
