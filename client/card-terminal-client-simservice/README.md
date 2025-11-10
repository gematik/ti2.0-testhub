<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# Card Terminal Client Simulator Service

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

The Card Terminal Client Simulator Service (card-terminal-client-simservice) is a Java-based implementation that
simulates the behavior of a card terminal for testing and development purposes.
It provides a RESTful API to interact with the simulated card terminal,
allowing users to perform various operations such as inserting and removing cards, reading card data, and executing APDU
commands.

### Implementation Status

**Implemented Features:**

- ✅ Virtual card slot management (configurable up to 3000 slots)
- ✅ Multiple card type support: EGK/EHC, HBA/HPC, HPIC, SMC-B
- ✅ XML-based card loading and parsing
- ✅ APDU command transmission and processing
- ✅ PACE (Password Authenticated Connection Establishment) protocol
- ✅ Digital signature functionality with modern algorithms (SHA256+)
- ✅ Certificate extraction and validation
- ✅ Card data extraction (KVNR, IKNR, patient data from EGK cards)
- ✅ SMC-B card information retrieval (Telematik-ID, ProfessionOID)
- ✅ Debug and diagnostic endpoints
- ✅ Signature protocol service
- ✅ Card connection management
- ✅ Cryptographic operations (RSA, ECDSA)
- ✅ Comprehensive error handling and validation

**Not Implemented / Limitations:**

- ❌ Physical card reader integration (virtual simulation only)
- ❌ Real smartcard operations (uses XML-based simulated card data)
- ❌ PIN verification workflows (no CHV/PIN handling)
- ❌ Complete ISO 7816 specification compliance
- ❌ Multi-terminal support (single terminal simulation)
- ❌ Real-time card insertion/removal events
- ❌ Hardware Security Module (HSM) integration
- ❌ Production-grade security for real card data

**Note:** This is a comprehensive simulator service designed for testing and development environments. It provides
extensive card terminal functionality but operates entirely in simulation mode without real hardware interaction.

## Installation

### Prerequisites

- Java 21
- Maven 3.6 or higher

### Build from Source

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd card-terminal-client-simservice
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

or use

``` bash
mvn clean install -Ddocker
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
java -jar target/card-terminal-client-simservice-*.jar
```

**Option 4: Use Docker**

```bash
docker run -p 8000:8000 card-terminal-client-simservice
```

**Option 5: Integrated Setup**
The card-terminal-simservice will automatically be built and started when the rebuild/restart script for the VSDM test
setup is executed. (See the README.md in the root directory of the repository for more information.)

### Verify Installation

Once the server is running:

1. **Check available cards:**
   ```bash
   curl http://localhost:8000/cards
   ```

2. **Access Swagger UI:**
   Open http://localhost:8000/swagger-ui/index.html in your browser to explore and test the API endpoints.

### Quick Test

Test basic functionality by loading a card and checking slot status:

```bash
# Check slot status (should be empty initially)
curl http://localhost:8000/slots/1

# Load sample card data (see src/main/resources/cardimages for examples)
curl -X 'PUT' \
  'http://localhost:8000/slots/1' \
  -H 'Content-Type: application/xml' \
  -d @src/main/resources/cardimages/sample-card.xml

# Verify card is loaded
curl http://localhost:8000/slots/1
```

## Configuration

All configuration parameters for the project can be set in the `application.yaml` file located in the
`src/main/resources` directory. Especially, you can set the server port by modifying the `server.port` property.

Specific to the application, you can configure the following properties:

| Name                | Description                                                                 |
|:--------------------|-----------------------------------------------------------------------------|
| card.terminal.slots | Number of (virtual) slots that are available by the simulated card terminal |

An example configuration is provided in the `application-local.yaml` file.
To use this configuration, you can specify the `spring.profiles.active=local` property when starting the server.

## Endpoints

The server exposes the following endpoints:

| Name                                 | Description                                                                            |
|:-------------------------------------|----------------------------------------------------------------------------------------|
| PUT /slots/{slotId}                  | Inserts a card in the specified slot                                                   |
| DELETE /slots/{slotId}               | Removes the card from the specified slot                                               |
| GET /slots/{slotId}                  | Returns data of the card in the specified slot                                         |
| POST /slots/{slotId}/transmit        | Transmit an APDU command to the card in a specific slot.                               |
| DELETE /cards/{cardHandle}           | Close a virtual connection to a card                                                   |
| GET /cards/{cardHandle}              | Establish a virtual connection to a card.                                              |
| GET /cards/{cardHandle}/smc-b-info   | Get SMC-B card information including Telematik-ID and ProfessionOID                    |
| GET /cards/{cardHandle}/egk-info     | Extract EGK information from the card containing authentic KVNR, IKNR and patient data |
| GET /cards                           | List all available cards across all slots                                              |
| POST /cards/{cardHandle}/transmit    | Transmit an APDU command to a connected card                                           |
| POST /cards/{cardHandle}/sign        | Sign data with the card's certificate                                                  |
| POST /cards/{cardHandle}/certificate | Get certificate from card                                                              |

