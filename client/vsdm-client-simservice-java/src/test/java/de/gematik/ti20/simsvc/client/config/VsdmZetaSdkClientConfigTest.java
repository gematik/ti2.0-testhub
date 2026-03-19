/*
 *
 * Copyright 2025-2026 gematik GmbH
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
package de.gematik.ti20.simsvc.client.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.zeta.sdk.authentication.SubjectTokenProvider;
import de.gematik.zeta.sdk.authentication.smb.SmbTokenProvider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class VsdmZetaSdkClientConfigTest {

  private SubjectTokenProvider invokeGetTokenProvider(VsdmZetaSdkClientConfig cfg)
      throws Exception {
    Method m = VsdmZetaSdkClientConfig.class.getDeclaredMethod("getTokenProvider");
    m.setAccessible(true);
    try {
      Object res = m.invoke(cfg);
      return (SubjectTokenProvider) res;
    } catch (InvocationTargetException ite) {
      // unwrap the real exception for easier assertions in tests
      throw (Exception) ite.getTargetException();
    }
  }

  @Test
  public void blankPathThrows() {
    VsdmZetaSdkClientConfig cfg = new VsdmZetaSdkClientConfig();
    RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeGetTokenProvider(cfg));
    assertTrue(
        ex.getMessage().contains("SMCB private key path is not configured"),
        "expected message about missing configuration, got: " + ex.getMessage());
  }

  @Test
  public void nonExistingFileThrows(@TempDir Path tmpDir) {
    VsdmZetaSdkClientConfig cfg = new VsdmZetaSdkClientConfig();
    Path p = tmpDir.resolve("does-not-exist.pem");
    cfg.setSmcbPrivateKeyPath(p.toString());

    RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeGetTokenProvider(cfg));
    assertTrue(
        ex.getMessage().contains("does not exist"),
        "expected message about missing file, got: " + ex.getMessage());
  }

  @Test
  public void directoryNotRegularFileThrows(@TempDir Path tmpDir) throws Exception {
    VsdmZetaSdkClientConfig cfg = new VsdmZetaSdkClientConfig();
    Path dir = Files.createDirectory(tmpDir.resolve("aDir"));
    cfg.setSmcbPrivateKeyPath(dir.toString());

    RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeGetTokenProvider(cfg));
    assertTrue(
        ex.getMessage().contains("is not readable"),
        "expected message about not readable, got: " + ex.getMessage());
  }

  @Test
  public void validFileReturnsProvider(@TempDir Path tmpDir) throws Exception {
    VsdmZetaSdkClientConfig cfg = new VsdmZetaSdkClientConfig();
    Path f = Files.createTempFile(tmpDir, "smcb", ".pem");
    // ensure readable
    assertTrue(f.toFile().setReadable(true, false), "temp key file must be readable");
    cfg.setSmcbPrivateKeyPath(f.toString());
    cfg.setSmcbAlias("aliasX");
    cfg.setSmcbPrivateKeyPassword("pw");

    SubjectTokenProvider provider = assertDoesNotThrow(() -> invokeGetTokenProvider(cfg));
    assertNotNull(provider);
    // the implementation should be SmbTokenProvider
    assertInstanceOf(SmbTokenProvider.class, provider);
  }
}
