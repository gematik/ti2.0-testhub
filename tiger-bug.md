# Tiger bug report: `NullPointerException` in `RbelElement.getChildNodesWithKeyStream` under concurrent traffic

**Component:** `tiger-rbel` / `de.gematik.rbellogger.data.RbelElement`
**Affected versions:** 4.4.0 (confirmed), 4.4.1 (method bytecode is identical — see §6)
**Severity:** medium — makes load/parallel-traffic test scenarios non-deterministically fail.
The test logic itself passes; the failure happens in Tiger's reporting path.
**Type:** race condition / thread safety

---

## 1. Summary

When the Tiger proxy is ingesting messages concurrently (many parallel HTTP requests), any
traversal of the RBel element tree from another thread can hit a **null `Map.Entry`** and throw:

```
java.lang.NullPointerException: Cannot invoke "java.util.Map$Entry.getValue()" because "e" is null
	at de.gematik.rbellogger.data.RbelElement.lambda$getChildNodesWithKeyStream$1(RbelElement.java:245)
```

The traversal is triggered by Tiger's own reporting code, not by test code, so a test whose
steps all pass is still reported as an error by failsafe.

## 2. Root cause

Decompiled from `tiger-rbel-4.4.0.jar` (`javap -p -c`), `RbelElement.getChildNodesWithKeyStream()`
is effectively:

```java
public Stream<Map.Entry<String, RbelElement>> getChildNodesWithKeyStream() {
  return facets.stream()                                  // ConcurrentLinkedQueue<RbelFacet>
      .flatMap(facet -> facet.getChildElements().stream()) // RbelMultiMap -> ArrayDeque-backed
      .filter(e -> e.getValue() != null);                  // <-- NPE here: `e` itself is null
}
```

Two problems compound:

1. **The outer container is thread-safe, the inner one is not.** `facets` is a
   `ConcurrentLinkedQueue`, but each facet's `RbelMultiMap` is backed by an `ArrayDeque`
   (visible in the stack trace as `java.util.ArrayDeque$DeqSpliterator.forEachRemaining`).
   `ArrayDeque` is explicitly **not** thread-safe and its spliterator is fail-fast only for
   structural modification it happens to notice — a concurrent append can be observed as a
   torn read that yields a `null` slot.

2. **The filter guards the wrong thing.** `.filter(e -> e.getValue() != null)` defends against
   a null *value* but dereferences `e` without a null check, so the torn read becomes an NPE
   instead of being skipped.

So while proxy parser threads append child elements to a message's facets, a reader thread
streaming the same tree observes a `null` entry and dies.

## 3. Two observed trigger paths

Both reach the same defect; the first is the one that fails the build.

**(a) Cucumber reporting thread — fails the test**

```
SerenityReporterCallbacks.handleTestStepFinished(SerenityReporterCallbacks.java:485)
SerenityReporterCallbacks.updateStepInformation(SerenityReporterCallbacks.java:429)
SerenityReporterCallbacks.informWorkflowUiAboutCurrentStep(SerenityReporterCallbacks.java:547)
SerenityReporterCallbacks.getMessageMetaData(SerenityReporterCallbacks.java:690)
MessageMetaDataDto.createFrom(MessageMetaDataDto.java:82)
RbelElement.lambda$findAllNestedElementsWithFacet$16(RbelElement.java:530)
RbelPathAble.getChildNodes(RbelPathAble.java:44)
RbelElement.lambda$getChildNodesWithKeyStream$1(RbelElement.java:245)   <-- NPE
```

After **every finished step**, Tiger snapshots the message tree to inform the Workflow UI —
while requests from the previous step are still being parsed. Note this runs even with
`-Dtiger.lib.activateWorkflowUi=false`, which is how we hit it (headless run).

**(b) Tiger proxy web API — logged as HTTP 500, does not fail the test**

```
ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet] - Servlet.service() ... threw exception
MetaMessageScrollableDto.createFrom(MetaMessageScrollableDto.java:82)
... same RbelElement frames ...
```

The scrollable message list endpoint traverses the same live tree from a Tomcat worker thread.

## 4. Reproduction

