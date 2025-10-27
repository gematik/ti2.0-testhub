/*
 *
 * Copyright 2025 gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */
package net.thucydides.model.steps;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import net.thucydides.model.domain.DataTable;
import net.thucydides.model.domain.Story;
import net.thucydides.model.domain.TestOutcome;
import net.thucydides.model.domain.TestResult;
import net.thucydides.model.screenshots.ScreenshotAndHtmlSource;

public class StepListenerAdapter implements StepListener {

  @Override
  public void testSuiteStarted(Class<?> aClass) {}

  @Override
  public void testSuiteStarted(Story story) {}

  @Override
  public void testSuiteFinished() {}

  @Override
  public void testStarted(String s) {}

  @Override
  public void testStarted(String s, String s1) {}

  @Override
  public void testStarted(String s, String s1, ZonedDateTime zonedDateTime) {}

  @Override
  public void testFinished(TestOutcome testOutcome) {}

  @Override
  public void testFinished(TestOutcome testOutcome, boolean b, ZonedDateTime zonedDateTime) {}

  @Override
  public void testRetried() {}

  @Override
  public void stepStarted(ExecutedStepDescription executedStepDescription) {}

  @Override
  public void skippedStepStarted(ExecutedStepDescription executedStepDescription) {}

  @Override
  public void stepFailed(StepFailure stepFailure) {}

  @Override
  public void stepFailed(StepFailure stepFailure, List<ScreenshotAndHtmlSource> list, boolean b) {}

  @Override
  public void stepFailed(
      StepFailure stepFailure,
      List<ScreenshotAndHtmlSource> list,
      boolean b,
      ZonedDateTime zonedDateTime) {}

  @Override
  public void lastStepFailed(StepFailure stepFailure) {}

  @Override
  public void stepIgnored() {}

  @Override
  public void stepPending() {}

  @Override
  public void stepPending(String s) {}

  @Override
  public void stepFinished() {}

  @Override
  public void stepFinished(List<ScreenshotAndHtmlSource> list, ZonedDateTime zonedDateTime) {}

  @Override
  public void testFailed(TestOutcome testOutcome, Throwable throwable) {}

  @Override
  public void testIgnored() {}

  @Override
  public void testSkipped() {}

  @Override
  public void testPending() {}

  @Override
  public void testIsManual() {}

  @Override
  public void notifyScreenChange() {}

  @Override
  public void useExamplesFrom(DataTable dataTable) {}

  @Override
  public void addNewExamplesFrom(DataTable dataTable) {}

  @Override
  public void exampleStarted(Map<String, String> map) {}

  @Override
  public void exampleFinished() {}

  @Override
  public void assumptionViolated(String s) {}

  @Override
  public void testRunFinished() {}

  @Override
  public void takeScreenshots(List<ScreenshotAndHtmlSource> list) {}

  @Override
  public void takeScreenshots(TestResult testResult, List<ScreenshotAndHtmlSource> list) {}

  @Override
  public void recordScreenshot(String s, byte[] bytes) {}
}
