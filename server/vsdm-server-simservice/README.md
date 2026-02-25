<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# VSDM Server Simulator Service

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a></li>
    <li><a href="#installation">Installation</a></li>
    <li><a href="#getting-started">Getting Started</a></li>
    <li><a href="#configuration">Configuration</a></li>
    <li><a href="#test-data">Test Data</a></li>
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

The VSDM2 Server SimService (vsdm-server-simservice) is a vendor-independent implementation of the VSDM 2
service designed for automated continuous testing in integration environments.
It provides VSDM data retrieval endpoints that simulate real VSDM server behavior for testing purposes.

### Implementation Status

**Implemented Features:**

- ✅ VSDM bundle retrieval (`GET /vsdservice/v1/vsdmbundle`)
- ✅ PoPP token validation and processing
- ✅ FHIR+JSON response format support
- ✅ ETag-based conditional requests (`If-None-Match` header)
- ✅ Configurable test data via YAML files
- ✅ IKNr-based data filtering
- ✅ ZeTA integration for security headers

**Not Implemented / Limitations:**
iance (subset for testing)

- ❌ Real insurance company integrat
- ❌ Full VSDM2 specification complions
- ❌ Production-grade security features
- ❌ Complete FHIR validation
- ❌ Multi-tenant support beyond single IKNr configuration

**Note:** This is a simulator service designed for testing and development environments, not for production use.