Repo: `gematik/ti2.0-testhub` (VSDM 2.0 testsuite). The two scenarios that reproduce it fire
**100 requests** through the proxy:

- `test/vsdm-testsuite/src/test/resources/features/perf/UC_VSDM2_RVSD_PERF_MULTI_WITH_UPDATE.feature`
- `test/vsdm-testsuite/src/test/resources/features/perf/UC_VSDM2_RVSD_PERF_MULTI_WITHOUT_UPDATE.feature`

```bash
docker compose -f ./doc/docker/compose-local.yaml --profile full up -d --remove-orphans

VSDM_LOAD_TESTING_ACTIVE=false ./mvnw -pl test/vsdm-testsuite/ verify \
  -Dcucumber.filter.tags="@TYPE:PERF" \
  -Dskip.inttests=false \
  -Dtiger.lib.activateWorkflowUi=false
```

**Observed frequency:** 3 consecutive runs on the same machine —

| Run | Outcome | NPE occurrences |
|-----|---------|-----------------|
| 1 | 14/15 (failed on an unrelated response-time assertion, 1136 ms > 1000 ms) | 0 |
| 2 | all 15 scenarios passed, **build FAILED** on 2 driver errors | 8 |
| 3 | all 15 scenarios passed, **build FAILED** on 2 driver errors | 8 |

Reported as:

```
[ERROR] Tests run: 16, Failures: 0, Errors: 2, Skipped: 12
[ERROR]   Driver012IT.Mehrfache Abfrage der VSD mit eGK und ohne VSD Update unter Last » NullPointer
[ERROR]   Driver013IT.Mehrfache Abfrage der VSD mit eGK und mit VSD Update unter Last » NullPointer
[INFO]   - Results: 15 tests | 15 passed | 0 failed | 0 errors
```

Note the contradiction in that output: Serenity reports **15/15 passed** because every Gherkin
step succeeded, while failsafe reports **2 errors** because the exception is thrown from the
reporting callback after the step finished. That mismatch is a good fingerprint for this bug.

## 5. Environment

| | |
|---|---|
| Tiger | 4.4.0 (`version.tiger` in root `pom.xml`) |
| JDK | Amazon Corretto 22.0.2 (arm64) |
| OS | macOS, arm64 (Darwin 25.6.0) |
| Concurrency | 100 requests per scenario through the local Tiger proxy |
| Workflow UI | disabled (`-Dtiger.lib.activateWorkflowUi=false`) — not required to reproduce |

## 6. Does 4.4.1 fix it?

**No.** Comparing `javap -p -c` output for `getChildNodesWithKeyStream` and its lambdas between
`tiger-rbel-4.4.0.jar` and `tiger-rbel-4.4.1.jar`, the instruction sequences are identical — the
only differences are constant-pool indices. The unguarded `e.getValue()` is still present.

## 7. Suggested fixes

**Minimal, hides the symptom:**

```java
.filter(e -> e != null && e.getValue() != null)
```

This stops the crash but still yields a torn, possibly incomplete view of the tree — acceptable
for best-effort reporting DTOs, not for assertions.

**Proper fix — make the traversal safe.** Options, roughly in order of preference:

1. Back `RbelMultiMap` with a concurrent structure (e.g. `ConcurrentLinkedQueue`/
   `CopyOnWriteArrayList`) so iteration is weakly consistent instead of undefined, matching
   what `facets` already does.
2. Snapshot under a lock: have `getChildElements()` return an immutable copy while holding the
   same monitor that mutation uses.
3. Give reporting an explicitly immutable view — freeze a message's element tree once parsing
   for that message completes, and have `MessageMetaDataDto` / `MetaMessageScrollableDto`
   traverse only frozen messages.

Option 1 or 3 also fixes trigger path (b), which currently returns HTTP 500 from the proxy's
own web API under load.

## 8. Workaround for consumers

None that is both reliable and useful. Disabling the Workflow UI does **not** help, because
`informWorkflowUiAboutCurrentStep` runs regardless. Reducing parallelism in the scenario
lowers the hit rate but defeats the purpose of a load test. Until this is fixed, load
scenarios that route through the Tiger proxy have to be treated as flaky.
