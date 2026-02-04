<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# TI 2.0 Testhub

**NOTE:** This project is not meant to be run in production and we strongly
advise against!

the **ti 2.0 testhub** provides a comprehensive test environment for the
modernized german telematics infrastructure ( ti) version 2.0. as the healthcare
telematics infrastructure undergoes significant architectural improvements, a
core aspect is the implementation of **zero trust architecture (zeta)** security
concepts.

this project enables developers and testers to:

- **simulate and validate** zero trust architecture (zeta) components
- **test popp** (proof of possession) workflows for secure authentication
- **validate vsdm2** (versichertenstammdatenmanagement 2.0) functionality
- **develop and test** client applications against mock backend services
- **understand integration patterns** for ti 2.0 ecosystem

# Getting started

Install required software:

- Java 21
- Docker
- Docker Compose

Then run in a shell from project root:

``` bash
./doc/bin/test-with-compose-local-rebuild.sh
```

This will:

1. compile the sources
2. build the required Docker Images
3. run `docker compose`
4. execute available test suites against the local Testhub instance

# Usage

> [!IMPORTANT]  
> Running Testhub testsuits requires a valid SMC-B certificate and key pair. Please request a test SMC-B at [gematik Anfrageportal](https://service.gematik.de/servicedesk/customer/portal/37)
> Please place them in the `doc/docker/backend/zeta/smcb-private` folder as follows:  `smcb_private.p12` (p12 file with AUT_E256_X509 certificate), `smcb_private.alias.txt` (alias of the smcb certificate in the p12 file, default is `alias`) and `smcb_private.pw.txt` (password of the p12 file).

After making changes to the code you can either run

``` bash
./doc/bin/docker-compose-local-rebuild.sh
```

or for more fine-grained control use

``` bash
./mvnw install -Pdocker
```

To stop all services:

```bash
./doc/bin/docker-compose-down.sh
```

If you don't want to start the PS-Simulation (VSDM Client and Card Terminal Client) you can simply set the environment variable `PSSIM_ACTIVE=true`. This will skip starting those two services through the Tiger TestEnvironment Manager.

```bash
set PSSIM_ACTIVE=false
```

## Running Tests

The Testhub includes a test suite for validating VSDM2 workflows:

```bash
./mvnw verify -Dskip.inttests=false -pl test/vsdm-testsuite
```

## Running Tests manually

Tests are written using Cucumber and Gherkin. The files are located in `test/`.

To execute tests manually using IntelliJ:

1. Install the plugins Gherkin and Cucumber for Java.
2. [Configure IntelliJ using the Tiger manual](https://gematik.github.io/app-Tiger/Tiger-User-Manual.html#intellij)
3. Locate your desired test case and run it in IntelliJ

# Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the (latest) releases.

# Changelog

See [CHANGELOG.md](./CHANGELOG.md) for information about changes.

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
