<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# VSDM Client Simulator Service

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
    <li><a href="#contract-testing-internal">Contract Testing (internal)</a></li>
    <li><a href="#release-notes">Release Notes</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

## About the Project

The VSDM2 Client Simulator Service (vsdm-client-simservice) utilizes the reference implementations
of the PoPP Client and ZeTA Client to simulate VSDM2-related functionalities within primary systems.
It provides the endpoints defined in the VSDM2 specification, enabling comprehensive testing and
validation of VSDM2 interactions. Additionally, it offers endpoints for managing test scenarios,
e.g. to test if data is correctly cached.

### Implementation Status

**Implemented Features:**

- ✅ VSDM data retrieval (`GET /client/vsdm/vsd`) with support for XML and JSON responses
- ✅ Terminal configuration management (`GET/PUT /client/config/terminal`)
- ✅ PoPP token retrieval from configured service, cache, or injected token
- ✅ ZeTA SDK integration with configurable storage interception
- ✅ Data caching mechanisms with ETag support
- ✅ Test endpoints for cache inspection and card data diagnostics

**Not Implemented / Limitations:**

- ❌ Physical card terminal support (only simulated terminals of type SIMSVC)
- ❌ Real smartcard operations (uses simulated card data)
- ❌ Advanced VSDM2 features beyond basic data retrieval
- ❌ Full VSDM2 specification compliance (subset implementation for testing purposes)

**Note:** This is a simulator service designed for testing and development. It implements the core
VSDM2 client functionality but may not include all features defined in the complete specification.

