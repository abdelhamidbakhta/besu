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
package org.hyperledger.besu.ethereum.core.fees;

import static org.hyperledger.besu.ethereum.core.fees.EIP1559Config.BASEFEE_MAX_CHANGE_DENOMINATOR;
import static org.hyperledger.besu.ethereum.core.fees.EIP1559Config.EIP1559_DECAY_RANGE;
import static org.hyperledger.besu.ethereum.core.fees.EIP1559Config.EIP1559_GAS_INCREMENT_AMOUNT;
import static org.hyperledger.besu.ethereum.core.fees.EIP1559Config.MAX_GAS_EIP1559;
import static org.hyperledger.besu.ethereum.core.fees.EIP1559Config.TARGET_GAS_USED;

import org.hyperledger.besu.config.GenesisConfigOptions;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EIP1559Manager {
  private static final Logger LOG = LogManager.getLogger();

  private final long initialForkBlknum;
  private final long finalForkBlknum;

  public EIP1559Manager(final GenesisConfigOptions genesisConfigOptions) {
    // TODO EIP-1559 remove this fake assignment, this is for test only
    initialForkBlknum = Long.MAX_VALUE - EIP1559_DECAY_RANGE - 2;
    LOG.debug(
        "Ignoring genesis config options for EIP-1559 fork block: {}",
        genesisConfigOptions.getEIP1559BlockNumber());
    // initialForkBlknum = genesisConfigOptions.getEIP1559BlockNumber().orElse(0);
    finalForkBlknum = initialForkBlknum + EIP1559_DECAY_RANGE;
    EIP1559Config.INITIAL_FORK_BLKNUM = Optional.of(() -> initialForkBlknum);
  }

  public long computeBaseFee(final long parentBaseFee, final long parentBlockGasUsed) {
    long delta = parentBlockGasUsed - TARGET_GAS_USED;
    if (delta < 0) {
      delta = -delta;
    }
    return parentBaseFee
        + ((parentBaseFee * delta) / TARGET_GAS_USED / BASEFEE_MAX_CHANGE_DENOMINATOR);
  }

  public boolean isValidBaseFee(final long parentBaseFee, final long baseFee) {
    return Math.abs(baseFee - parentBaseFee)
        <= Math.max(1, parentBaseFee / BASEFEE_MAX_CHANGE_DENOMINATOR);
  }

  public long eip1559GasPool(final long blockNumber) {
    if (blockNumber >= finalForkBlknum) {
      return MAX_GAS_EIP1559;
    }
    return (MAX_GAS_EIP1559 / 2)
        + ((blockNumber - initialForkBlknum) * EIP1559_GAS_INCREMENT_AMOUNT);
  }

  public long legacyGasPool(final long blockNumber) {
    return MAX_GAS_EIP1559 - eip1559GasPool(blockNumber);
  }
}