## Examples

### Slot Management

#### Insert Card into Slot

```bash
curl -X 'PUT' \
  'http://localhost:8000/slots/1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/xml' \
  -d 'CARD_DATA'
```

Inserts a card in the specified slot.
The card data must be provided in the request body in XML format.
Examples for card data can be found in the `src/main/resources/cardimages` and `src/test/resources` directories.

#### Remove Card from Slot

```bash
curl -X 'DELETE' \
  'http://localhost:8000/slots/1' \
  -H 'accept: */*'
```

Removes the card from the specified slot.
Before a slot can be reused by another card, the slot must be empty, i.e. the currently used card must be removed first.

#### Get Slot Information

```bash
curl -X 'GET' \
  'http://localhost:8000/slots/1' \
  -H 'accept: */*'
```

Returns data of the card in the specified slot.
The returned data includes information such as card type and card handle, which can be used to address the /cards
endpoints.

#### Transmit APDU to Slot

```bash
curl -X 'POST' \
  'http://localhost:8000/slots/1/transmit' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "command": "d2 5f af dF 8e f7 fA 5B CD 74 2D 0B ED f3 dB 53 d9 81 7F bc"
}'
```

Transmits an APDU command to the card in a specific slot.
This endpoint can be used by the PoPP server to simulate a connection to a card in a specified slot.

### Card Management

#### Establish Card Connection

```bash
curl -X 'GET' \
  'http://localhost:8000/cards/card-1760354731986' \
  -H 'accept: */*'
```

Establish a virtual connection to a card.

#### Close Card Connection

```bash
curl -X 'DELETE' \
  'http://localhost:8000/cards/card-1760354731986' \
  -H 'accept: */*'
```

Close a virtual connection to a card.

#### Get SMC-B Information

```bash
curl -X 'GET' \
  'http://localhost:8000/cards/card-1760340780478/smc-b-info' \
  -H 'accept: */*'
```

Get SMC-B card information including Telematik-ID and ProfessionOID.
The provided cardHandle must be that of an SMC-B card.

#### Get SMC-B Debug Information

```bash
curl -X 'GET' \
  'http://localhost:8000/cards/card-1760340780478/smc-b-debug' \
  -H 'accept: */*'
```

Get detailed SMC-B debug information.
The provided cardHandle must be that of an SMC-B card.

#### Get EGK Information

```bash
curl -X 'GET' \
  'http://localhost:8000/cards/card-1760340780478/egk-info' \
  -H 'accept: */*'
```

Extract EGK information from the card containing authentic KVNR, IKNR and patient data.
The provided cardHandle must be that of an EGK card.

#### Get Card Debug Information

```bash
curl -X 'GET' \
  'http://localhost:8000/cards/card-1760340780478/debug-info' \
  -H 'accept: */*'
```

Get debug information for a card.

#### Get Certificate Information

```bash
curl -X 'GET' \
  'http://localhost:8000/cards/card-1760340780478/cert-info' \
  -H 'accept: */*'
```

Get certificate information for any card type.
Returns EGK info for EGK cards and SMC-B info for SMC-B cards.

#### List All Cards

```bash
curl -X 'GET' \
  'http://localhost:8000/cards/' \
  -H 'accept: */*'
```

List all available cards across all slots.

#### Transmit APDU to Card

```bash
curl -X 'POST' \
  'http://localhost:8000/cards/card-1760354731986/transmit' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "command": "d2 5f af dF 8e f7 fA 5B CD 74 2D 0B ED f3 dB 53 d9 81 7F bc"
}'
```

Transmit an APDU command to a connected card.
This endpoint can be used by the PoPP server to simulate a connection to a card with the specified handle.

#### Sign Data with Card

```bash
curl -X 'POST' \
  'http://localhost:8000/cards/card-1760354731986/sign' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "data": "data to be signed"
}'
```

Sign data with the card's certificate.

#### Get Certificate from Card

```bash
curl -X 'POST' \
  'http://localhost:8000/cards/card-1760354731986/certificate' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json'
```

Get certificate information from the card with the specified handle.

#### Load Card from XML

```bash
curl -X 'POST' \
  'http://localhost:8000/cards/load' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "cardType": "egk",
  "xmlFile": "file path"
}'
```

Load card information from an XML file.

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
