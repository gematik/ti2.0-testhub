<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# API Specification for Scenarios

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a></li>
    <li><a href="#release-notes">Release Notes</a></li>
    <li><a href="#changelog">Changelog</a></li>
    <li><a href="#folder-structure">Folder Structure</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

## About the Project

This project provides interface specifications for PoPP-Service.
For a specification of the PoPP-Service, visit [gemSpec_Pages][].

## Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the
(latest) releases.

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for information about changes.

## Folder Structure

This project has the following folders:

| Folder | Subfolder | Content                                                |
|:-------|-----------|--------------------------------------------------------|
| images |           | static image material for rendering Markdown documents |
| src    | openapi   | API as [YAML][]-files with [OpenAPI][] content         | 

## Contributing

If you want to contribute, please check our [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

Copyright 2025 gematik GmbH

Apache License, Version 2.0

See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under the License

## How to run

To start the server, run mvn spring-boot:run in the project directory.
Configuration parameters such as port, keystore paths and passwords can be set in
src/main/resources/application.properties.
Once the server is running, you can access the Swagger UI at http://localhost:9210/swagger-ui/index.html to explore and
test the API endpoints.

## Examples

Beside Swagger, you can use tools like curl or Postman to send requests to the server. Here are some example curl
commands:

To create tokens with the specified parameters using the configured keystore to sign the tokens:

```
curl -X 'POST' \
  'http://localhost:9210/popp/test/api/v1/token-generator' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"tokenParamsList":
 [
   {
      "proofMethod":"ehc-practitioner-user-x509",
      "patientProofTime":"1753877030",
      "iat":"1753877040",
      "patientId":"X110639491",
      "insurerId":"109500969",
      "actorId":"883110000168650",
      "actorProfessionOid":"1.2.276.0.76.4.32"
   },
   {
      "proofMethod":"ehc-practitioner-user-x509",
      "patientProofTime":"1753877050",
      "iat":"1753877060",
      "patientId":"F110639491",
      "insurerId":"209500969",
      "actorId":"883110000168650",
      "actorProfessionOid":"1.2.276.0.76.4.32"
   }
]
}'
```

To create tokens with the specified parameters using a provided private key to sign the tokens:

```
curl -X 'POST' \
  'http://localhost:9210/popp/test/api/v1/token-generator' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"tokenParamsList":
     [
       {
          "proofMethod":"ehc-practitioner-user-x509",
          "patientProofTime":"1753877030",
          "iat":"1753877040",
          "patientId":"X110639491",
          "insurerId":"109500969",
          "actorId":"883110000168650",
          "actorProfessionOid":"1.2.276.0.76.4.32"
       }
    ],
    "securityParams": {
        "storeContent":"MIIGAQIBAzC... (base64 endoded PKCS#12 content) ...",
        "storePass": "00",
        "keyAlias": "alias",
        "keyPass": "00"
    }
}'
```

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

[OpenAPI]:https://www.openapis.org/

[YAML]:https://yaml.org/

[gemSpec_Pages]:https://gemspec.gematik.de/docs/gemSpec/