For the complete VSDM 2.0 specification, visit
the [gemSpecPages](https://gemspec.gematik.de/docs/gemSpec/gemSpec_VSDM_2/latest/).

## Installation

### Prerequisites

- Java 21
- Maven 3.6 or higher

### Build from Source

1. **Build the project**
   ```bash
   ../../mvnw clean install
   ```

2. **Build without tests (faster)**
   ```bash
   ../../mvnw clean install -DskipTests
   ```

### Docker Build

To build a Docker Image use the docker profile:

```bash
../../mvnw install -Pdocker
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
java -jar target/vsdm-server-simservice-*.jar
```

**Option 4: Use Docker**

```bash
docker run -p 9220:9220 vsdm-server-simservice
```

**Option 5: Integrated Setup**
The vsdm-server will automatically be built and started when the rebuild/restart script for the VSDM test setup is
executed. (See the README.md in the root directory of the repository for more information.)

### Verify Installation

Once the server is running:

1. **Check service status:**
   ```bash
   curl http://localhost:9220/service/status
   ```

2. **Access Swagger UI:**
   Open http://localhost:9220/swagger-ui/index.html in your browser to explore and test the API endpoints.

### Quick Test

Test the main VSDM endpoint:

```bash
curl -X 'GET' \
  'http://localhost:9220/vsdservice/v1/vsdmbundle' \
  -H 'popp: <VALID_POPP_TOKEN>' \
  -H 'zeta-user-info: ' \
  -H 'If-None-Match: 0' \
  -H 'accept: application/fhir+json'
```

**Note:** You need a valid PoPP token containing KVNr and IKNr matching your configuration.

## Configuration

All configuration parameters for the project can be set in the `application.yaml` file located in the
`src/main/resources` directory. Especially, you can set the server port by modifying the `server.port` property.

Specific to the application, you can configure the following properties:

| Name                     | Description                                                                                                                |
|:-------------------------|----------------------------------------------------------------------------------------------------------------------------|
| vsdm.iknr                | IKNr for which this configuration can return data                                                                          |
| vsdm.valid-kvnr-prefix   | If no testdata is found, the server will respond with synthetic data to KVNRs beginning with this prefix                   |
| vsdm.invalid-kvnr-prefix | If no testdata is found, the server will respond with an VSDSERVICE_INVALID_KVNR error to KVNRs beginning with this prefix |
| vsdm.unknown-kvnr-prefix | If no testdata is found, the server will respond with an VSDSERVICE_UNKNOWN_KVNR error to KVNRs beginning with this prefix |

An example configuration is provided in the `application-local.yaml` file.
To use this configuration, you can specify the `spring.profiles.active=local` property when starting the server.

## Test Data

The data returned by the server is based on YAML files located in the
`<projectRoot>/public-test-data/person` directory. A full walk-through of the
test data format is out of scope for this document. See below to learn how to
customize the test data or provide your own.

**Note:** Test data is parsed upon application start. Restart the application for changes to take effect.

**Tip:** Wrap strings with quotes to avoid running into typical YAML issues like the Norway problem.

Examples:

```yaml
patients:
   JonDoe:
      alias:
         - 'Jon Doe'
      personData:
         gender: 'm'
         name:
            given: 'Jon'
            family: 'Doe'
         birthDate: '1980-01-01'
         kvnr: 'J000000001'
         address:
            postalCode: '12345'
            city: 'Sample City'
            streetName: 'Sample Street'
            houseNumber: '1'
         insurance: 'Insurance Example'
```

It's also possible to merge multiple data sets into one file:

```yaml
patients:
   test-user-0:
      personData:
         kvnr: 'kvnr-1'
      ownerTestsuite: 'unit-test TestDataCardsTest.java'
      cards:
         - file: '/patient/cardImages/EGK_80276883110000164023_gema5.xml'
   test-user-1:
      personData:
         kvnr: 'kvnr-2'
      ownerTestsuite: 'unit-test TestDataCardsTest.java'
      cards:
         - file: '/patient/cardImages/EGK_80276883110000164023_gema5.xml'
```

### Using custom test data

If you want to customize or reuse your test data you can use Docker Volumes. The
easiest way to get started is:

1. Copy the examples from `<projectRoot>/public-test-data` to a local folder.
2. Open `<projectRoot>/doc/docker/vsdm/compose-local.yaml` and check the value
   of the environment variable `VSDM_PATH_TO_TEST_DATA`.
3. Add a volume to the compose file and point your local directory to the value
   of `VSDM_PATH_TO_TEST_DATA`.
   
   Example: Assume `VSDM_PATH_TO_TEST_DATA` is set to `/opt/test-data` and the
   data on your host machine is located at `/tmp/test-data`:
   
```yaml
services:
  vsdm-server:
    image: de.gematik.ti20.simsvc.server/vsdm-server-simservice:local
    volumes:
      - type: bind
        # path on your host machine
        source: /tmp/test-data
        # value referenced by VSDM_PATH_TO_TEST_DATA
        target: /opt/test-data
    environment:
      - VSDM_PATH_TO_TEST_DATA=/opt/test-data
      # other variables
```
   
Change some test data e.g. a KVNR and restart Docker for the changes to take
effect.

You can verify your configuration by:
- checking the logs for lines containing `TestDataConfiguration` or `TestDataManager`
- make requests against the `DebugController` to list
available test data.

## Behavior on requests (order)

1. `TestDataRepository` attempts to load the YAML file for the KVNR.
    - Found: server returns the loaded resources (HTTP 200).
2. Not found:
    - If `kvnr.startsWith(vsdm.invalid-kvnr-prefix)` → error response:
        - HTTP 400 with `OperationOutcome` and code `VSDSERVICE_PATIENT_RECORD_NOT_FOUND`.
    - Else, if `kvnr.startsWith(vsdm.valid-kvnr-prefix)` → synthetic data (HTTP 200).
        - Synthetic rules: e.g., `family` is set to `family-name-{kvnr}`, `given` = `given-name-{kvnr}`, `id` =
          `patient-{kvnr}`, payor `iknr` may be the default.
    - Else → error response:
        - HTTP 400 with `OperationOutcome` and code `VSDSERVICE_INVALID_KVNR`.

Note: In the implementation the invalid-prefix check is performed before the valid-prefix check (priority: `invalid`
first).

## Endpoints

The server exposes the following endpoints:

| Name                          | Description                                                                           |
|:------------------------------|---------------------------------------------------------------------------------------|
| GET /vsdservice/v1/vsdmbundle | Returns the VSDM bundle for the KVNr and IKNr encoded in the PoPP token.              |
| GET /service/status           | Returns the status of the server.                                                     |
| GET /debug/kvnrs              | Returns a set of KVNRs found in the test data.                                        |
| GET /debug/patients           | Returns patient data for debugging purposes. Refer to the method for more information |

## Examples

You can use tools like curl or Postman to send requests to the server. Here are some example curl
commands:

### VSDM Bundle Retrieval

```bash
curl -X 'GET' \
  'http://localhost:9220/vsdservice/v1/vsdmbundle' \
  -H 'popp: POPP_TOKEN' \
  -H 'zeta-user-info: USER_INFO' \
  -H 'If-None-Match: 0' \
  -H 'accept: application/fhir+json'
```

Returns the VSDM bundle for the KVNr and IKNr encoded in the PoPP token.

**Required Headers:**

- `popp` - Must contain a valid PoPP token with KVNr and IKNr
- `zeta-user-info` - Must be specified but can be empty

**Optional Headers:**

- `If-None-Match` - ETag for conditional requests (set to `0` to always return data)
- `accept` - Response format (defaults to `application/fhir+json`)

**Important:** The server is configured to return data only for the IKNr configured in the `application.properties`
file.

### Service Status

```bash
curl -X 'GET' \
  'http://localhost:9220/service/status' \
  -H 'accept: */*'
```

Returns the status of the server.

## Folder Structure

This project has the following folders:

| Folder | Content                                                |
|:-------|--------------------------------------------------------|
| bin    | scripts relating to the build process                  |
| docker | docker file                                            |
| images | static image material for rendering Markdown documents |
| src    | source files of the project                            | 

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
