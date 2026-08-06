# E-Rezept PoPP Integration Test Suite

## Prerequisites

### Build Docker images (optional)

```bash
cd $(git rev-parse --show-toplevel)
./mvnw clean install -Pdocker -DskipTests
```

## Local Setup

### Start the TestHub with a Konnektor from KSP

Set the Konnektor credentials and context information:

Create a local shared volume for the Konnektor and smartcards. This step is only required once, as the volume will persist until you remove it.

```bash
cd $(git rev-parse --show-toplevel)
PRJ=testhub-local
VOLUME_NAME="${PRJ}_test_data"

echo "Creating local volume ${VOLUME_NAME} for Konnektor and smartcards"
docker volume create "${VOLUME_NAME}"
cid=$(docker run -d -v "testhub-local_test_data:/app" alpine:3 sleep infinity)

docker exec $cid mkdir -p /app/konnektor
docker exec $cid mkdir -p /app/cards
docker cp ./no-publish/test-data/erp/konnektor/. $cid:/app/konnektor
docker cp ./no-publish/test-data/erp/smartcards/smcb/80276883110000163973-C_SMCB_HCI_AUT_E256.p12 $cid:/app/smcb_private.p12
docker rm -f "$cid"

# now create the local volume for cards used by VSDM testsuite
VOLUME_NAME="${PRJ}_cards"

echo "Creating local volume ${VOLUME_NAME} for cards"
docker volume create "${VOLUME_NAME}"
cid=$(docker run -d -v "testhub-local_cards:/app" alpine:3 sleep infinity)
docker cp ./test/vsdm-testsuite/src/test/resources/data/cards/. $cid:/app/
docker rm -f "$cid"
```

Start the TestHub with a Konnektor from KSP:

> ⚠️ **Attention**: This is intended for documentation purposes and internal testing only. Required certificates are **not** provided with this repository.

```bash
#export POPP_SERVER_URL=wss://popp.dev.poppservice.de:443/popp/practitioner/api/v1/token-generation-ehc
export POPP_SERVER_URL=wss://popp.test.poppservice.de/popp/practitioner/api/v1/token-generation-ehc
#export POPP_SERVER_URL=wss://popp.ref.poppservice.de/popp/practitioner/api/v1/token-generation-ehc

export CONNECTOR_END_POINT_URL=https://kon9.ksp.ltuzd.telematik-test
export CONNECTOR_SECURE_KEYSTORE=/mnt/konnektor/kon9_CS1.p12
export CONNECTOR_SECURE_KEYSTORE_PASSWORD=123456
export CONNECTOR_SECURE_TRUSTSTORE=/mnt/erpe2e_truststore.p12
export CONNECTOR_SECURE_TRUSTSTORE_PASSWORD=123456

export CARD_TERMINAL_ID=e526d874-e1fb-4b88-b55f-930a5ca7baab
export CONTEXT_CLIENT_SYSTEM_ID=CS1
export CONTEXT_WORKPLACE_ID=AP1
export CONTEXT_MANDANT_ID=Mandant1

cd $(git rev-parse --show-toplevel)
docker compose -f ./doc/docker/compose-local.yaml --profile full up -d --remove-orphans
```

## Running Tests

Execute the integration test suite with proxy configuration:

> ⚠️ **Attention**: This is intended for documentation purposes and internal testing only. Without SZZP you won't be able to reach the E-Rezept Fachdienst.

```bash
cd test/erp-testsuite

mvn clean verify \
  -Dspring.profiles.active=tu \
  -Dhttps.proxyHost=127.0.0.1 \
  -Dhttps.proxyPort=6350 \
  -Dskip.unittests=true \
  -Dskip.inttests=false \
  -Dtiger.lib.activateWorkflowUi=false \
  -Djunit.tags=positive
```

## Debugging

To fetch a PoPP Token via curl, you can use the following command:

```bash
curl -X 'POST' 'http://popp-client-erp/token' \
  --proxy 'http://127.0.0.1:6350' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"communicationType": "contact-connector"}'
```

## Cleanup

```bash
cd $(git rev-parse --show-toplevel)
docker compose -f ./doc/docker/compose-local.yaml --profile full down -v --remove-orphans
```
