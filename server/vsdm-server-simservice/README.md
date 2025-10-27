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

- Java 21 or higher
- Maven 3.6 or higher

### Build from Source

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd vsdm-server-simservice
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
  -H 'zeta-popp-token-content: <VALID_POPP_TOKEN>' \
  -H 'zeta-user-info: ' \
  -H 'If-None-Match: 0' \
  -H 'accept: application/fhir+json'
```

**Note:** You need a valid PoPP token containing KVNr and IKNr matching your configuration.

## Configuration

All configuration parameters for the project can be set in the `application.yaml` file located in the
`src/main/resources` directory. Especially, you can set the server port by modifying the `server.port` property.

Specific to the application, you can configure the following properties:

| Name      | Description                                       |
|:----------|---------------------------------------------------|
| vsdm.iknr | IKNr for which this configuration can return data |

An example configuration is provided in the `application-local.yaml` file.
To use this configuration, you can specify the `spring.profiles.active=local` property when starting the server.

## Test Data

The data returned by the server is based on YAML files located in the `src/main/resources/de/gematik/vsdm/testdata`
directory.

To add or modify test data, you can create or edit YAML files in this directory.

## Endpoints

The server exposes the following endpoints:

| Name                          | Description                                                              |
|:------------------------------|--------------------------------------------------------------------------|
| GET /vsdservice/v1/vsdmbundle | Returns the VSDM bundle for the KVNr and IKNr encoded in the PoPP token. |
| GET /service/status           | Returns the status of the server.                                        |

## Examples

You can use tools like curl or Postman to send requests to the server. Here are some example curl
commands:

### VSDM Bundle Retrieval

```bash
curl -X 'GET' \
  'http://localhost:9220/vsdservice/v1/vsdmbundle' \
  -H 'zeta-popp-token-content: POPP_TOKEN' \
  -H 'zeta-user-info: USER_INFO' \
  -H 'If-None-Match: 0' \
  -H 'accept: application/fhir+json'
```

Returns the VSDM bundle for the KVNr and IKNr encoded in the PoPP token.

**Required Headers:**

- `zeta-popp-token-content` - Must contain a valid PoPP token with KVNr and IKNr
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
