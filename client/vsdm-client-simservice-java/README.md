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
    <li><a href="#changelog">Changelog</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

## About the Project

The VSDM2 Client Simulator Service (vsdm-client-simservice) utilizes the reference implementations of the PoPP Client
and ZeTA Client to simulate VSDM2-related functionalities within primary systems.
It provides the endpoints defined in the VSDM2 specification, enabling comprehensive testing and validation of VSDM2
interactions.
Additionally, it offers endpoints for managing test scenarios, e.g. to test if data is correctly cached.

### Implementation Status

**Implemented Features:**

- ✅ VSDM data retrieval (`GET /client/vsdm/vsd`) - Core VSDM2 specification endpoint
- ✅ Terminal configuration management (`GET/PUT /client/config/terminal`)
- ✅ PoPP token integration for authentication
- ✅ ZeTA client integration for security policies
- ✅ Data caching mechanisms with ETag support
- ✅ FHIR XML/JSON format support
- ✅ Test endpoints for debugging and validation

**Not Implemented / Limitations:**

- ❌ Physical card terminal support (only simulated terminals of type SIMSVC)
- ❌ Real smartcard operations (uses simulated card data)
- ❌ Advanced VSDM2 features beyond basic data retrieval
- ❌ Full VSDM2 specification compliance (subset implementation for testing purposes)

**Note:** This is a simulator service designed for testing and development. It implements the core VSDM2 client
functionality but may not include all features defined in the complete specification.

For the complete VSDM 2.0 specification,
visit [gemSpecPages](https://gemspec.gematik.de/docs/gemSpec/gemSpec_VSDM_2/latest/).

## Installation

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Access to PoPP and VSDM server instances

### Build from Source

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd vsdm-client-simservice-java
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

Alternatively, use the provided script to build a Docker image:

```bash
./bin/docker-build.sh
```

## Getting Started

### Starting the Service

**Option 1: Run with Maven**

```bash
mvn spring-boot:run
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
The vsdm-client will automatically be built and started when the rebuild/restart script for the VSDM test setup is
executed. (See the README.md in the root directory of the repository for more information.)

### Verify Installation

Once the server is running, you can verify it's working by:

1. **Check service status:**
   ```bash
   curl http://localhost:8220/service/status
   ```

2. **Access Swagger UI:**
   Open http://localhost:8220/swagger-ui/index.html in your browser to explore and test the API endpoints.

### Quick Test

Test the main VSDM endpoint:

```bash
curl -X 'GET' \
  'http://localhost:8220/client/vsdm/vsd?terminalId=1&egkSlotId=1&smcBSlotId=1' \
  -H 'accept: */*'
```

**Note:** You must configure a terminal via `/client/config/terminal` endpoint before using the VSDM endpoints.

## Configuration

All configuration parameters for the project can be set in the `application.yaml` file located in the
`src/main/resources` directory. Especially, you can set the server port by modifying the `server.port` property.

Specific to the application, you can configure the following properties:

| Name          | Description                                                       |
|:--------------|-------------------------------------------------------------------|
| popp.http.url | URL of the HTTP endpoint of a PoppServer providing the popp token |
| popp.ws.url   | URL of the WS endpoint of a PoppServer providing the popp token   |
| vsdm.url      | URL of the VSDM server providing the data                         |

An example configuration is provided in the `application-local.yaml` file.
To use this configuration, you can specify the `spring.profiles.active=local` property when starting the server.

## Endpoints

The server exposes the following endpoints:

| Name                        | Description                                                                                   |
|:----------------------------|-----------------------------------------------------------------------------------------------|
| GET /client/vsdm/vsd        | Returns the VSDM data provided by the server given the terminal and slot of the Egk           |
| GET /client/config/terminal | Return the terminal configuration of the vsdm-client                                          |
| PUT /client/config/terminal | Allows to set/update the terminal configuration of the vsdm-client                            |
| GET /client/test/vsdmData   | Test endpoint (non-spec) to inspect the VsdData cached in the client for the specified card   |
| GET /client/test/poppToken  | Test endpoint (non-spec) to inspect the PoppToken cached in the client for the specified card |
| GET /client/test/readEgk    | Test endpoint (non-spec) to inspect the (truncated) Vsd data stored on the specified card     |
| GET /service/status         | Returns the status of the server.                                                             |

## Examples

### Vsd data query

```
curl -X 'GET' \
'http://localhost:8220/client/vsdm/vsd?terminalId=1&egkSlotId=1&smcBSlotId=1' \
-H 'accept: */*'
```

Returns the VSDM data provided by the server given the terminal and slot of the eGK and slot of the SMC-B.

**Required parameters:**

- `terminalId`, `egkSlotId`, `smcBSlotId` - The terminal for `terminalId` must be configured via the
  `/client/config/terminal` endpoint before use

**Optional parameters:**

- `isFhirXML` - Specifies the desired format of the returned VSDM data
- `forceUpdate` - Forces an update of the cached data (defaults to false)
- `poppToken` - Provides a custom PoPP token (if not provided, token is requested from configured PoPP server)
- `If-None-Match` - ETag of the last received VSDM data (defaults to '0' which triggers server to always return data)

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

Allows to set/update the terminal configuration of the vsdm-client.
Initially only simulated terminals of type SIMSVC are supported.
Simulated terminals are backed by an instance of the card-terminal-simsvc-java project.

### Test endpoints

```
curl -X 'GET' \
  'http://localhost:8220/client/test/vsdmData?terminalId=1&slotId=1&cardId=abcd1234' \
  -H 'accept: */*'
```

Returns the VsdData cached in the client for the specified card.
This does specifically not query data from the vsdm server and may result in an empty response.

```
curl -X 'GET' \
  'http://localhost:8220/client/test/poppToken?terminalId=1&slotId=1&cardId=abcd1234' \
  -H 'accept: */*'
```

Returns the PoppToken cached in the client for the specified card.
This does specifically not query data from the popp server and may result in an empty response.

```
curl -X 'GET' \
  'http://localhost:8220/client/test/readEgk?terminalId=1&egkSlotId=1' \
  -H 'accept: */*'
```

Returns the truncated Vsd data stored on the specified card.

### Status endpoint

```
curl -X 'GET' \
  'http://localhost:8220/service/status' \
  -H 'accept: */*'
```

Returns the status of the server.

## Contract Testing (internal)

1) Build the project, with `./doc/bin/vsdm/mvn-install-all.sh --skip-tests`
2) Run the Pact test suite in `./src/test/java/de.gematik.ti20.simsvc.client/service/VsdmClientServicePactTest`
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

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the
(latest) releases.

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for information about changes.

## Contributing

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull
requests to us.

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
