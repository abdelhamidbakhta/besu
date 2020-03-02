/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.besu.tests.acceptance.database;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.tests.acceptance.dsl.AcceptanceTestBase;
import org.hyperledger.besu.tests.acceptance.dsl.account.Account;
import org.hyperledger.besu.tests.acceptance.dsl.blockchain.Amount;
import org.hyperledger.besu.tests.acceptance.dsl.node.BesuNode;
import org.hyperledger.besu.tests.acceptance.dsl.node.configuration.BesuNodeConfigurationBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DatabaseMigrationAcceptanceTest extends AcceptanceTestBase {
  private final String testName;
  private final String dataPath;
  private final long expectedChainHeight;
  private Path hostDataPath;
  private BesuNode node;
  private final List<AccountData> testAccounts;

  public DatabaseMigrationAcceptanceTest(
      final String testName,
      final String dataPath,
      final long expectedChainHeight,
      final Map<Address, Wei> expectedBalances) {
    this.testName = testName;
    this.dataPath = dataPath;
    this.expectedChainHeight = expectedChainHeight;
    testAccounts = new ArrayList<>(expectedBalances.size());
    expectedBalances.forEach(
        (address, wei) -> testAccounts.add(new AccountData(accounts.createAccount(address), wei)));
  }

  @Parameters(name = "{0}")
  public static Object[][] getParameters() {
    return new Object[][] {
      // First 10 blocks of ropsten
      new Object[] {
        "Before versioning was enabled",
        "version0",
        0xA,
        ImmutableMap.of(
            Address.fromHexString("0xd1aeb42885a43b72b518182ef893125814811048"),
            Wei.fromHexString("0x2B5E3AF16B1880000"))
      },
      new Object[] {
        "After versioning was enabled ",
        "version1",
        0xA,
        ImmutableMap.of(
            Address.fromHexString("0xd1aeb42885a43b72b518182ef893125814811048"),
            Wei.fromHexString("0x2B5E3AF16B1880000"))
      }
    };
  }

  @Before
  public void setUp() throws Exception {
    final URL rootURL = DatabaseMigrationAcceptanceTest.class.getResource(dataPath);
    hostDataPath = copyDataDir(rootURL);
    final Path databaseArchive =
        Paths.get(
            DatabaseMigrationAcceptanceTest.class
                .getResource(String.format("%s/besu-db-archive.tar.gz", dataPath))
                .toURI());
    extract(databaseArchive, hostDataPath.toAbsolutePath().toString());
    node = besu.createNode(testName, this::configureNode);
    cluster.start(node);
  }

  private BesuNodeConfigurationBuilder configureNode(
      final BesuNodeConfigurationBuilder nodeBuilder) {
    final String genesisData = getGenesisConfiguration();
    return nodeBuilder
        .devMode(false)
        .dataPath(hostDataPath)
        .genesisConfigProvider((nodes) -> Optional.of(genesisData))
        .jsonRpcEnabled();
  }

  private String getGenesisConfiguration() {
    try {
      return Resources.toString(
          hostDataPath.resolve("genesis.json").toUri().toURL(), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void shouldReturnCorrectBlockHeight() {
    blockchain.currentHeight(expectedChainHeight).verify(node);
  }

  @Test
  public void shouldReturnCorrectAccountBalance() {
    testAccounts.forEach(
        accountData ->
            accountData
                .account
                .balanceEquals(Amount.wei(accountData.expectedBalance.toBigInteger()))
                .verify(node));
  }

  private static void extract(final Path path, final String destDirectory) throws IOException {
    try (TarArchiveInputStream fin =
        new TarArchiveInputStream(
            new GzipCompressorInputStream(new FileInputStream(path.toAbsolutePath().toString())))) {
      TarArchiveEntry entry;
      while ((entry = fin.getNextTarEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }
        final File curfile = new File(destDirectory, entry.getName());
        final File parent = curfile.getParentFile();
        if (!parent.exists()) {
          parent.mkdirs();
        }
        IOUtils.copy(fin, new FileOutputStream(curfile));
      }
    }
  }

  private Path copyDataDir(final URL url) {
    if (url == null) {
      throw new RuntimeException("Unable to locate resource.");
    }

    try {
      final Path tmpDir = Files.createTempDirectory("data");
      Files.delete(tmpDir);
      final Path toCopy = Paths.get(url.toURI());
      try (final Stream<Path> pathStream = Files.walk(toCopy)) {
        pathStream.forEach(source -> copy(source, tmpDir.resolve(toCopy.relativize(source))));
        return tmpDir.toAbsolutePath();
      }
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void copy(final Path source, final Path dest) {
    try {
      Files.copy(source, dest, REPLACE_EXISTING);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private static class AccountData {
    private final Account account;
    private final Wei expectedBalance;

    private AccountData(final Account account, final Wei expectedBalance) {
      this.account = account;
      this.expectedBalance = expectedBalance;
    }
  }
}
