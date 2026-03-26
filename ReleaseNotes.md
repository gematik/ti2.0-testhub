<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# Release Notes TI 2.0 TestHub

## Release 1.1.13

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

- TESTHUB-64: Add Docker Compose profiles for performance testing (`full`, `perf`, `backend-only`)
- TESTHUB-64: Reintroduce Tiger-Proxy into the communication between client and server
- TESTHUB-62: Add additional troubleshooting tips to the user manual.
- TESTHUB-81: Allow application/fhir+xml content type

## Release 1.1.11

### Changes

- TESTHUB-76: remove empty script files from `doc/bin` and improve documentation
  with troubleshooting tips.

## Release 1.1.10

### Update Notes

#### Obsolete SMCB Files (TESTHUB-54)

The content of the following files has been moved to environment variables and the files can be removed:

- doc/docker/backend/zeta/smcb-private/smcb_private.alias.txt
- doc/docker/backend/zeta/smcb-private/smcb_private.pw.txt

#### Removed Bash Script Files (TESTHUB-55)

The Bash script files in `doc/bin/` have been removed. Use regular commands as documented in the `README.md`.
For example:

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

- TESTHUB-47: Enable feature flag for client attestation for PEP and tone down
  log levels. Feature flag will be removed in future versions.
- TESTHUB-50: Introduce JWK endpoint for PoPP server and set `pep_require_popp
  on;`. Should reduce `during jwk cache refresh (popp): error decoding response
  body` errors in PEP.
- TESTHUB-54: Simplify handling of SMCB-B certificate in configuration
- TESTHUB-55: Replace shell scripts with direct maven and docker commands
- TESTHUB-59: Additional troubleshooting steps in the user manual, add 'Getting
  Help' to README.md
- TESTHUB-72: Adapt Vsdm-Server implementation to accept PoppToken as Base64 encoded claims
- ZTI-3856: add ZeTA Testsuite with test cases for WebSocket communication via
  PEP, client registration and smoke tests
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
