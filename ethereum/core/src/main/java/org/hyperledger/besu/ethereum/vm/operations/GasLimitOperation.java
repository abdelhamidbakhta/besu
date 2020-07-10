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
package org.hyperledger.besu.ethereum.vm.operations;

import org.hyperledger.besu.ethereum.core.Gas;
import org.hyperledger.besu.ethereum.vm.EVM;
import org.hyperledger.besu.ethereum.vm.GasCalculator;
import org.hyperledger.besu.ethereum.vm.MessageFrame;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class GasLimitOperation extends AbstractFixedCostOperation {

  public GasLimitOperation(final GasCalculator gasCalculator) {
    super(0x45, "GASLIMIT", 0, 1, false, 1, gasCalculator, gasCalculator.getBaseTierGasCost());
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    if (frame.getRemainingGas().compareTo(gasCost) < 0) {
      return outOfGasResponse;
    }

    final Gas gasLimit = Gas.of(frame.getBlockHeader().getGasLimit());
    final Bytes32 value = Bytes32.leftPad(Bytes.of(gasLimit.getBytes()));
    frame.pushStackItem(value);

    return successResponse;
  }
}