For the complete VSDM 2.0 specification,
visit [gemSpecPages](https://gemspec.gematik.de/docs/gemSpec/gemSpec_VSDM_2/latest/).

## Installation

### Prerequisites

- Java 21
- Maven 3.6 or higher
- Access to PoPP and VSDM server instances, depending on the selected profile

### Build from Source

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd vsdm-client-simservice-java
   ```

2. **Build the project**
   ```bash
   ../../mvnw clean install
   ```

3. **Build without tests (faster)**
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
../../mvnw spring-boot:run
```

**Option 2: Run the JAR file**

```bash
java -jar target/vsdm-client-simservice-java-*.jar
```

**Option 3: Use Docker**

```bash
docker run -p 8220:8220 vsdm-client-simservice
```

**Option 4: Integrated Setup**
The vsdm-client will automatically be built and started when the rebuild/restart script for the VSDM
test setup is executed. (See the README.md in the root directory of the repository for more
information.)

### Verify Installation

Once the server is running, you can verify it's working by:

1. **Check service status:**
   ```bash
   curl http://localhost:8220/service/status
   ```

2. **Access Swagger UI:**
   Open http://localhost:8220/swagger-ui/index.html in your browser to explore and test the API
   endpoints.

### Quick Test

Test the main VSDM endpoint:

```bash
curl -X 'GET' \
  'http://localhost:8220/client/vsdm/vsd?terminalId=1&egkSlotId=1' \
  -H 'accept: application/json' \
  -H 'poppToken: <optional-token>' \
  -H 'If-None-Match: <etag>'
```

**Note:** You must configure a terminal via `/client/config/terminal` endpoint before using the VSDM
endpoints.

## Configuration

All configuration parameters for the project can be set in the `application.yaml` file located in
the
`src/main/resources` directory. Especially, you can set the server port by modifying the
`server.port` property.

Specific to the application, you can configure the following properties:

| Name                       | Description                                                                                    |
|:---------------------------|------------------------------------------------------------------------------------------------|
| popp.http.url              | URL of the HTTP endpoint of a Popp server providing the PoPP token                             |
| popp.ws.url                | URL of the WS endpoint of a Popp server providing the PoPP token                               |
| vsdm.resourceServerUrl     | URL of the VSDM server providing the data                                                      |
| vsdm.useMockPoppToken      | If `true`, the mock token service is used to create a synthetic PoPP token                     |
| vsdm.poppTokenGeneratorURL | URL of the PoPP token generator used for mocked tokens                                         |
| vsdm.interceptStorage      | Enables Zeta SDK storage interception and exposes captured entries via `/client/test/zetaData` |

The `interceptStorage` flag controls whether the Zeta SDK uses the in-memory `StorageInterceptor`.
It is mainly intended for debugging and inspection, not for load-test scenarios.

An example configuration is provided in the `application-local.yaml` file. To use this
configuration, you can specify the `spring.profiles.active=local` property when starting the server.

## Endpoints

The server exposes the following endpoints:

| Name                        | Description                                                                           |
|:----------------------------|---------------------------------------------------------------------------------------|
| GET /client/vsdm/vsd        | Returns VSDM data for the given terminal and card slots                               |
| GET /client/config/terminal | Returns the configured terminal setup                                                 |
| PUT /client/config/terminal | Updates the terminal configuration                                                    |
| GET /client/test/vsdmData   | Inspects cached VSDM data for the specified card                                      |
| GET /client/test/poppToken  | Inspects the cached PoPP token for the specified card                                 |
| GET /client/test/readEgk    | Reads truncated eGK data directly from the specified card                             |
| GET /client/test/zetaData   | Returns the in-memory storage cache used by the Zeta SDK when `interceptStorage=true` |
| GET /service/status         | Returns the status of the server                                                      |

## Examples

### Vsd data query

```
curl -X 'GET' \
'http://localhost:8220/client/vsdm/vsd?terminalId=1&egkSlotId=1&virtualCard=virtualCard&isFhirXml=false&profileVersion=1.1' \
-H 'accept: application/json' \
-H 'poppToken: <optional-token>' \
-H 'If-None-Match: <etag>'
```

Returns the VSDM data provided by the server for the given terminal and eGK slot.

**Required parameters:**

- `terminalId`, `egkSlotId` - The terminal referenced by `terminalId` must be configured via
  `/client/config/terminal` before use

**Optional parameters:**

- `virtualCard` - Optional identifier used when a token is injected
- `isFhirXml` - When `true`, returns the VSDM response as FHIR XML instead of JSON
- `profileVersion` - Optional profile version appended as backend query parameter

**Optional headers:**

- `poppToken` - Optional request header that injects a PoPP token directly
- `If-None-Match` - Optional request header used for conditional requests

### Terminal configuration

```
curl -X 'GET' \
  'http://localhost:8220/client/config/terminal' \
  -H 'accept: */*'
```

Returns the terminal configuration of the vsdm-client.

```
curl -X 'PUT' \
  'http://localhost:8220/client/config/terminal' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '[
  {
    "name": "Simulated Terminal",
    "type": "SIMSVC",
    "url":  "ws://card-terminal-client"
  }
]'
```

Allows to set/update the terminal configuration of the vsdm-client. Initially only simulated
terminals of type `SIMSVC` are supported. Simulated terminals are backed by an instance of the
card-terminal-client-simservice project.

### Test endpoints

```
curl -X 'GET' \
  'http://localhost:8220/client/test/vsdmData?terminalId=1&slotId=1&cardId=abcd1234' \
  -H 'accept: */*'
```

Returns the VSDM data cached in the client for the specified card. This does not query the VSDM
server and may return an empty response.

```
curl -X 'GET' \
  'http://localhost:8220/client/test/poppToken?terminalId=1&slotId=1&cardId=abcd1234' \
  -H 'accept: */*'
```

Returns the PoPP token cached in the client for the specified card. This does not query the PoPP
server and may return an empty response.

```
curl -X 'GET' \
  'http://localhost:8220/client/test/readEgk?terminalId=1&egkSlotId=1' \
  -H 'accept: */*'
```

Returns the truncated eGK data stored on the specified card.

```
curl -X 'GET' \
  'http://localhost:8220/client/test/zetaData' \
  -H 'accept: application/json'
```

Returns the in-memory storage entries captured by the Zeta SDK while `INTERCEPT_STORAGE=true` is
active. This endpoint is intended for debugging and inspection of intercepted Zeta SDK data. With
the flag set to `false` or left unset, the returned map may be empty.

```
curl -X 'DELETE' \
   'http://localhost:8220/client/test/vsdmData'
```

Clear cached VSDM data.

### Status endpoint

```
curl -X 'GET' \
  'http://localhost:8220/service/status' \
  -H 'accept: */*'
```

Returns the status of the server.

## Contract Testing (internal)

1) Build the project, with `./doc/bin/mvn-install-all.sh --skip-tests`
2) Run the Pact test suite in
   `./src/test/java/de.gematik.ti20.simsvc.client/service/VsdmClientServicePactTest`
3) Read the results in `./target/pacts`

### Upload test results to the DeveloperPortal

1) Get the `MY_API_KEY` from the DevPortal Team. This key should have read and write rights.
2) Create local configuration file with:
   `cp ./src/test/resources/pactconfig-local.config.template ./src/test/resources/pactconfig-local.config`
3) Edit that file by adding `MY_API_KEY`
4) Upload the test results by running the `main` method of
   `./src/test/java/de.gematik.ti20.simsvc.client.pact.pactutils.ConsumerBrokerUtils`

### View the compatibility matrix

To check the compatibility of the interface tested using Pact

1) visit https://pact-deploy-insights.lovable.app/
2) Get the following information from `./src/test/resources/pactconfig.properties`
    - pact.consumer.name
    - pact.consumer.version
3) Configure the application with:

```

BaseUrl: https://gematik.pactflow.io
Bearer Token: MY_API_KEY (note: a read-only API key would be enough)
Participant: pact.consumer.name (e.g vsdm-client-simservice-java)
Version: pact.consumer.version (e.g. 0.0.2)
Environment: Test

```

## Folder Structure

This project has the following folders:

| Folder | Content                               |
|:-------|---------------------------------------|
| bin    | scripts relating to the build process |
| docker | docker file                           |
| src    | source files of the project           | 

## Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the (latest) releases.

## Contributing

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our code of conduct, and the process
for submitting pull requests to us.

## License

Copyright 2025 gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.
