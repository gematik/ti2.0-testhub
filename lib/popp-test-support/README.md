# PoPP Test Support

Shared RBEL-based PoPP token assertions and JSON blueprints for Testhub testsuites.
This module is a regular JAR; reusable code and blueprints are packaged from
`src/main/java` and `src/main/resources`. It does not contain Cucumber glue,
suite-specific card models, robot dependencies or environment credentials.

## Usage

Add the dependency to a testsuite; its version is managed by the Testhub parent:

```xml
<dependency>
    <groupId>de.gematik.ti20.popp</groupId>
    <artifactId>popp-test-support</artifactId>
    <scope>test</scope>
</dependency>
```

Use `de.gematik.ti20.popp.testsupport.PoppTokenValidator` after Tiger has recorded
the PoPP request/response traffic:

```java
PoppTokenValidator.validateClaimsInPoppToken();
PoppTokenValidator.assertPatientData(expectedPatientId, expectedInsurerId);
PoppTokenValidator.assertPractitionerData(expectedActorId, expectedProfessionOid);
PoppTokenValidator.proofMethodInTokenMatchesCommTypeOrThrow("kontaktbehaftet");
```

The validator selects the first request matching `.*/token`, preserving the
existing suites' selection behaviour. It checks the response, header and body
against the packaged blueprints and requires `iat` and `patientProofTime` to be
earlier than the current time plus 30 seconds. These are claim/response assertions,
not cryptographic signature verification.

The default body blueprint ignores the issuer and permits profession OIDs
`1.2.276.0.76.4.50` through `.54` and `.245`, as previously used by the PoPP suite.
For different expectations, use
`validateClaimsInPoppToken(expectedIssuer, actorProfessionOidPattern)`.
Both arguments are expected values in Tiger's JSON comparison, with the second
being a regular expression. The VSDM suite retains its configured issuer and
narrower profession-OID pattern through this overload.

Blueprints are loaded from a namespaced classpath location, so no paths into
another suite's source tree are needed. Tiger placeholders, including
`${popp.server.kidTokenKey}` and an explicitly supplied `${popp.server.url}`, are
resolved at validation time using the consuming suite's Tiger configuration.

Assertions for error status and error message are also available.
Mapping card models to expected values and storing a token in the test context
remain responsibilities of the consuming suite.

## Local build

From the repository root:

```shell
mvn -pl lib/popp-test-support -am test
```
