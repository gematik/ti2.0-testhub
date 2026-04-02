<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# TI 2.0 Testhub

> [!CAUTION]
> This project is not meant to be run in production and we strongly
> advise against!

The **ti 2.0 testhub** provides a comprehensive test environment for the
modernized german telematics infrastructure ( ti) version 2.0. As the healthcare
telematics infrastructure undergoes significant architectural improvements, a
core aspect is the implementation of **zero trust architecture (zeta)** security
concepts.

This project enables developers and testers to:

- **simulate and validate** zero trust architecture (zeta) components
- **test popp** (proof of possession) workflows for secure authentication
- **validate vsdm2** (versichertenstammdatenmanagement 2.0) functionality
- **develop and test** client applications against mock backend services
- **understand integration patterns** for ti 2.0 ecosystem

# Getting Started

## Acquire SMC-B Certificate

ZETA requires an SMC-B certificate to work. Follow these steps:

1. Request a test SMC-B certificate
   from [gematik Anfrageportal](https://service.gematik.de/servicedesk/customer/portal/37/create/198)
   You will receive the certificate from Gematik as a ZIP file.
2. In the ZIP file locate a `.p12` file whose filename includes `AUT_E256_`.
3. Extract the file and rename it to `smcb_private.p12`
4. Place the file in the `doc/docker/backend/zeta/smcb-private` folder

## Install Required Software

- Java 21
- Docker
- Docker Compose
- Optional: A GUI for Docker (e.g. Docker Desktop) can be helpful for monitoring containers and viewing logs. 
We had good results with [Rancher Desktop](https://rancherdesktop.io/), which is a free and open source alternative to Docker Desktop.

## Start Docker Containers

1. Build required Docker images locally:
   ```bash
    ./mvnw clean install -Pdocker -DskipTests
    ```
2. Remove Docker containers from previous runs:
    ```bash
    docker compose -f ./doc/docker/compose-local.yaml --profile full down -v
    ```
3. Start Docker containers:
    ```bash
    docker compose -f ./doc/docker/compose-local.yaml --profile full up -d --remove-orphans
    ```

### Docker Compose Profiles

The following profiles are available:

| Profile        | Description                                                                     |
|----------------|---------------------------------------------------------------------------------|
| `full`         | Full stack: Backend + Tiger-Proxies + Clients (traffic routed via tiger-proxy)  |
| `backend-only` | Backend services only (no tiger-proxies, no clients)                            |
| `perf`         | Performance testing: Backend + Clients (direct backend access, bypassing proxy) |

**Usage examples:**

```bash
# Full stack
docker compose -f ./doc/docker/compose-local.yaml --profile full up -d

# Backend only
docker compose -f ./doc/docker/compose-local.yaml --profile backend-only up -d

# Performance testing (clients bypass tiger-proxy)
docker compose -f ./doc/docker/compose-local.yaml --profile perf up -d
```

# Usage

## Running Tests

To verify Testhub is working as expected run the VSDM2 integration tests:

```bash
./mvnw -pl test/vsdm-testsuite/ -Dit.test="Vsdm*IT" -Dskip.inttests=false verify
```

Additional E2E tests can be found in `test/`.

## Running Tests Manually

Tests are written using Cucumber and Gherkin. The files are located in `test/`.

To execute tests manually using IntelliJ:

1. Install the plugins Gherkin and Cucumber for Java.
2. [Configure IntelliJ using the Tiger manual](https://gematik.github.io/app-Tiger/Tiger-User-Manual.html#intellij)
3. Locate your desired test case and run it in IntelliJ

# Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the (latest) releases.

# User Manual

The [Testhub user manual](https://gematik.github.io/ti2.0-testhub/) is available
online. Make sure to check [the Troubleshooting
section](https://gematik.github.io/ti2.0-testhub/#_troubleshooting) if you run
into any issues.

# Getting Help

When requesting help or reporting issues please include the following information:

- What operating system are you running? Which CPU architecture? How many CPUs
  are available? How much memory does your machine have?
- Which commands are you running so that we can reproduce the issue. What is the
  outcome?
- Supply any applicable logs. You can use [docker
  logs](https://docs.docker.com/reference/cli/docker/container/logs/).

You can request help or report issuse through [gematik
Anfrageportal](https://service.gematik.de/servicedesk/customer/portal/37).

# Contributing

If you want to contribute, please check our [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

Copyright 2025 gematik GmbH

Apache License, Version 2.0

See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under the License.

## Additional Notes and Disclaimer from gematik GmbH

1. Copyright notice: Each published work result is accompanied by an explicit statement of the license conditions for
   use. These are regularly typical conditions in connection with open source or free software. Programs
   described/provided/linked here are free software, unless otherwise stated.
2. Permission notice: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
   associated documentation files (the "Software"), to deal in the Software without restriction, including without
   limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
   Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
    1. The copyright notice (Item 1) and the permission notice (Item 2) shall be included in all copies or substantial
       portions of the Software.
    2. The software is provided "as is" without warranty of any kind, either express or implied, including, but not
       limited to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The
       authors or copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising
       from, out of or in connection with the software or the use or other dealings with the software, whether in an
       action of contract, tort, or otherwise.
    3. The software is the result of research and development activities, therefore not necessarily quality assured and
       without the character of a liable product. For this reason, gematik does not provide any support or other user
       assistance (unless otherwise stated in individual cases and without justification of a legal obligation).
       Furthermore, there is no claim to further development and adaptation of the results to a more current state of
       the art.
3. Gematik may remove published results temporarily or permanently from the place of publication at any time without
   prior notice or justification.
4. Please note: Parts of this code may have been generated using AI-supported technology. Please take this into account,
   especially when troubleshooting, for security analyses and possible adjustments.
