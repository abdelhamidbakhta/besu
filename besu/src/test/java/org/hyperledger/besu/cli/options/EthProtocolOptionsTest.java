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
package org.hyperledger.besu.cli.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import org.hyperledger.besu.ethereum.eth.EthProtocolConfiguration;

import org.junit.Test;

public class EthProtocolOptionsTest
    extends AbstractCLIOptionsTest<EthProtocolConfiguration, EthProtocolOptions> {

  @Test
  public void parsesValidEwpMaxGetHeadersOptions() {

    final TestBesuCommand cmd = parseCommand("--Xewp-max-get-headers", "13");

    final EthProtocolOptions options = getOptionsFromBesuCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxGetBlockHeaders().intValue()).isEqualTo(13);
    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void parsesInvalidEwpMaxGetHeadersOptionsShouldFail() {
    parseCommand("--Xewp-max-get-headers", "-13");
    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString())
        .contains(
            "Invalid value for option '--Xewp-max-get-headers': cannot convert '-13' to PositiveNumber");
  }

  @Test
  public void parsesValidEwpMaxGetBodiesOptions() {
    final TestBesuCommand cmd = parseCommand("--Xewp-max-get-bodies", "14");

    final EthProtocolOptions options = getOptionsFromBesuCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxGetBlockBodies().intValue()).isEqualTo(14);

    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void parsesInvalidEwpMaxGetBodiesOptionsShouldFail() {
    parseCommand("--Xewp-max-get-bodies", "-14");
    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString())
        .contains(
            "Invalid value for option '--Xewp-max-get-bodies': cannot convert '-14' to PositiveNumber");
  }

  @Test
  public void parsesValidEwpMaxGetReceiptsOptions() {
    final TestBesuCommand cmd = parseCommand("--Xewp-max-get-receipts", "15");

    final EthProtocolOptions options = getOptionsFromBesuCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxGetReceipts().intValue()).isEqualTo(15);

    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void parsesInvalidEwpMaxGetReceiptsOptionsShouldFail() {
    parseCommand("--Xewp-max-get-receipts", "-15");

    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString())
        .contains(
            "Invalid value for option '--Xewp-max-get-receipts': cannot convert '-15' to PositiveNumber");
  }

  @Test
  public void parsesValidEwpMaxGetNodeDataOptions() {
    final TestBesuCommand cmd = parseCommand("--Xewp-max-get-node-data", "16");

    final EthProtocolOptions options = getOptionsFromBesuCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxGetNodeData().intValue()).isEqualTo(16);

    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void parsesInvalidEwpMaxGetNodeDataOptionsShouldFail() {
    parseCommand("--Xewp-max-get-node-data", "-16");
    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString())
        .contains(
            "Invalid value for option '--Xewp-max-get-node-data': cannot convert '-16' to PositiveNumber");
  }

  @Override
  EthProtocolConfiguration createDefaultDomainObject() {
    return EthProtocolConfiguration.builder().build();
  }

  @Override
  EthProtocolConfiguration createCustomizedDomainObject() {
    final EthProtocolConfiguration defaultConfig = EthProtocolConfiguration.builder().build();
    return EthProtocolConfiguration.builder()
        .maxGetBlockHeaders(defaultConfig.getMaxGetBlockHeaders() + 2)
        .maxGetBlockBodies(defaultConfig.getMaxGetBlockBodies() + 2)
        .maxGetReceipts(defaultConfig.getMaxGetReceipts() + 2)
        .maxGetNodeData(defaultConfig.getMaxGetNodeData() + 2)
        .maxGetPooledTransactions(defaultConfig.getMaxGetPooledTransactions() + 2)
        .eth65Enabled(!defaultConfig.isEth65Enabled())
        .build();
  }

  @Override
  EthProtocolOptions optionsFromDomainObject(final EthProtocolConfiguration domainObject) {
    return EthProtocolOptions.fromConfig(domainObject);
  }

  @Override
  EthProtocolOptions getOptionsFromBesuCommand(final TestBesuCommand besuCommand) {
    return besuCommand.getEthProtocolOptions();
  }
}
